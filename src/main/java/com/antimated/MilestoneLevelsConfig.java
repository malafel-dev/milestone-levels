package com.antimated;

import java.awt.Color;

import com.antimated.leaderboard.ValidLeaderboard;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.ui.JagexColors;

@ConfigGroup(MilestoneLevelsConfig.CONFIG_GROUP)
public interface MilestoneLevelsConfig extends Config
{
	String CONFIG_GROUP = "milestoneLevels";
	@ConfigSection(
		name = "Levels",
		description = "All level notification settings.",
		position = 100
	)
	String SECTION_LEVELS = "levels";

	@ConfigItem(
		keyName = "notificationLevelColor",
		name = "Color",
		description = "Changes the color of the notification title and text.",
		section = SECTION_LEVELS,
		position = 0
	)
	default Color notificationLevelColor()
	{
		return JagexColors.DARK_ORANGE_INTERFACE_TEXT;
	}

	@ConfigItem(
		keyName = "notificationLevelTitle",
		name = "Title",
		description = "Can include $level and $skill variables.",
		section = SECTION_LEVELS,
		position = 1
	)
	default String notificationLevelTitle()
	{
		return "Level milestone";
	}

	@ConfigItem(
		keyName = "notificationLevelText",
		name = "Text",
		description = "Can include $level and $skill variables.",
		section = SECTION_LEVELS,
		position = 2
	)
	default String notificationLevelText()
	{
		return "Gained level $level in $skill!";
	}

	@ConfigItem(
		keyName = "showOnLevels",
		name = "Levels",
		description = "Configures levels to display notifications on, comma separated.",
		section = SECTION_LEVELS,
		position = 3
	)
	default String showOnLevels()
	{
		return "10, 20, 30, 40, 50, 60, 70, 80, 90, 99";
	}

	@ConfigItem(
		keyName = "showVirtualLevels",
		name = "Notify for virtual levels",
		description = "Notify when leveling a virtual level. Ignores the list of skills.",
		section = SECTION_LEVELS,
		position = 4
	)
	default boolean showVirtualLevels()
	{
		return true;
	}

	@ConfigSection(
		name = "Experience",
		description = "All xp notification settings.",
		position = 200
	)
	String SECTION_EXPERIENCE = "experience";

	@ConfigItem(
		keyName = "notificationExperienceColor",
		name = "Color",
		description = "Changes the color of the notification title and text.",
		section = SECTION_EXPERIENCE,
		position = 0
	)
	default Color notificationExperienceColor()
	{
		return JagexColors.DARK_ORANGE_INTERFACE_TEXT;
	}

	@ConfigItem(
		keyName = "notificationExperienceTitle",
		name = "Title",
		description = "Can include $xp and $skill variables.",
		section = SECTION_EXPERIENCE,
		position = 1
	)
	default String notificationExperienceTitle()
	{
		return "XP milestone";
	}

	@ConfigItem(
		keyName = "notificationExperienceText",
		name = "Text",
		description = "Can include $xp and $skill variables.",
		section = SECTION_EXPERIENCE,
		position = 2
	)
	default String notificationExperienceText()
	{
		return "Achieved $xp \nXP in $skill!";
	}

	@ConfigItem(
		keyName = "showOnExperience",
		name = "Experience",
		description = "Configures xp to display notifications on, comma separated.",
		section = SECTION_EXPERIENCE,
		position = 3
	)
	default String showOnExperience()
	{
		return "1000000, 5000000, 10000000, 15000000, 20000000, 25000000, 30000000, 35000000, 40000000, 45000000, 50000000, 55000000, 60000000, 65000000, 70000000, 75000000, 80000000, 85000000, 90000000, 95000000, 100000000, 125000000, 150000000, 200000000";
	}

	@ConfigSection(
		name = "Hiscore Ranks",
		description = "All hiscore rank notification settings",
		position = 300
	)
	String SECTION_LEADERBOARD = "leaderboardRanks";

	@ConfigItem(
		keyName = "notificationLeaderboardRankColor",
		name = "Color",
		description = "Changes the color of the notification title and text.",
		section = SECTION_LEADERBOARD,
		position = 0
	)
	default Color notificationLeaderboardRankColor()
	{
		return JagexColors.DARK_ORANGE_INTERFACE_TEXT;
	}

	@ConfigItem(
		keyName = "notificationLeaderboardRankTitle",
		name = "Title",
		description = "Can include $rank, $xp, $player, and $skill variables.",
		section = SECTION_LEADERBOARD,
		position = 1
	)
	default String notificationLeaderboardRankTitle()
	{
		return "Hiscore rank milestone";
	}

	@ConfigItem(
		keyName = "notificationLeaderboardRankText",
		name = "Text",
		description = "Can include $rank, $xp, $player, and $skill variables.",
		section = SECTION_LEADERBOARD,
		position = 2
	)
	default String notificationLeaderboardRankText()
	{
		return "Achieved rank $rank in $skill,\nsurpassing $name!";
	}

	@ConfigItem(
		keyName = "chosenLeaderboard",
		name = "Leaderboard",
		description = "Configures which leaderboard to use for rank notifications.",
		section = SECTION_LEADERBOARD,
		position = 3
	)
	default ValidLeaderboard chosenLeaderboard()
	{
		return ValidLeaderboard.NORMAL;
	}

	@ConfigSection(
		name = "Skills",
		description = "Settings for what skills we want to display notifications on",
		position = 400
	)
	String SECTION_SKILLS = "skills";

	@ConfigItem(
		keyName = "showAttackNotifications",
		name = "Attack",
		description = "Should we show Attack notifications?",
		section = SECTION_SKILLS
	)
	default boolean showAttackNotifications()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showDefenceNotifications",
		name = "Defence",
		description = "Should we show Defence notifications?",
		section = SECTION_SKILLS
	)
	default boolean showDefenceNotifications()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showStrengthNotifications",
		name = "Strength",
		description = "Should we show Strength notifications?",
		section = SECTION_SKILLS
	)
	default boolean showStrengthNotifications()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showHitpointsNotifications",
		name = "Hitpoints",
		description = "Should we show Hitpoints notifications?",
		section = SECTION_SKILLS
	)
	default boolean showHitpointsNotifications()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showRangedNotifications",
		name = "Ranged",
		description = "Should we show Ranged notifications?",
		section = SECTION_SKILLS
	)
	default boolean showRangedNotifications()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showPrayerNotifications",
		name = "Prayer",
		description = "Should we show Prayer notifications?",
		section = SECTION_SKILLS
	)
	default boolean showPrayerNotifications()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showMagicNotifications",
		name = "Magic",
		description = "Should we show Magic notifications?",
		section = SECTION_SKILLS
	)
	default boolean showMagicNotifications()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showCookingNotifications",
		name = "Cooking",
		description = "Should we show Cooking notifications?",
		section = SECTION_SKILLS
	)
	default boolean showCookingNotifications()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showWoodcuttingNotifications",
		name = "Woodcutting",
		description = "Should we show Woodcutting notifications?",
		section = SECTION_SKILLS
	)
	default boolean showWoodcuttingNotifications()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showFletchingNotifications",
		name = "Fletching",
		description = "Should we show Fletching notifications?",
		section = SECTION_SKILLS
	)
	default boolean showFletchingNotifications()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showFishingNotifications",
		name = "Fishing",
		description = "Should we show Fishing notifications?",
		section = SECTION_SKILLS
	)
	default boolean showFishingNotifications()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showFiremakingNotifications",
		name = "Firemaking",
		description = "Should we show Firemaking notifications?",
		section = SECTION_SKILLS
	)
	default boolean showFiremakingNotifications()
	{
		return true;
	}


	@ConfigItem(
		keyName = "showCraftingNotifications",
		name = "Crafting",
		description = "Should we show Crafting notifications?",
		section = SECTION_SKILLS
	)
	default boolean showCraftingNotifications()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showSmithingNotifications",
		name = "Smithing",
		description = "Should we show Smithing notifications?",
		section = SECTION_SKILLS
	)
	default boolean showSmithingNotifications()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showMiningNotifications",
		name = "Mining",
		description = "Should we show Mining notifications?",
		section = SECTION_SKILLS
	)
	default boolean showMiningNotifications()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showHerbloreNotifications",
		name = "Herblore",
		description = "Should we show Herblore notifications?",
		section = SECTION_SKILLS
	)
	default boolean showHerbloreNotifications()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showAgilityNotifications",
		name = "Agility",
		description = "Should we show Agility notifications?",
		section = SECTION_SKILLS
	)
	default boolean showAgilityNotifications()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showThievingNotifications",
		name = "Thieving",
		description = "Should we show Thieving notifications?",
		section = SECTION_SKILLS
	)
	default boolean showThievingNotifications()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showSlayerNotifications",
		name = "Slayer",
		description = "Should we show Slayer notifications?",
		section = SECTION_SKILLS
	)
	default boolean showSlayerNotifications()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showFarmingNotifications",
		name = "Farming",
		description = "Should we show Farming notifications?",
		section = SECTION_SKILLS
	)
	default boolean showFarmingNotifications()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showRunecraftNotifications",
		name = "Runecraft",
		description = "Should we show Runecraft notifications?",
		section = SECTION_SKILLS
	)
	default boolean showRunecraftNotifications()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showHunterNotifications",
		name = "Hunter",
		description = "Should we show Hunter notifications?",
		section = SECTION_SKILLS
	)
	default boolean showHunterNotifications()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showConstructionNotifications",
		name = "Construction",
		description = "Should we show Construction notifications?",
		section = SECTION_SKILLS
	)
	default boolean showConstructionNotifications()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showSailingNotifications",
		name = "Sailing",
		description = "Should we show Sailing notifications?",
		section = SECTION_SKILLS
	)
	default boolean showSailingNotifications()
	{
		return true;
	}
}
