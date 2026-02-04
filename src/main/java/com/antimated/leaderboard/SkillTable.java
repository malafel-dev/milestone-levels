package com.antimated.leaderboard;

/**
 * Maps skills to leaderboard table numbers from the OSRS hiscores website.
 */
public enum SkillTable {
    ATTACK(1),
    DEFENCE(2),
    STRENGTH(3),
    HITPOINTS(4),
    RANGED(5),
    PRAYER(6),
    MAGIC(7),
    COOKING(8),
    WOODCUTTING(9),
    FLETCHING(10),
    FISHING(11),
    FIREMAKING(12),
    CRAFTING(13),
    SMITHING(14),
    MINING(15),
    HERBLORE(16),
    AGILITY(17),
    THIEVING(18),
    SLAYER(19),
    FARMING(20),
    RUNECRAFT(21),
    HUNTER(22),
    CONSTRUCTION(23),
    SAILING(24);

    public final int tableNumber;

    SkillTable(int tableNumber) {
        this.tableNumber = tableNumber;
    }
}
