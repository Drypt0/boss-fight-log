package com.bossfightlog;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Singleton;
import net.runelite.api.NPC;

/**
 * Maps NPCs onto the encounter they belong to.
 * <p>
 * Most bosses are an encounter of one. A few are fought as a pair and are recorded as a single
 * kill, because two rows reading half a fight each is not what happened.
 * <p>
 * Wave minigames are represented by their final boss alone, so the fight begins on the first hit
 * against Jad, Zuk or Sol Heredit rather than covering the waves before them.
 */
@Singleton
public class BossRegistry
{
	/**
	 * One tracked fight.
	 */
	static final class Encounter
	{
		private final String name;
		private final Set<String> members;
		private final Set<String> adds;

		private Encounter(String name, Set<String> members, Set<String> adds)
		{
			this.name = name;
			this.members = members;
			this.adds = adds;
		}

		String getName()
		{
			return name;
		}

		/**
		 * Members are the boss itself: the encounter ends once every engaged member has died.
		 */
		boolean isMember(String npcName)
		{
			return npcName != null && members.contains(npcName);
		}

		/**
		 * Adds contribute their damage to the fight but their deaths do not end it.
		 */
		boolean isAdd(String npcName)
		{
			return npcName != null && adds.contains(npcName);
		}
	}

	/**
	 * Bosses fought alone, where the encounter is just the one NPC.
	 */
	private static final String[] SOLO = {
		// God Wars Dungeon
		"General Graardor",
		"K'ril Tsutsaroth",
		"Kree'arra",
		"Commander Zilyana",
		// Slayer
		"Cerberus",
		"Abyssal Sire",
		"Kraken",
		"Thermonuclear smoke devil",
		"Alchemical Hydra",
		"Sarachnis",
		"Araxxor",
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
		// Dagannoth Kings, killed and tracked separately
		"Dagannoth Rex",
		"Dagannoth Prime",
		"Dagannoth Supreme",
		// Desert Treasure II
		"Duke Sucellus",
		"The Leviathan",
		"The Whisperer",
		"Vardorvis",
		// Barrows
		"Ahrim the Blighted",
		"Dharok the Wretched",
		"Guthan the Infested",
		"Karil the Tainted",
		"Torag the Corrupted",
		"Verac the Defiled",
		// Perilous Moons
		"Blood Moon",
		"Blue Moon",
		"Eclipse Moon",
		// Wave minigames, final boss only
		"TzTok-Jad",
		"TzKal-Zuk",
		"Sol Heredit",
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
		"Amoxliatl",
		"The Mimic",
		"Shellbane gryphon",
		"Brutus",
		"Demonic Brutus",
		"Mad Angel",
		"Maggot King",
	};

	private final Map<String, Encounter> byNpcName = new HashMap<>();

	public BossRegistry()
	{
		for (String boss : SOLO)
		{
			register(new Encounter(boss, singleton(boss), Collections.emptySet()));
		}

		// Fought as a pair and recorded as one kill.
		register(new Encounter("Grotesque Guardians",
			names("Dusk", "Dawn"), Collections.emptySet()));
		register(new Encounter("Royal Titans",
			names("Branda the Fire Queen", "Eldric the Ice King"), Collections.emptySet()));
	}

	private void register(Encounter encounter)
	{
		for (String member : encounter.members)
		{
			byNpcName.put(member, encounter);
		}
		for (String add : encounter.adds)
		{
			byNpcName.put(add, encounter);
		}
	}

	/**
	 * @return the encounter this NPC takes part in, or null if it is not tracked
	 */
	Encounter encounterFor(NPC npc)
	{
		String name = npc.getName();
		return name == null ? null : byNpcName.get(name);
	}

	private static Set<String> singleton(String name)
	{
		return Collections.singleton(name);
	}

	private static Set<String> names(String... values)
	{
		return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(values)));
	}
}
