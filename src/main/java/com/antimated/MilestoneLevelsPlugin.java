package com.antimated;

import com.antimated.leaderboard.*;
import com.antimated.notifications.NotificationManager;
import com.antimated.util.Util;
import com.antimated.version.VersionManager;
import com.google.common.primitives.Ints;
import com.google.inject.Provides;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.*;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Experience;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.events.CommandExecuted;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.StatChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.hiscore.HiscoreClient;
import net.runelite.client.hiscore.HiscoreEndpoint;
import net.runelite.client.hiscore.HiscoreResult;
import net.runelite.client.hiscore.HiscoreSkill;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.QuantityFormatter;
import net.runelite.client.util.Text;


@Slf4j
@PluginDescriptor(
	name = "Milestone Levels",
	description = "Display milestone levels on a fancy league-like notification",
	tags = {"level", "skill", "xp", "experience", "notification", "notifier", "milestone"}
)
public class MilestoneLevelsPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private MilestoneLevelsConfig config;

	@Inject
	private EventBus eventBus;

	@Inject
	private NotificationManager notifications;

	@Inject
	private VersionManager version;

	@Inject
	private ConfigManager configManager;

	@Inject
	private LeaderboardManager leaderboardManager;

	@Inject
	@Named("developerMode")
	boolean developerMode;

	private final Map<Skill, Integer> previousXpMap = new EnumMap<>(Skill.class);
	private HiscoreEndpoint previousHiscoreEndpoint = HiscoreEndpoint.NORMAL;

	@Provides
	MilestoneLevelsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(MilestoneLevelsConfig.class);
	}

	@Override
	protected void startUp()
	{
		clientThread.invoke(this::initializePreviousXpMap);
		previousHiscoreEndpoint = config.hiscoreEndpoint();
		notifications.startUp();
		version.startUp();
		migrate();
	}

	@Override
	protected void shutDown()
	{
		previousXpMap.clear();
		notifications.shutDown();
		version.shutDown();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		// Clear previous XP when not logged in
		switch (gameStateChanged.getGameState())
		{
			case HOPPING:
			case LOGGING_IN:
			case LOGIN_SCREEN:
			case LOGIN_SCREEN_AUTHENTICATOR:
			case CONNECTION_LOST:
				previousXpMap.clear();
				break;
		}

	}

	@Subscribe
	public void onGameTick(GameTick event) {
		leaderboardManager.process(event);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (previousHiscoreEndpoint != config.hiscoreEndpoint()) {
			leaderboardManager.reset();
			previousHiscoreEndpoint = config.hiscoreEndpoint();
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged statChanged)
	{
		final Skill skill = statChanged.getSkill();

		final int currentXp = statChanged.getXp();
		final int currentLevel = Experience.getLevelForXp(currentXp);

		final int previousXp = previousXpMap.getOrDefault(skill, -1);
		final int previousLevel = previousXp == -1 ? -1 : Experience.getLevelForXp(previousXp);

		previousXpMap.put(skill, currentXp);

		// Previous xp has to be set, and our current xp has to be higher or equal to the previous xp
		if (previousXp == -1 || previousXp >= currentXp)
		{
			return;
		}

		// Only standard worlds are allowed, and if a player is in LMS, we should abort.
		if (!Util.isStandardWorld(client) || Util.isInLMS(client))
		{
			log.debug("Not on a standard world nor in LMS.");
			return;
		}

		// Only notify on regular levels when the skill is enabled
		final List<Integer> milestoneLevels = getMilestoneLevels(previousLevel, currentLevel);

		if (shouldNotifyForSkill(skill) && !milestoneLevels.isEmpty())
		{
			log.debug("Milestone levels to notify for after {} check: {}", skill.getName(), milestoneLevels);

			for (int level : milestoneLevels)
			{
				notifyLevel(skill, level);
			}
		}

		// Always notify for virtual levels
		final List<Integer> milestoneVirtualLevels = getMilestoneVirtualLevels(previousLevel, currentLevel);

		if (shouldNotifyVirtualLevels() && !milestoneVirtualLevels.isEmpty())
		{
			log.debug("Virtual milestone levels to notify for: {}", milestoneVirtualLevels);

			for (int virtualLevel : milestoneVirtualLevels)
			{
				notifyLevel(skill, virtualLevel);
			}
		}

		// Only notify on experience when the skill is enabled
		final List<Integer> milestoneExperience = getMilestoneExperience(previousXp, currentXp);

		if (shouldNotifyForSkill(skill) && !milestoneExperience.isEmpty())
		{
			log.debug("Milestone experience to notify for after {} check: {}", skill.getName(), milestoneExperience);

			for (int xp : milestoneExperience)
			{
				notifyExperience(skill, xp);
			}
		}

		final List<LeaderboardEntry> milestoneLeaderboardEntries = getMilestoneLeaderboardEntries(skill, previousXp, currentXp);
		if (shouldNotifyForSkill(skill) && !milestoneLeaderboardEntries.isEmpty())
		{
			log.debug("Milestone leaderboard rank to notify for {}", skill.getName());

			for (LeaderboardEntry entry: milestoneLeaderboardEntries) {
				notifyLeaderboard(skill, entry);
			}
		}
	}

	/**
	 * Gets the list of milestone xp values between two numbers from values specified in the milestone experience config
	 *
	 * @return List<Integer>
	 */
	private List<Integer> getMilestoneExperience(int previousXp, int currentXp)
	{
		return Text.fromCSV(config.showOnExperience()).stream()
			.distinct()
			.filter(Util::isInteger)
			.map(Integer::parseInt)
			.filter(Util::isValidExperience)
			.filter(n -> n > previousXp && n <= currentXp)
			.sorted()
			.collect(Collectors.toList());
	}

	/**
	 * Gets list of valid real levels from config
	 *
	 * @param previousLevel int
	 * @param currentLevel  int
	 * @return List<Integer>
	 */
	private List<Integer> getMilestoneLevels(int previousLevel, int currentLevel)
	{
		return Text.fromCSV(config.showOnLevels()).stream()
			.distinct()
			.filter(Util::isInteger)
			.map(Integer::parseInt)
			.filter(Util::isValidRealLevel)
			.filter(n -> n > previousLevel && n <= currentLevel)
			.sorted()
			.collect(Collectors.toList());
	}

	/**
	 * Gets the list of milestone xp values between two numbers from values that were fetched from the OSRS hiscores
	 *
	 * @param skill Skill
	 * @param previousXp int
	 * @param currentXp int
	 * @return List<LeaderboardEntry>
	 */
	private List<LeaderboardEntry> getMilestoneLeaderboardEntries(Skill skill, int previousXp, int currentXp) {
		return leaderboardManager.getMilestoneLeaderboardEntries(skill, previousXp, currentXp);
	}

	/**
	 * Gets list of valid virtual levels
	 *
	 * @param previousLevel int
	 * @param currentLevel  int
	 * @return List<Integer>
	 */
	private List<Integer> getMilestoneVirtualLevels(int previousLevel, int currentLevel)
	{
		return IntStream
			.rangeClosed(Experience.MAX_REAL_LEVEL + 1, Experience.MAX_VIRT_LEVEL)
			.boxed()
			.filter(n -> n > previousLevel && n <= currentLevel)
			.sorted()
			.collect(Collectors.toList());
	}

	/**
	 * Populate initial xp per skill.
	 */
	private void initializePreviousXpMap()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			previousXpMap.clear();
		}
		else
		{
			for (final Skill skill : Skill.values())
			{
				previousXpMap.put(skill, client.getSkillExperience(skill));
			}
		}
	}

	/**
	 * Adds a level-up notification to the queue if certain requirements are met.
	 *
	 * @param skill Skill
	 * @param level int
	 */
	private void notifyLevel(Skill skill, int level)
	{
		String title = Util.replaceSkillAndLevel(config.notificationLevelTitle(), skill, level);
		String text = Util.replaceSkillAndLevel(config.notificationLevelText(), skill, level);
		int color = Util.getIntValue(config.notificationLevelColor());

		log.debug("Notify {}up milestone reached for {} to level {}", level > Experience.MAX_REAL_LEVEL ? "virtual level-" : "level-", skill.getName(), level);
		notifications.addNotification(title, text, color);
	}

	/**
	 * Adds an xp notification to the queue if certain requirements are met.
	 *
	 * @param skill Skill
	 * @param xp    int
	 */
	private void notifyExperience(Skill skill, int xp)
	{
		String title = Util.replaceSkillAndExperience(config.notificationExperienceTitle(), skill, xp);
		String text = Util.replaceSkillAndExperience(config.notificationExperienceText(), skill, xp);
		int color = Util.getIntValue(config.notificationExperienceColor());

		log.debug("Notify xp milestone reached for {} to xp {}", skill.getName(), QuantityFormatter.formatNumber(xp));
		notifications.addNotification(title, text, color);
	}

	/**
	 * Adds a leaderboard rank notification to the queue if certain requirements are met.
	 *
	 * @param skill Skill
	 * @param leaderboardEntry LeaderboardEntry
	 */
	private void notifyLeaderboard(Skill skill, LeaderboardEntry leaderboardEntry)
	{
		String title = Util.replaceLeaderboardValues(config.notificationLeaderboardRankTitle(), skill, leaderboardEntry);
		String text = Util.replaceLeaderboardValues(config.notificationLeaderboardRankText(), skill, leaderboardEntry);
		int color = Util.getIntValue(config.notificationLeaderboardRankColor());

		log.debug("Notify leaderboard milestone reached for {} to rank {} (xp {})",
				skill.getName(),
				QuantityFormatter.formatNumber(leaderboardEntry.rank),
				QuantityFormatter.formatNumber(leaderboardEntry.xp));
		notifications.addNotification(title, text, color);
	}

	/**
	 * Check if we should notify for virtual levels
	 *
	 * @return boolean
	 */
	private boolean shouldNotifyVirtualLevels()
	{
		return config.showVirtualLevels();
	}

	/**
	 * Check if we should notify for the given skill based off of our config settings.
	 *
	 * @param skill Skill
	 * @return boolean
	 */
	private boolean shouldNotifyForSkill(Skill skill)
	{
		return Util.skillEnabledInConfig(config, skill);
	}

	public void migrate()
	{
		String migrated = configManager.getConfiguration(MilestoneLevelsConfig.CONFIG_GROUP, "migrated");

		if (migrated != null)
		{
			return;
		}

		log.debug("Start config key migration...");

		Map<String, String> configMapping = Map.of(
			"notificationColor", "notificationLevelColor",
			"notificationText", "notificationLevelText",
			"notificationTitle", "notificationLevelTitle"
		);

		for (Map.Entry<String, String> entry : configMapping.entrySet())
		{
			String oldKey = entry.getKey();
			String newKey = entry.getValue();
			String oldValue = configManager.getConfiguration(MilestoneLevelsConfig.CONFIG_GROUP, oldKey);

			log.debug("Old key {} with value {}", oldKey, oldValue);

			if (oldValue != null)
			{
				configManager.setConfiguration(MilestoneLevelsConfig.CONFIG_GROUP, newKey, oldValue);
				configManager.unsetConfiguration(MilestoneLevelsConfig.CONFIG_GROUP, oldKey);
			}
		}

		log.debug("End migration of notification keys to notification level keys");
		configManager.setConfiguration(MilestoneLevelsConfig.CONFIG_GROUP, "migrated", "1");
	}

	@Subscribe
	public void onCommandExecuted(CommandExecuted commandExecuted)
	{
		if (developerMode)
		{
			String[] args = commandExecuted.getArguments();
			switch (commandExecuted.getCommand())
			{
				case "clear":
					notifications.clearNotifications();
					break;

				case "clearversion":
					version.clearLastUpdateMessage();
					break;

				case "setstats":
					for (Skill skill : Skill.values())
					{
						int level = Integer.parseInt(args[0]);

						if (skill == Skill.HITPOINTS && level < 10)
						{
							level = 10;
						}


						level = Ints.constrainToRange(level, 1, Experience.MAX_REAL_LEVEL);
						int xp = Experience.getXpForLevel(level);

						client.getBoostedSkillLevels()[skill.ordinal()] = level;
						client.getRealSkillLevels()[skill.ordinal()] = level;
						client.getSkillExperiences()[skill.ordinal()] = xp;

						client.queueChangedSkill(skill);

						StatChanged statChanged = new StatChanged(
							skill,
							xp,
							level,
							level
						);
						eventBus.post(statChanged);
					}
					break;
			}
		}
	}
}
