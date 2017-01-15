package com.hepolite.mmob.mobs;

import java.util.AbstractMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * The mob stat tracker tracks certain stats for the mob it is associated with, such as damage dealt, current health and so on
 */
public class MobStatTracker {
    // Control variables
    private MalevolentMob mob = null;

    private int currentTime = 0;

    // Stats
    private final Stat damageDealt = new Stat(-1);
    private final Stat damageTaken = new Stat(-1);
    private final Stat health = new Stat(200);

    public MobStatTracker(final MalevolentMob mob) {
        this.mob = mob;
    }

    // ///////////////////////////////////////////////////////////////////////////////////////
    // CORE FUNCTIONALITY // CORE FUNCTIONALITY // CORE FUNCTIONALITY // CORE FUNCTIONALITY //
    // ///////////////////////////////////////////////////////////////////////////////////////

    /** The on-tick method is called each and every tick from the associated Malevolent Mob */
    public void onTick() {
        recordHealth();
        currentTime++;
    }

    /** Called whenever the mob is attacking a certain target for whatever reason */
    public void recordAttackOnTarget(final EntityDamageEvent event, final double rawDamage) {
        damageDealt.record(new double[] { rawDamage, event.getDamage() }, currentTime);
    }

    /** Called whenever the mob is attacked by a certain target for whatever reason */
    public void recordAttackFromTarget(final EntityDamageEvent event, final double rawDamage) {
        damageTaken.record(new double[] { rawDamage, event.getDamage() }, currentTime);
    }

    /** Called to record the mob's current health value */
    @SuppressWarnings("deprecation")
    public void recordHealth() {
        final LivingEntity entity = mob.getEntity();
        health.record(new double[] { entity.getHealth(), entity.getMaxHealth() }, currentTime);
    }

    /** Return the mob's estimated damage per second over the time duration */
    public double getDPS(double timeDuration) {
        if (timeDuration <= 0.0)
            timeDuration = 1.0;

        // Obtain the data needed from the stat tracker
        final LinkedList<Entry<Integer, Object>> damageResult = damageDealt.getEntries(currentTime, (int) (20.0 * timeDuration));

        double damage = 0.0;
        for (final Entry<Integer, Object> entry : damageResult) {
            final double[] damages = (double[]) entry.getValue();
            damage += damages[1];
        }
        return damage / timeDuration;
    }

    /** Get the mob's estimated health change per second over the time duration */
    public double getHPS(double timeDuration) {
        if (timeDuration <= 0.0)
            timeDuration = 1.0;

        // Obtain the data needed from the stat tracker
        final LinkedList<Entry<Integer, Object>> healthResult = health.getEntries(currentTime, (int) (20.0 * timeDuration));

        double health = 0.0;
        for (final Entry<Integer, Object> entry : healthResult) {
            final double[] healthValues = (double[]) entry.getValue();
            health += healthValues[0];
        }
        return mob.getEntity().getHealth() - health / healthResult.size();
    }

    // /////////////////////////////////////////////////////////////////////////////////////////
    // STAT DATA TYPE // STAT DATA TYPE // STAT DATA TYPE // STAT DATA TYPE // STAT DATA TYPE //
    // /////////////////////////////////////////////////////////////////////////////////////////

    /** The stat class will take care of holding data and trimming it to the specified parameters */
    private class Stat {
        // Control variables
        private int timeToTrackStatsFor = -1;

        private final LinkedList<Entry<Integer, Object>> dataList = new LinkedList<Entry<Integer, Object>>();

        /**
         * @maxTimeToTrackFor Duration any data point will be tracked for, -1 for never removing any data, that number for that number of ticks to maintain data
         */
        public Stat(final int timeToTrackStatsFor) {
            this.timeToTrackStatsFor = timeToTrackStatsFor;
        }

        /** Records a data point at the specified point in time. Erases all entries older than "time" minus "timeToTrackStatsFor", unless set to -1. Assumes that time is ALWAYS equal or greater than the previous entry */
        public void record(final Object value, final int time) {
            // Find and delete entries older than the specified time, if relevant
            if (timeToTrackStatsFor != -1)
                for (final Iterator<Entry<Integer, Object>> it = dataList.iterator(); it.hasNext();) {
                    final Entry<Integer, Object> entry = it.next();
                    if (entry.getKey() < time - timeToTrackStatsFor)
                        it.remove();
                    else
                        break;
                }

            // Store the entry
            dataList.add(new AbstractMap.SimpleEntry<Integer, Object>(time, value));
        }

        /** Returns all entries that are more recent than "time" old. Returns an empty list if there were no results. If time is -1, then all entries are returned */
        public LinkedList<Entry<Integer, Object>> getEntries(final int time, final int duration) {
            if (dataList.size() == 0)
                return new LinkedList<Entry<Integer, Object>>();
            if (time == -1)
                return new LinkedList<Entry<Integer, Object>>(dataList);

            final int timeThreshold = time - duration;
            final LinkedList<Entry<Integer, Object>> list = new LinkedList<Entry<Integer, Object>>();
            for (final Iterator<Entry<Integer, Object>> it = dataList.descendingIterator(); it.hasNext();) {
                final Entry<Integer, Object> entry = it.next();
                if (entry.getKey() < timeThreshold)
                    break;
                list.add(entry);
            }
            return list;
        }
    }
}
