package com.bossfightlog;

import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
	name = "Boss Fight Log",
	description = "Per-kill boss history with DPS, kill times, damage taken and accuracy",
	tags = {"dps", "boss", "kill", "damage", "combat", "pvm", "log"}
)
public class BossFightLogPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private FightTracker fightTracker;

	@Inject
	private FightStore fightStore;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ClientToolbar clientToolbar;

	private final List<BossFight> fights = new ArrayList<>();

	private BossFightLogPanel panel;
	private NavigationButton navButton;

	/**
	 * Character whose history is currently loaded, or null before the first login. History is kept
	 * per character so accounts do not pool their kills.
	 */
	private String loadedCharacter;

	@Override
	protected void startUp() throws Exception
	{
		fights.clear();
		loadedCharacter = null;
		fightTracker.reset();
		fightTracker.setFightListener(this::onFightComplete);

		panel = new BossFightLogPanel();

		BufferedImage icon = ImageUtil.loadImageResource(BossFightLogPlugin.class, "icon.png");
		navButton = NavigationButton.builder()
			.tooltip("Boss Fight Log")
			.icon(icon)
			.priority(5)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);

		log.debug("Boss Fight Log started, panel font: {}", Fonts.describe());
	}

	@Override
	protected void shutDown() throws Exception
	{
		fightTracker.setFightListener(fight -> {
		});
		fightTracker.reset();
		fights.clear();
		loadedCharacter = null;

		clientToolbar.removeNavigation(navButton);
		navButton = null;
		panel = null;

		log.debug("Boss Fight Log stopped");
	}

	/**
	 * Handles the settings toggle that wipes this character's history. The toggle is a trigger
	 * rather than a stored preference, so it is switched straight back off once acted on.
	 */
	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!BossFightLogConfig.GROUP.equals(event.getGroup())
			|| !BossFightLogConfig.CLEAR_HISTORY.equals(event.getKey())
			|| !Boolean.parseBoolean(event.getNewValue()))
		{
			return;
		}

		configManager.setConfiguration(BossFightLogConfig.GROUP, BossFightLogConfig.CLEAR_HISTORY, false);
		clearFights();
	}

	private void clearFights()
	{
		clientThread.invoke(() -> {
			if (loadedCharacter == null)
			{
				log.debug("Nothing cleared: no character loaded");
				return;
			}

			log.debug("Clearing {} recorded kills for {}", fights.size(), loadedCharacter);
			fights.clear();
			// Copied aside rather than simply overwritten. This is the one action that can destroy
			// a history outright, and a confirmation is little comfort once it has been dismissed.
			fightStore.saveWithBackup(loadedCharacter, fights);
			updatePanel();
		});
	}

	private void onFightComplete(BossFight fight)
	{
		fights.add(fight);

		while (fights.size() > FightStore.MAX_FIGHTS)
		{
			fights.remove(0);
		}

		persist();
		updatePanel();

		log.debug("{} killed in {}s: {} dps, {} damage taken, {}/{} accuracy",
			fight.getBossName(),
			String.format("%.1f", fight.getDurationSeconds()),
			String.format("%.2f", fight.getDps()),
			fight.getDamageTaken(),
			fight.getHits(),
			fight.getHits() + fight.getMisses());
	}

	private void persist()
	{
		if (loadedCharacter != null)
		{
			fightStore.save(loadedCharacter, fights);
		}
	}

	private void updatePanel()
	{
		if (panel != null)
		{
			panel.setFights(fights);
		}
	}

	/**
	 * Loads the history for whichever character is logged in, once their name is available.
	 * <p>
	 * The name is not readable at the moment the game state flips to logged in, so this is driven
	 * from the tick instead of from the state change.
	 */
	private void loadHistoryIfNeeded()
	{
		Player local = client.getLocalPlayer();
		if (local == null)
		{
			return;
		}

		String name = local.getName();
		if (name == null || name.equals(loadedCharacter))
		{
			return;
		}

		loadedCharacter = name;
		log.debug("Loading fight history for {}", name);

		fightStore.load(name, loaded -> clientThread.invoke(() -> {
			// Anything recorded while the file was being read is kept, and would otherwise be lost
			// to the load overwriting it.
			List<BossFight> merged = new ArrayList<>(FightStore.capped(loaded));
			merged.addAll(fights);

			fights.clear();
			fights.addAll(FightStore.capped(merged));
			updatePanel();

			log.debug("Loaded {} recorded kills for {}", fights.size(), name);
		}));
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		fightTracker.onHitsplatApplied(event);
	}

	@Subscribe
	public void onActorDeath(ActorDeath event)
	{
		fightTracker.onActorDeath(event);
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		fightTracker.onNpcDespawned(event);
	}

	@Subscribe
	public void onNpcChanged(NpcChanged event)
	{
		fightTracker.onNpcChanged(event);
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		fightTracker.onGameTick();
		loadHistoryIfNeeded();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		GameState state = event.getGameState();
		if (state == GameState.LOGIN_SCREEN || state == GameState.HOPPING)
		{
			fightTracker.reset();
		}
	}

	@Provides
	BossFightLogConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BossFightLogConfig.class);
	}
}
