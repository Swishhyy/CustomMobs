package me.swishhyy.customMobs.listener;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import me.swishhyy.customMobs.mob.MobManager;

public class MobListener implements Listener {
    private final MobManager mobManager;

    public MobListener(MobManager mobManager) {
        this.mobManager = mobManager;
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof LivingEntity && e.getEntity() instanceof LivingEntity) {
            mobManager.handleHit((LivingEntity) e.getDamager(), (LivingEntity) e.getEntity(), e);
        }
        if (e.getEntity() instanceof LivingEntity && e.getDamager() instanceof LivingEntity) {
            mobManager.handleDamaged((LivingEntity) e.getEntity(), (LivingEntity) e.getDamager(), e);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent e) {
        if (!(e instanceof EntityDamageByEntityEvent) && e.getEntity() instanceof LivingEntity) {
            mobManager.handleDamaged((LivingEntity) e.getEntity(), null, e);
        }
    }
}
