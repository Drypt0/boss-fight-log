package com.bossfightlog;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Every recorded kill of one boss, with its aggregates.
 */
class BossGroup
{
	private final String bossName;
	private final int combatLevel;
	private final List<BossFight> fights = new ArrayList<>();

	private int totalTicks;
	private int totalDamage;
	private int totalTaken;
	private int totalHits;
	private int totalAttempts;
	private int bestTicks = Integer.MAX_VALUE;
	private double bestDps;

	private BossGroup(BossFight first)
	{
		this.bossName = first.getBossName();
		this.combatLevel = first.getCombatLevel();
	}

	private void add(BossFight fight)
	{
		fights.add(fight);
		totalTaken += fight.getDamageTaken();
		totalHits += fight.getHits();
		totalAttempts += fight.getHits() + fight.getMisses();
		bestTicks = Math.min(bestTicks, fight.getDurationTicks());

		// A same tick kill has damage but no duration to divide it by. Counting its damage
		// without its time would inflate every rate derived from the pair.
		if (!fight.isDpsMeasurable())
		{
			return;
		}

		totalTicks += fight.getDurationTicks();
		totalDamage += fight.getDamageDealt();
		bestDps = Math.max(bestDps, fight.getDps());
	}

	String getBossName()
	{
		return bossName;
	}

	int getCombatLevel()
	{
		return combatLevel;
	}

	int getKills()
	{
		return fights.size();
	}

	int getDamageTaken()
	{
		return totalTaken;
	}

	double getBestDps()
	{
		return bestDps;
	}

	/**
	 * Kills of this boss, most recent first.
	 */
	List<BossFight> getFightsNewestFirst()
	{
		List<BossFight> reversed = new ArrayList<>(fights);
		java.util.Collections.reverse(reversed);
		return reversed;
	}

	double getAverageSeconds()
	{
		int measured = 0;
		for (BossFight fight : fights)
		{
			if (fight.isDpsMeasurable())
			{
				measured++;
			}
		}
		return measured == 0 ? 0 : totalTicks * BossFight.SECONDS_PER_TICK / measured;
	}

	double getBestSeconds()
	{
		return bestTicks == Integer.MAX_VALUE ? 0 : bestTicks * BossFight.SECONDS_PER_TICK;
	}

	/**
	 * Total damage over total time, so a long kill weighs more than a short one.
	 */
	double getAverageDps()
	{
		double seconds = totalTicks * BossFight.SECONDS_PER_TICK;
		return seconds > 0 ? totalDamage / seconds : 0;
	}

	double getAccuracy()
	{
		return totalAttempts > 0 ? (double) totalHits / totalAttempts : 0;
	}

	/**
	 * Groups kills by boss, ordered by most recently killed first.
	 */
	static List<BossGroup> group(List<BossFight> fights)
	{
		Map<String, BossGroup> byName = new LinkedHashMap<>();
		for (BossFight fight : fights)
		{
			byName.computeIfAbsent(fight.getBossName(), name -> new BossGroup(fight)).add(fight);
		}

		List<BossGroup> groups = new ArrayList<>(byName.values());
		// Insertion order follows first kill; the most recently fought boss is more useful on top.
		groups.sort((a, b) -> Integer.compare(lastIndex(fights, b), lastIndex(fights, a)));
		return groups;
	}

	private static int lastIndex(List<BossFight> fights, BossGroup group)
	{
		for (int i = fights.size() - 1; i >= 0; i--)
		{
			if (fights.get(i).getBossName().equals(group.getBossName()))
			{
				return i;
			}
		}
		return -1;
	}

	/**
	 * How this kill's DPS compares with the player's other recorded kills of the same boss,
	 * as a signed fraction. Returns null when there is nothing to compare against.
	 */
	static Double dpsTrend(BossFight fight, List<BossFight> all)
	{
		double otherDamageSeconds = 0;
		double otherDamage = 0;
		int others = 0;

		if (!fight.isDpsMeasurable())
		{
			return null;
		}

		for (BossFight other : all)
		{
			if (other == fight || !other.getBossName().equals(fight.getBossName()) || !other.isDpsMeasurable())
			{
				continue;
			}
			otherDamage += other.getDamageDealt();
			otherDamageSeconds += other.getDurationSeconds();
			others++;
		}

		if (others == 0 || otherDamageSeconds <= 0)
		{
			return null;
		}

		double baseline = otherDamage / otherDamageSeconds;
		if (baseline <= 0)
		{
			return null;
		}

		return (fight.getDps() - baseline) / baseline;
	}
}
