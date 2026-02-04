package com.antimated.util;

import java.awt.Color;

import com.antimated.MilestoneLevelsConfig;
import com.antimated.leaderboard.LeaderboardEntry;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import net.runelite.client.config.RuneScapeProfileType;
import net.runelite.client.util.QuantityFormatter;
import net.runelite.client.util.Text;

@Slf4j
public class Util
{
	private static final int IN_LMS_VARBIT = 5314;

	/**
	 * Checks if a level is a valid real level (>= 1 and <= 99)
	 *
	 * @param level int
	 * @return boolean
	 */
	public static boolean isValidRealLevel(int level)
	{
		return level >= 1 && level <= Experience.MAX_REAL_LEVEL;
	}


	/**
	 * Checks if a number is a valid XP target (>= 1 and <= 200M)
	 *
	 * @param xp int
	 * @return boolean
	 */
	public static boolean isValidExperience(int xp)
	{
		return xp > 0 && xp <= Experience.MAX_SKILL_XP;
	}


	/**
	 * @param string String
	 * @return boolean
	 */
	public static boolean isInteger(String string)
	{
		try
		{
			Integer.parseInt(string);
			return true;
		}
		catch (NumberFormatException e)
		{
			return false;
		}
	}

	/**
	 * Gets the int value for a color.
	 *
	 * @param color color
	 * @return int
	 */
	public static int getIntValue(Color color)
	{
		int red = color.getRed();
		int green = color.getGreen();
		int blue = color.getBlue();

		// Combine RGB values into a single integer
		return (red << 16) | (green << 8) | blue;
	}


	/**
	 * Replaces the words $skill and $level from the text to the passed skill and level respectively
	 *
	 * @param text  String
	 * @param skill Skill
	 * @param level int
	 * @return String
	 */
	public static String replaceSkillAndLevel(String text, Skill skill, int level)
	{
		return Text.escapeJagex(text
			.replaceAll("\\$skill", skill.getName())
			.replaceAll("\\$level", Integer.toString(level)));
	}

	/**
	 * Replaces the words $skill and $xp from the text to the passed skill and level respectively
	 *
	 * @param text  String
	 * @param skill Skill
	 * @param xp    int
	 * @return String
	 */
	public static String replaceSkillAndExperience(String text, Skill skill, int xp)
	{
		return Text.escapeJagex(text
			.replaceAll("\\$skill", skill.getName())
			.replaceAll("\\$xp", QuantityFormatter.formatNumber(xp)));
	}


	/** Replaces the words $skill, $xp, $rank, and $player from the text to the passed skill and data from
	 * leaderboardEntry
	 *
	 * @param text  String
	 * @param skill Skill
	 * @param leaderboardEntry LeaderboardEntry
	 * @return String
	 */
	public static String replaceLeaderboardValues(String text, Skill skill, LeaderboardEntry leaderboardEntry)
	{
		return Text.escapeJagex(text
				.replaceAll("\\$skill", skill.getName())
				.replaceAll("\\$xp", QuantityFormatter.formatNumber(leaderboardEntry.xp))
				.replaceAll("\\$rank", QuantityFormatter.formatNumber(leaderboardEntry.rank))
				.replaceAll("\\$player", leaderboardEntry.name)
				.replaceAll("\\$name", leaderboardEntry.name));
	}

	/**
	 * Check if notification for a skill is enabled in the config.
	 *
	 * @param config MilestoneLevelsConfig
	 * @param skill Skill
	 * @return boolean
	 */
	public static boolean skillEnabledInConfig(MilestoneLevelsConfig config, Skill skill)
	{
		switch (skill)
		{
			case ATTACK:
				return config.showAttackNotifications();
			case DEFENCE:
				return config.showDefenceNotifications();
			case STRENGTH:
				return config.showStrengthNotifications();
			case HITPOINTS:
				return config.showHitpointsNotifications();
			case RANGED:
				return config.showRangedNotifications();
			case PRAYER:
				return config.showPrayerNotifications();
			case MAGIC:
				return config.showMagicNotifications();
			case COOKING:
				return config.showCookingNotifications();
			case WOODCUTTING:
				return config.showWoodcuttingNotifications();
			case FLETCHING:
				return config.showFletchingNotifications();
			case FISHING:
				return config.showFishingNotifications();
			case FIREMAKING:
				return config.showFiremakingNotifications();
			case CRAFTING:
				return config.showCraftingNotifications();
			case SMITHING:
				return config.showSmithingNotifications();
			case MINING:
				return config.showMiningNotifications();
			case HERBLORE:
				return config.showHerbloreNotifications();
			case AGILITY:
				return config.showAgilityNotifications();
			case THIEVING:
				return config.showThievingNotifications();
			case SLAYER:
				return config.showSlayerNotifications();
			case FARMING:
				return config.showFarmingNotifications();
			case RUNECRAFT:
				return config.showRunecraftNotifications();
			case HUNTER:
				return config.showHunterNotifications();
			case CONSTRUCTION:
				return config.showConstructionNotifications();
			case SAILING:
				return config.showSailingNotifications();
		}

		return true;
	}

	public static boolean isStandardWorld(Client client)
	{
		return RuneScapeProfileType.getCurrent(client) == RuneScapeProfileType.STANDARD;
	}

	public static boolean isInLMS(Client client)
	{
		return client.getVarbitValue(IN_LMS_VARBIT) == 1;
	}
}
