package com.bossfightlog;

import java.time.Instant;
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
		if (active != null && event.getActor() == active.npc)
		{
			complete(client.getTickCount());
			return;
		}

		// Death normally arrives before the despawn, but if the order is ever reversed the fight
		// would already be parked and the kill would be thrown away when the grace period expired.
		if (parked != null && event.getActor() == parked.npc)
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
		if (active != null && event.getNpc() == active.npc)
		{
			log.debug("{} transformed in place, fight continues", active.name);
		}
	}

	public void onNpcDespawned(NpcDespawned event)
	{
		if (active != null && event.getNpc() == active.npc)
		{
			// A despawn is not necessarily the end: a phased boss may vanish and return as a fresh
			// NPC. The fight is parked rather than discarded, and resumes if the same boss is
			// engaged again shortly afterwards.
			parked = active;
			parkedAtTick = client.getTickCount();
			active = null;
			log.debug("Parked fight against {} after despawn", parked.name);
		}
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
		if (active != null)
		{
			return active.npc == npc ? active : null;
		}

		if (parked != null && parked.name.equals(npc.getName()))
		{
			// Same boss back again within the grace period: carry on the fight it was already in
			// rather than starting a second one. The time it was away is deducted as idle by the
			// activity bookkeeping, so vanishing does not flatter the DPS.
			active = parked;
			active.npc = npc;
			parked = null;
			log.debug("Resumed fight against {}", active.name);
			return active;
		}

		if (!bossRegistry.isBoss(npc))
		{
			return null;
		}

		active = new FightState(npc, tick);
		log.debug("Started fight against {}", active.name);
		return active;
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

		int duration = Math.max(1, tick - fight.startTick - fight.idleTicks);

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
		/**
		 * Reassigned when a fight resumes against a boss that despawned and came back.
		 */
		private NPC npc;
		private final String name;
		private final int combatLevel;
		private final int startTick;

		private int lastActivityTick;
		private int idleTicks;
		private int damageDealt;
		private int damageTaken;
		private int hits;
		private int misses;
		private int maxHit;

		private FightState(NPC npc, int startTick)
		{
			this.npc = npc;
			this.name = npc.getName();
			this.combatLevel = npc.getCombatLevel();
			this.startTick = startTick;
			this.lastActivityTick = startTick;
		}
	}
}
