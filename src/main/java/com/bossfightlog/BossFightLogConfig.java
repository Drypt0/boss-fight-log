package com.bossfightlog;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(BossFightLogConfig.GROUP)
public interface BossFightLogConfig extends Config
{
	String GROUP = "boss-fight-log";
	String CLEAR_HISTORY = "clearHistory";

	@ConfigItem(
		keyName = "idleTimeoutTicks",
		name = "Idle timeout (ticks)",
		description = "Pause the fight timer after this many ticks without landing a hit, so banking or walking away does not count against your DPS",
		position = 1
	)
	default int idleTimeoutTicks()
	{
		return 10;
	}

	/**
	 * A trigger rather than a setting: enabling it wipes the history and it switches itself back
	 * off. RuneLite shows a confirmation prompt for any item carrying a warning, which is what
	 * makes this safe to sit in the settings panel.
	 */
	@ConfigItem(
		keyName = CLEAR_HISTORY,
		name = "Clear recorded history",
		description = "Deletes every kill recorded for the character you are logged in as",
		warning = "This deletes every kill recorded for this character. A backup is kept in the boss-fight-log folder, but the panel will be emptied.",
		position = 2
	)
	default boolean clearHistory()
	{
		return false;
	}
}
