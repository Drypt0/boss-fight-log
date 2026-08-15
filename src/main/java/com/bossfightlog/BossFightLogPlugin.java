package com.bossfightlog;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;
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
	private Client client;

	@Inject
	private BossFightLogConfig config;

	@Override
	protected void startUp() throws Exception
	{
		log.debug("Boss Fight Log started");
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.debug("Boss Fight Log stopped");
	}

	@Provides
	BossFightLogConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BossFightLogConfig.class);
	}
}
