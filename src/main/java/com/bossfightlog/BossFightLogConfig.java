package com.bossfightlog;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(BossFightLogConfig.GROUP)
public interface BossFightLogConfig extends Config
{
	String GROUP = "boss-fight-log";

	@ConfigItem(
		keyName = "idleTimeoutTicks",
		name = "Idle timeout (ticks)",
		description = "Pause the fight timer after this many ticks without landing a hit, so banking or walking away does not count against your DPS"
	)
	default int idleTimeoutTicks()
	{
		return 10;
	}
}
