package com.bossfightlog;

import com.google.inject.Provides;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameState;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.NpcDespawned;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "Boss Fight Log",
	description = "Per-kill boss history with DPS, kill times, damage taken and accuracy",
	tags = {"dps", "boss", "kill", "damage", "combat", "pvm", "log"}
)
public class BossFightLogPlugin extends Plugin
{
	@Inject
	private BossFightLogConfig config;

	@Inject
	private FightTracker fightTracker;

	private final List<BossFight> fights = new ArrayList<>();

	@Override
	protected void startUp() throws Exception
	{
		fights.clear();
		fightTracker.reset();
		fightTracker.setFightListener(this::onFightComplete);
		log.debug("Boss Fight Log started");
	}

	@Override
	protected void shutDown() throws Exception
	{
		fightTracker.setFightListener(fight -> {
		});
		fightTracker.reset();
		fights.clear();
		log.debug("Boss Fight Log stopped");
	}

	/**
	 * Kills recorded this session, most recent last.
	 */
	public List<BossFight> getFights()
	{
		return Collections.unmodifiableList(fights);
	}

	private void onFightComplete(BossFight fight)
	{
		fights.add(fight);
		log.debug("{} killed in {}s: {} dps, {} damage taken, {}/{} accuracy",
			fight.getBossName(),
			String.format("%.1f", fight.getDurationSeconds()),
			String.format("%.2f", fight.getDps()),
			fight.getDamageTaken(),
			fight.getHits(),
			fight.getHits() + fight.getMisses());
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
	public void onGameTick(GameTick event)
	{
		fightTracker.onGameTick();
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
