package com.antimated.leaderboard;

/**
 * Possible states for the LeaderboardManager state machine.
 *
 * See LeaderboardManager for implementation details and notes.
 */
public enum LeaderboardManagerState {
    // The first step is for the LeaderboardManager to get the player's name for leaderboard lookup. This is not
    // available until the player is logged in and gameplay is active.
    AWAITING_PLAYER_NAME,
    // After the player name is available, the LeaderboardManager immediately issues a request to the hiscores API for
    // that player's full hiscore state, which is kept steady until the player logs out or hops.
    AWAITING_PLAYER_HISCORE,
    // If player hiscore data is available, the LeaderboardManager has a starting point for querying leaderboard pages.
    // This means it is in the active state where it is continually trying to upkeep lists of XP milestones for the
    // player to pass.
    ACTIVE,
    // Failsafe to stop all operation of the LeaderboardManager after an error that doesn't have recovery logic
    // implemented. Effectively prefer turning the submodule off over having it run in a weird state, potentially
    // spamming requests to the OSRS hiscores website, slowing the server down, and slowing down the player's client.
    UNRECOVERABLE_ERROR,
}
