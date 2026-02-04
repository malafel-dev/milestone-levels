package com.antimated.leaderboard;

/**
 * Represents a single row from the skill leaderboard tables on the OSRS hiscores website.
 */
public class LeaderboardEntry {
    public LeaderboardEntry(String name, int rank, int level, int xp) {
        this.name = name;
        this.rank = rank;
        this.level = level;
        this.xp = xp;
    }

    public final String name;
    public final int rank;
    public final int level;
    public final int xp;
}
