package com.bossfightlog;

import java.time.Instant;
import lombok.Value;

/**
 * A completed boss kill. Immutable once the fight has resolved.
 */
@Value
public class BossFight
{
	public static final double SECONDS_PER_TICK = 0.6d;

	String bossName;
	int combatLevel;
	int damageDealt;
	int damageTaken;
	int hits;
	int misses;
	int maxHit;
	/**
	 * Ticks spent actually fighting, with idle gaps already deducted.
	 */
	int durationTicks;
	Instant finishedAt;

	public double getDurationSeconds()
	{
		return durationTicks * SECONDS_PER_TICK;
	}

	/**
	 * Whether this fight lasted long enough for a rate to mean anything. A kill landed inside one
	 * tick has no duration to divide by, so it has damage but no DPS.
	 */
	public boolean isDpsMeasurable()
	{
		return durationTicks > 0;
	}

	public double getDps()
	{
		double seconds = getDurationSeconds();
		return seconds > 0 ? damageDealt / seconds : 0d;
	}

	/**
	 * Share of attempted attacks that landed, in the range 0..1.
	 */
	public double getAccuracy()
	{
		int attempts = hits + misses;
		return attempts > 0 ? (double) hits / attempts : 0d;
	}
}
