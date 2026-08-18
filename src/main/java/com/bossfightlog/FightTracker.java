package com.bossfightlog;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Hitsplat;
import net.runelite.api.NPC;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcDespawned;

/**
 * Accumulates combat statistics for the boss currently being fought, and emits a {@link BossFight}
 * once the boss dies.
 * <p>
 * Only one fight is tracked at a time. Damage taken cannot be attributed to a particular attacker,
 * so a single active fight is the only way to assign it unambiguously; bosses are close enough to
 * single-target that this holds in practice.
 * <p>
 * Timing is measured in game ticks rather than wall clock, so it does not drift with frame rate or
 * latency. Gaps longer than the configured idle timeout are deducted entirely, which keeps banking
 * and travel out of the DPS figure. The timeout must stay above normal attack speed, or ordinary
 * gaps between attacks would be mistaken for idling.
 */
@Slf4j
@Singleton
public class FightTracker
{
	/**
	 * Drop a fight that has seen no activity for this long. Prevents a boss that was walked away
	 * from, or that despawned unseen, from being credited to the next kill.
	 */
	private static final int ABANDON_AFTER_TICKS = 200;

	/**
	 * How long a despawned boss has to reappear before its fight is written off. Long enough to
	 * cover a phase transition, short enough that a boss which genuinely reset is not credited to
	 * the next kill.
	 */
	private static final int RESUME_GRACE_TICKS = 20;

	private final Client client;
	private final BossFightLogConfig config;
	private final BossRegistry bossRegistry;

	private FightState active;

	/**
	 * A fight whose boss has despawned, held in case the same boss returns.
	 */
	private FightState parked;
	private int parkedAtTick;
	private Consumer<BossFight> fightListener = fight -> {
	};

	@Inject
	private FightTracker(Client client, BossFightLogConfig config, BossRegistry bossRegistry)
	{
		this.client = client;
		this.config = config;
		this.bossRegistry = bossRegistry;
	}

	public void setFightListener(Consumer<BossFight> fightListener)
	{
		this.fightListener = fightListener;
	}

	public void reset()
	{
		active = null;
		parked = null;
	}

	public void onHitsplatApplied(HitsplatApplied event)
	{
		Actor actor = event.getActor();
		Hitsplat hitsplat = event.getHitsplat();
		int type = hitsplat.getHitsplatType();
		int amount = hitsplat.getAmount();
		int tick = client.getTickCount();

		if (actor instanceof NPC)
		{
			boolean dealt = Hitsplats.isDamageDealtByPlayer(type);
			if (!dealt && !Hitsplats.isMissByPlayer(type))
			{
				return;
			}

			FightState fight = fightFor((NPC) actor, tick);
			if (fight == null)
			{
				return;
			}

			registerActivity(fight, tick);

			if (dealt && amount > 0)
			{
				fight.hits++;
				fight.damageDealt += amount;
				fight.maxHit = Math.max(fight.maxHit, amount);
			}
			else
			{
				fight.misses++;
			}
		}
		else if (active != null && actor == client.getLocalPlayer() && Hitsplats.isDamageTaken(type))
		{
			// Taking damage deliberately does not count as activity: tanking without attacking is
			// idle time as far as DPS is concerned.
			active.damageTaken += amount;
		}
	}

	public void onActorDeath(ActorDeath event)
	{
		if (!(event.getActor() instanceof NPC))
		{
			return;
		}

		NPC npc = (NPC) event.getActor();

		if (active != null && active.liveMembers.remove(npc))
		{
			// A paired encounter is only over once both halves are down.
			if (active.liveMembers.isEmpty())
			{
				complete(client.getTickCount());
			}
			return;
		}

		// Death normally arrives before the despawn, but if the order is ever reversed the fight
		// would already be parked and the kill would be thrown away when the grace period expired.
		if (parked != null && parked.encounter == bossRegistry.encounterFor(npc))
		{
			active = parked;
			parked = null;
			complete(client.getTickCount());
		}
	}

	/**
	 * A boss that changes form is reported as the same {@link NPC} object with a new id, so the
	 * fight carries on untouched. Logged only to establish which bosses behave this way and which
	 * despawn instead.
	 */
	public void onNpcChanged(NpcChanged event)
	{
		if (active != null && active.liveMembers.contains(event.getNpc()))
		{
			log.debug("{} transformed in place, fight continues", active.name);
		}
	}

	public void onNpcDespawned(NpcDespawned event)
	{
		if (active == null || !active.liveMembers.remove(event.getNpc()))
		{
			return;
		}

		if (!active.liveMembers.isEmpty())
		{
			// The other half of a pair is still up, so the fight carries on.
			return;
		}

		// A despawn is not necessarily the end: a phased boss may vanish and return as a fresh
		// NPC. The fight is parked rather than discarded, and resumes if the same encounter is
		// engaged again shortly afterwards.
		parked = active;
		parkedAtTick = client.getTickCount();
		active = null;
		log.debug("Parked fight against {} after despawn", parked.name);
	}

	public void onGameTick()
	{
		int tick = client.getTickCount();

		if (active != null && tick - active.lastActivityTick > ABANDON_AFTER_TICKS)
		{
			log.debug("Abandoning stale fight against {}", active.name);
			active = null;
		}

		if (parked != null && tick - parkedAtTick > RESUME_GRACE_TICKS)
		{
			log.debug("Discarding parked fight against {}: did not return", parked.name);
			parked = null;
		}
	}

	private FightState fightFor(NPC npc, int tick)
	{
		BossRegistry.Encounter encounter = bossRegistry.encounterFor(npc);
		if (encounter == null)
		{
			return null;
		}

		if (active != null)
		{
			if (active.encounter != encounter)
			{
				return null;
			}
			track(active, npc, encounter);
			return active;
		}

		if (parked != null && parked.encounter == encounter)
		{
			// Same encounter back again within the grace period: carry on the fight it was already
			// in rather than starting a second one. The time it was away is deducted as idle by the
			// activity bookkeeping, so vanishing does not flatter the DPS.
			active = parked;
			parked = null;
			track(active, npc, encounter);
			log.debug("Resumed fight against {}", active.name);
			return active;
		}

		// Hitting an add first does not open a fight; the boss itself has to be engaged.
		if (!encounter.isMember(npc.getName()))
		{
			return null;
		}

		active = new FightState(encounter, npc, tick);
		log.debug("Started fight against {}", active.name);
		return active;
	}

	/**
	 * Records a member as part of the fight so its death is what ends the encounter. Adds are
	 * deliberately not tracked here: their damage counts, but killing one proves nothing.
	 */
	private static void track(FightState fight, NPC npc, BossRegistry.Encounter encounter)
	{
		if (encounter.isMember(npc.getName()))
		{
			fight.liveMembers.add(npc);
		}
	}

	private void registerActivity(FightState fight, int tick)
	{
		int gap = tick - fight.lastActivityTick;
		if (gap > idleTimeoutTicks())
		{
			fight.idleTicks += gap;
		}
		fight.lastActivityTick = tick;
	}

	private void complete(int tick)
	{
		FightState fight = active;
		active = null;

		// A boss killed within a single tick took no measurable time. Clamping that to one tick
		// would invent 0.6 seconds to divide by and report a DPS that was never measured.
		int duration = Math.max(0, tick - fight.startTick - fight.idleTicks);

		BossFight result = new BossFight(
			fight.name,
			fight.combatLevel,
			fight.damageDealt,
			fight.damageTaken,
			fight.hits,
			fight.misses,
			fight.maxHit,
			duration,
			Instant.now());

		log.debug("Completed fight: {}", result);
		fightListener.accept(result);
	}

	private int idleTimeoutTicks()
	{
		return Math.max(1, config.idleTimeoutTicks());
	}

	private static final class FightState
	{
		private final BossRegistry.Encounter encounter;
		private final String name;
		private final int combatLevel;
		private final int startTick;

		/**
		 * Members engaged and not yet dead. The encounter ends when this empties, which is what
		 * makes a paired boss one kill rather than two half fights.
		 */
		private final Set<NPC> liveMembers = new HashSet<>();

		private int lastActivityTick;
		private int idleTicks;
		private int damageDealt;
		private int damageTaken;
		private int hits;
		private int misses;
		private int maxHit;

		private FightState(BossRegistry.Encounter encounter, NPC first, int startTick)
		{
			this.encounter = encounter;
			this.name = encounter.getName();
			this.combatLevel = first.getCombatLevel();
			this.startTick = startTick;
			this.lastActivityTick = startTick;
			this.liveMembers.add(first);
		}
	}
}
