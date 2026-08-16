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

	private final Client client;
	private final BossFightLogConfig config;
	private final BossRegistry bossRegistry;

	private FightState active;
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
		}
	}

	public void onNpcDespawned(NpcDespawned event)
	{
		if (active != null && event.getNpc() == active.npc)
		{
			// A despawn without a death means the boss was lost rather than killed. Transforming
			// bosses also land here, which is why they are not in the registry yet.
			log.debug("Discarding fight against {}: despawned without dying", active.name);
			active = null;
		}
	}

	public void onGameTick()
	{
		if (active != null && client.getTickCount() - active.lastActivityTick > ABANDON_AFTER_TICKS)
		{
			log.debug("Abandoning stale fight against {}", active.name);
			active = null;
		}
	}

	private FightState fightFor(NPC npc, int tick)
	{
		if (active != null)
		{
			return active.npc == npc ? active : null;
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
		private final NPC npc;
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
