package com.antimated.leaderboard;

import lombok.Value;
import net.runelite.api.Skill;

import java.util.Collections;
import java.util.List;

/**
 * Array of LeaderboardEntries that result from requesting and processing a single page from the OSRS hiscores website.
 */
@Value
public class LeaderboardResult {
    private List<LeaderboardEntry> entries;

    public List<LeaderboardEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }
}
