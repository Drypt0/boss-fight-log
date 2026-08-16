package com.bossfightlog;

import net.runelite.api.HitsplatID;

/**
 * Classifies raw hitsplat type ids.
 * <p>
 * {@link net.runelite.api.Hitsplat#isMine()} is deliberately not used here. It reports true for
 * BLOCK_ME and for the POISE types as well as for real damage, and this plugin needs those three
 * cases kept apart: a block is a miss for accuracy purposes, and poise is a separate stagger meter
 * rather than hitpoint damage, so folding it into damage dealt would inflate DPS.
 */
final class Hitsplats
{
	private Hitsplats()
	{
	}

	/**
	 * Hitpoint damage dealt by the local player. Excludes poise, which is not hitpoint damage.
	 */
	static boolean isDamageDealtByPlayer(int type)
	{
		switch (type)
		{
			case HitsplatID.DAMAGE_ME:
			case HitsplatID.DAMAGE_ME_CYAN:
			case HitsplatID.DAMAGE_ME_ORANGE:
			case HitsplatID.DAMAGE_ME_YELLOW:
			case HitsplatID.DAMAGE_ME_WHITE:
			case HitsplatID.DAMAGE_MAX_ME:
			case HitsplatID.DAMAGE_MAX_ME_CYAN:
			case HitsplatID.DAMAGE_MAX_ME_ORANGE:
			case HitsplatID.DAMAGE_MAX_ME_YELLOW:
			case HitsplatID.DAMAGE_MAX_ME_WHITE:
				return true;
			default:
				return false;
		}
	}

	/**
	 * A splash or block from the local player, which counts against accuracy.
	 */
	static boolean isMissByPlayer(int type)
	{
		return type == HitsplatID.BLOCK_ME;
	}

	/**
	 * Damage landing on the local player, from any source.
	 * <p>
	 * Callers must already have established that the hitsplat is on the local player, which is why
	 * both the ME and OTHER damage families are accepted. Heals, prayer and sanity drain, poise and
	 * blocks are all excluded.
	 */
	static boolean isDamageTaken(int type)
	{
		switch (type)
		{
			case HitsplatID.DAMAGE_ME:
			case HitsplatID.DAMAGE_ME_CYAN:
			case HitsplatID.DAMAGE_ME_ORANGE:
			case HitsplatID.DAMAGE_ME_YELLOW:
			case HitsplatID.DAMAGE_ME_WHITE:
			case HitsplatID.DAMAGE_MAX_ME:
			case HitsplatID.DAMAGE_MAX_ME_CYAN:
			case HitsplatID.DAMAGE_MAX_ME_ORANGE:
			case HitsplatID.DAMAGE_MAX_ME_YELLOW:
			case HitsplatID.DAMAGE_MAX_ME_WHITE:
			case HitsplatID.DAMAGE_OTHER:
			case HitsplatID.DAMAGE_OTHER_CYAN:
			case HitsplatID.DAMAGE_OTHER_ORANGE:
			case HitsplatID.DAMAGE_OTHER_YELLOW:
			case HitsplatID.DAMAGE_OTHER_WHITE:
			case HitsplatID.POISON:
			case HitsplatID.VENOM:
			case HitsplatID.BURN:
			case HitsplatID.BLEED:
				return true;
			default:
				return false;
		}
	}
}
