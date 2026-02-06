package com.antimated.leaderboard;

import com.antimated.MilestoneLevelsConfig;
import com.antimated.util.Util;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.events.GameTick;
import net.runelite.client.hiscore.HiscoreClient;
import net.runelite.client.hiscore.HiscoreEndpoint;
import net.runelite.client.hiscore.HiscoreResult;
import net.runelite.client.hiscore.HiscoreSkill;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * Issues requests and processes results from the OSRS hiscores website for each tracked skill to maintain lists of XP
 * milestones. Also controls rate limiting and retries, to limit strain on the hiscores page.
 * <p>
 * The objective is to ensure that these lists keep up with the player gaining XP. To accomplish this, an arbitrary
 * minimum list length is chosen `MINIMUM_LEADERBOARD_LIST_LENGTH`. If there are fewer milestones than the minimum list
 * size for a given skill, the LeaderboardManager will issue a request for more leaderboard data. At most one active
 * request may exist at a time for each skill.
 * <p>
 * All logic is performed on the main game thread as part of `onGameTick`, while requests are performed asynchronously.
 * Because of the asynchronous nature, LeaderboardManager operates on a state machine to ensure correct sequencing of
 * operations.
 * <p>
 * NOTE: the term `hiscore` already exists in RuneLite to indicate a player's specific hiscore page results. This class,
 * and related classes continue to use the term `hiscore` in that way, though references may be made to the
 * `hiscore servers`, which vaguely mean the black box of Jagex servers that provide all similar data. This project uses
 * the term `leaderboard` to refer to the array of entries containing [ranks, player names, levels, xp values],
 * ordered by rank for a single skill.
 */
@Slf4j
@Singleton
public class LeaderboardManager {
    @Inject
    private Client client;

    @Inject
    private HiscoreClient hiscoreClient;

    @Inject
    private LeaderboardClient leaderboardClient;

    @Inject
    private MilestoneLevelsConfig config;

    private static final int MIN_LEADERBOARD_SIZE = 100;
    private static final int MAX_REQUEST_RETRIES = 3;
    // The minimum level required in a skill before leaderboard tracking begins. The lower the player's level, the more
    // densely packed the leaderboards should be. A densely packed leaderboard defeats the purpose of these sorts of
    // milestones, and results in lots of outgoing requests to the hiscore servers. This acts as a first line of defense
    // for rate limiting, out of respect to Jagex.
    private static final int MIN_REQUIRED_LEVEL_FOR_TRACKING = 60;

    // State machine indicating the 3 main stages of operation, and an error state that ceases operation.
    private LeaderboardManagerState state = LeaderboardManagerState.AWAITING_PLAYER_NAME;

    // Future that is completed after the player's hiscore data is fetched from the hiscore server.
    private Future<HiscoreResult> hiscoreFuture = null;

    // Holds a complete, valid version of the player's hiscore data after it has been fetched from the hiscore server.
    private HiscoreResult playerHiscore = null;

    // Tracks retries for fetching player hiscore data.
    private int hiscoreRetryCount = 0;

    private final Map<Skill, LeaderboardSkillState> skillStates = new EnumMap<>(Skill.class);
    private boolean wasEnabled = false;

    LeaderboardManager() {
        reset();
    }

    public void process(GameTick event) {
        if (!config.enableLeaderboard()) {
            wasEnabled = false;
            return;
        } else {
            if (!wasEnabled) {
                reset();
            }
            wasEnabled = true;
        }

        switch (state) {
            case AWAITING_PLAYER_NAME:
                processAwaitingPlayerName();
                break;
            case AWAITING_PLAYER_HISCORE:
                processAwaitingPlayerHiscore();
                break;
            case ACTIVE:
                processActive();
                break;
            case UNRECOVERABLE_ERROR:
                // cease operation.
                break;
        }
    }

    /**
     * Returns all `LeaderboardEntry` values for a skill whose xp value lies between previousXP and currentXP exclusive.
     *
     * @param skill Skill
     * @param previousXp int
     * @param currentXp int
     * @return List<LeaderboardEntry>
     */
    public List<LeaderboardEntry> getMilestoneLeaderboardEntries(Skill skill, int previousXp, int currentXp) {
        if (!config.enableLeaderboard()) {
            return new ArrayList<LeaderboardEntry>();
        }

        LeaderboardSkillState skillState = skillStates.get(skill);
        return skillState.validLeaderboardEntries.stream()
                                                 .filter(entry -> entry.xp > previousXp && entry.xp < currentXp)
                                                 .distinct()
                                                 .collect(Collectors.toList());
    }

    /**
     * Set LeaderboardManager to the state it should be in on initialization.
     */
    public void reset() {
        state = LeaderboardManagerState.AWAITING_PLAYER_NAME;
        hiscoreFuture = null;
        playerHiscore = null;
        hiscoreRetryCount = 0;
        for (Skill s: Skill.values()) {
            skillStates.put(s, new LeaderboardSkillState());
        }
    }

    private void processAwaitingPlayerName() {
        if (client.getGameState() == GameState.LOGGED_IN && client.getLocalPlayer() != null && client.getLocalPlayer().getName() != null) {
            if (hiscoreFuture != null) {
                log.error("Already had a future for player hiscore data when entering the AWAITING_PLAYER_HISCORE state.");
                state = LeaderboardManagerState.UNRECOVERABLE_ERROR;
                return;
            }
            hiscoreFuture = hiscoreClient.lookupAsync(client.getLocalPlayer().getName(), HiscoreEndpoint.valueOf(config.chosenLeaderboard().name()));
            state = LeaderboardManagerState.AWAITING_PLAYER_HISCORE;
        }
    }

    private void processAwaitingPlayerHiscore() {
        if (hiscoreFuture == null) {
            // This is effectively an assert. Reaching this point indicates a programming error.
            log.error("Missing future when waiting for player hiscore data.");
            state = LeaderboardManagerState.UNRECOVERABLE_ERROR;
            return;
        }

        if (hiscoreFuture.isDone()) {
            try {
                playerHiscore = hiscoreFuture.get();
                ingestPlayerHiscoreData();
                state = LeaderboardManagerState.ACTIVE;
                hiscoreFuture = null;
            } catch (ExecutionException e) {
                log.warn("Encountered an exception when trying to fetch player specific hiscore data.", e);
                if (hiscoreRetryCount < MAX_REQUEST_RETRIES) {
                    hiscoreFuture = hiscoreClient.lookupAsync(client.getLocalPlayer().getName(), HiscoreEndpoint.valueOf(config.chosenLeaderboard().name()));
                    hiscoreRetryCount++;
                } else {
                    log.warn("Reached max retries when fetching player specific hiscore data. Stopping.");
                    state = LeaderboardManagerState.UNRECOVERABLE_ERROR;
                }
            } catch (InterruptedException e) {
                log.warn("Attempt to fetch player specific hiscore data was interrupted. Stopping.", e);
                state = LeaderboardManagerState.UNRECOVERABLE_ERROR;
            }
        }
    }

    /**
     * Updates all LeaderboardSkillStates based on information contained within the player's hiscore data. This must be
     * called after that data becomes available and before active processing begins.
     */
    private void ingestPlayerHiscoreData() {
        for (Skill s: Skill.values()) {
            skillStates.get(s).nextRankToMeasure = playerHiscore.getSkill(HiscoreSkill.valueOf(s.name())).getRank() - 1;
        }
    }

    /**
     * Steady state processing that happens every game tick. Loop over all skills, and ensure that for all the tracked
     * skills, the list of XP milestones is growing until it reaches the adequate length.
     */
    private void processActive() {
        for (Skill s: Skill.values()) {
            if (Util.skillEnabledInConfig(config, s)) {
                processSkill(s);
            }
        }
    }

    private void processSkill(Skill skill) {
        LeaderboardSkillState skillState = skillStates.get(skill);
        if (skillState.isDisabledFromError ||
            skillState.nextRankToMeasure < 1 ||
            playerHiscore.getSkill(HiscoreSkill.valueOf(skill.name())).getLevel() < MIN_REQUIRED_LEVEL_FOR_TRACKING) {
            return;
        }

        // There might already be an outgoing request for more leaderboard data. Nothing can be done until this is
        // future is completed.
        if (skillState.leaderboardFuture != null) {
            if (!skillState.leaderboardFuture.isDone()) {
                return;
            }

            try {
                // Success case: we just got a new leaderboard page. Now add that data to `skillState`.
                LeaderboardResult leaderboardResult = skillState.leaderboardFuture.get();
                skillState.leaderboardFuture = null;
                skillState.currentPageRetryCount = 0;

                // The results are ordered high XP to low XP since reading begins at the top of the page. Even though
                // the hiscores change over time, all results from this page will be a greater than or equal to the
                // highest XP value from the previous page.
                //
                // It's possible that several people have the same XP for the current skill. This is especially probable
                // at low
                //
                // There might be some duplicate names on this list that increased their rank since last time we
                // checked. It's hard to say how this ought to be handled. The leaderboard is constantly changing and
                // this plugin is using an approximation of the current leaderboar state. For now, duplicates will
                // be allowed.
                ArrayList<LeaderboardEntry> resultEntries = new ArrayList<>(leaderboardResult.getEntries());
                Collections.reverse(resultEntries);

                skillState.validLeaderboardEntries.addAll(resultEntries);

                // De-dupe XP values, only keeping the best (lowest numerical) rank.
                // TODO: Maybe revisit. This is O(n^2) and it doesn't need to be. Shouldn't matter for small lists.
                for (int i = 0; i < skillState.validLeaderboardEntries.size()-1; i++) {
                    if (skillState.validLeaderboardEntries.get(i).xp == skillState.validLeaderboardEntries.get(i+1).xp) {
                        skillState.validLeaderboardEntries.remove(i);
                        i--;
                    }
                }

                // Rank is in decreasing order, meaning the final element is the lowest rank numerically.
                skillState.nextRankToMeasure =
                        skillState.validLeaderboardEntries.get(skillState.validLeaderboardEntries.size()-1).rank - 1;

                // There can be lots of people with 200m experience. Short circuit this and skip straight to rank 1 to
                // prevent tons of pointless queries.
                if (skillState.nextRankToMeasure > 1 &&
                    skillState.validLeaderboardEntries.get(skillState.validLeaderboardEntries.size()-1).xp == 200_000_000) {
                    skillState.nextRankToMeasure = 1;
                }
            } catch (ExecutionException e) {
                // Error handling has lots of failure cases. We only want to retry if there was some sort of network
                // issue, which would manifest as an IOException wrapped with an ExecutionException from the future.
                Throwable cause = e.getCause();
                if (cause instanceof ParseException) {
                    log.warn("Failed to parse fetched hiscore data for skill: {}. Disabling future lookups for that skill.", skill, cause);
                    skillState.isDisabledFromError = true;
                } else if (cause instanceof IOException && skillState.currentPageRetryCount < MAX_REQUEST_RETRIES) {
                    log.warn("Failed to fetch hiscore data for skill: {} due to possible network issue. Retrying.", skill, cause);
                        skillState.currentPageRetryCount++;
                        skillState.leaderboardFuture = null;
                        requestMoreLeaderboardDataForSkill(skill);
                } else if (cause instanceof IOException) {
                    log.warn("Failed to fetch hiscore data for skill: {} due to possible network issue. Reached max retries.", skill, cause);
                        skillState.isDisabledFromError = true;
                } else {
                    log.warn("Failed to fetch hiscore data for skill: {}. Cause was unexpected. Disabling future lookups for that skill.", skill, cause);
                    skillState.isDisabledFromError = true;
                }

            } catch (InterruptedException e) {
                log.warn("Attempt to fetch data for skill: {} was interrupted. Disabling future lookups for that skill.", skill, e);
                skillState.isDisabledFromError = true;
            }

        }
        if (skillState.leaderboardFuture != null) {
            return;
        }
        // This point should now only be reached if there is no leaderboardFuture (it's possible that there was one when
        // this function was initially called).

        // Trim the list of leaderboard entries to remove all XP milestones lower than the player's current XP value for
        // this skill.
        skillState.validLeaderboardEntries =
                skillState.validLeaderboardEntries.stream()
                                                  .filter(entry -> entry.xp > client.getSkillExperience(skill))
                                                  .distinct()
                                                  .collect(Collectors.toList());

        if (skillState.validLeaderboardEntries.size() < MIN_LEADERBOARD_SIZE) {
            requestMoreLeaderboardDataForSkill(skill);
        }
    }

    /**
     * Helper function that initiates a request for the next leaderboard page for a skill.
     */
    private void requestMoreLeaderboardDataForSkill(Skill skill) {
        LeaderboardSkillState skillState = skillStates.get(skill);
        if (skillState.leaderboardFuture != null) {
            log.warn("Attempted to fetch more leaderboard data for skill: {} while a request was already pending. Disabling future lookups.", skill);
            skillState.isDisabledFromError = true;
            return;
        }

        int nextRankToMeasure = skillState.nextRankToMeasure - 1;
        if (nextRankToMeasure <= 0) {
            return;
        }

        // For example: page 2 contains ranks 26 to 50 inclusive.
        //   ((25-1) / 25) + 1 == 1
        //   ((26-1) / 25) + 1 == 2
        //   ((50-1) / 25) + 1 == 2
        //   ((51-1) / 25) + 1 == 3
        int pageToRequest = ((nextRankToMeasure - 1) / 25) + 1;

        skillState.leaderboardFuture = leaderboardClient.lookupAsync(
                skill, pageToRequest, LeaderboardEndpoint.valueOf(config.chosenLeaderboard().name()));
    }

}
