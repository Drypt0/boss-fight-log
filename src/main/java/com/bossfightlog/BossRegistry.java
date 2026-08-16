package com.bossfightlog;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Singleton;
import net.runelite.api.NPC;

/**
 * Decides which NPCs are worth logging a fight for.
 * <p>
 * This is a seed list of unambiguous single-target bosses, matched on display name. It is
 * deliberately conservative: transforming bosses whose death does not fire cleanly, and multi-NPC
 * encounters, still need dedicated handling before they can be listed here.
 */
@Singleton
public class BossRegistry
{
	private static final Set<String> BOSS_NAMES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
		// God Wars Dungeon
		"General Graardor",
		"K'ril Tsutsaroth",
		"Kree'arra",
		"Commander Zilyana",
		// Slayer bosses
		"Cerberus",
		"Abyssal Sire",
		"Kraken",
		"Thermonuclear smoke devil",
		"Alchemical Hydra",
		"Sarachnis",
		// Wilderness
		"Callisto",
		"Venenatis",
		"Vet'ion",
		"Artio",
		"Spindel",
		"Calvar'ion",
		"Chaos Elemental",
		"Chaos Fanatic",
		"Crazy archaeologist",
		"Deranged archaeologist",
		"Scorpia",
		// Dagannoth Kings
		"Dagannoth Rex",
		"Dagannoth Prime",
		"Dagannoth Supreme",
		// Desert Treasure II
		"Duke Sucellus",
		"The Leviathan",
		"The Whisperer",
		"Vardorvis",
		// Assorted
		"King Black Dragon",
		"Kalphite Queen",
		"Corporeal Beast",
		"Giant Mole",
		"Obor",
		"Bryophyta",
		"Skotizo",
		"Hespori",
		"Scurrius",
		"The Hueycoatl",
		"Amoxliatl"
	)));

	public boolean isBoss(NPC npc)
	{
		String name = npc.getName();
		return name != null && BOSS_NAMES.contains(name);
	}
}
