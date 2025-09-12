package me.swishhyy.customMobs.listener;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.EventPriority;

import me.swishhyy.customMobs.mob.MobManager;
import me.swishhyy.customMobs.mob.MobDefinition;
import me.swishhyy.customMobs.mob.DropSpec;
import me.swishhyy.customMobs.skills.SkillRuntime;

public class MobListener implements Listener {
    private final MobManager mobManager;

    public MobListener(MobManager mobManager) { this.mobManager = mobManager; }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof LivingEntity && e.getEntity() instanceof LivingEntity) {
            LivingEntity attacker = (LivingEntity) e.getDamager();
            LivingEntity victim = (LivingEntity) e.getEntity();
            MobDefinition attDef = mobManager.getDefinitionFromEntity(attacker);
            MobDefinition vicDef = mobManager.getDefinitionFromEntity(victim);
            if (attDef != null && vicDef != null) {
                String af = attDef.faction() == null ? "mobs" : attDef.faction();
                String vf = vicDef.faction() == null ? "mobs" : vicDef.faction();
                if (af.equalsIgnoreCase(vf)) { e.setCancelled(true); return; }
            }
            mobManager.handleHit(attacker, victim, e);
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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamageModifier(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof LivingEntity le)) return;
        MobDefinition def = mobManager.getDefinitionFromEntity(le);
        if (def == null) return;
        Double mult = def.damageMultipliers().get(e.getCause());
        if (mult == null) return;
        if (mult <= 0) { e.setCancelled(true); return; }
        e.setDamage(e.getDamage() * mult);
    }

    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        LivingEntity le = e.getEntity();
        MobDefinition def = mobManager.getDefinitionFromEntity(le);
        if (def == null) return;
        // Clear skill cooldowns
        SkillRuntime.clear(le);
        // Custom drops override if defined
        if (!def.drops().isEmpty()) {
            e.getDrops().clear();
            for (DropSpec spec : def.drops()) {
                if (spec.roll()) {
                    var item = spec.createStack();
                    if (item != null) e.getDrops().add(item);
                }
            }
        }
    }
}
