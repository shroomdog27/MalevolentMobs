package com.hepolite.mmob.abilities.passives;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.hepolite.mmob.abilities.PassiveAura;
import com.hepolite.mmob.mobs.MalevolentMob;
import com.hepolite.mmob.settings.Settings;
import com.hepolite.mmob.utility.Common;
import com.hepolite.mmob.utility.ParticleEffect;
import com.hepolite.mmob.utility.ParticleEffect.ParticleType;

/**
 * The deteriorating aura will negate the regeneration effect for players, preventing them from recovering health by normal means. Potions of health will not be prevented by this passive
 */
public class PassiveDeterioratingAura extends PassiveAura {
    private final HashMap<String, Double> playerHealthMap = new HashMap<String, Double>();
    private final HashSet<String> playersInRange = new HashSet<String>();

    public PassiveDeterioratingAura(final MalevolentMob mob, final float scale) {
        super(mob, "Deteriorating Aura", scale);
        updateTime = 0;
    }

    @Override
    public void loadFromConfig(final Settings settings, final Settings alternative) {
        super.loadFromConfig(settings, alternative);
        affectPlayersOnly = true;
    }

    @Override
    public void onTick() {
        super.onTick();

        // Find all players not in the vicinity and remove them from the health map
        for (final Iterator<String> it = playerHealthMap.keySet().iterator(); it.hasNext();)
            if (!playersInRange.contains(it.next()))
                it.remove();
        playersInRange.clear();
    }

    @Override
    protected void applyAuraEffect(final LivingEntity entity) {
        // If the player was detected in the health map, figure out if the player gained health
        final String player = ((Player) entity).getName();
        if (playerHealthMap.containsKey(player)) {
            final double oldHealth = playerHealthMap.get(player);
            if (entity.getHealth() > oldHealth && entity.getHealth() <= oldHealth + 1.0) {
                // Take health from the player and heal self
                final double healedAmount = entity.getHealth() - oldHealth;
                Common.doDamage(healedAmount, entity, mob.getEntity(), DamageCause.MAGIC);
            }
        }

        playerHealthMap.put(player, entity.getHealth());
        playersInRange.add(player);
    }

    @Override
    protected void displayAura(final Location location, final float range) {
        ParticleEffect.play(ParticleType.REDSTONE, location, 0.0f, (int) (5.0f * range), 0.5f * range);
    }
}
