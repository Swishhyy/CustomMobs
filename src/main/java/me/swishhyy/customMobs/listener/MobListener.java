package me.swishhyy.customMobs.listener;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.entity.Mob;

import me.swishhyy.customMobs.mob.MobManager;
import me.swishhyy.customMobs.mob.MobDefinition;
import me.swishhyy.customMobs.mob.DropSpec;
import me.swishhyy.customMobs.skills.SkillRuntime;
import me.swishhyy.customMobs.faction.FactionManager;
import me.swishhyy.customMobs.mob.MobBehavior;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.entity.Player;

public class MobListener implements Listener {
    private final MobManager mobManager;
    private final FactionManager factionManager;

    public MobListener(MobManager mobManager, FactionManager factionManager) {
        this.mobManager = mobManager;
        this.factionManager = factionManager;
    }

    private NamespacedKey provokedKey() { return new NamespacedKey(mobManager.getPlugin(), "provoked"); }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof LivingEntity && e.getEntity() instanceof LivingEntity) {
            LivingEntity attacker = (LivingEntity) e.getDamager();
            LivingEntity victim = (LivingEntity) e.getEntity();
            MobDefinition attDef = mobManager.getDefinitionFromEntity(attacker);
            MobDefinition vicDef = mobManager.getDefinitionFromEntity(victim);

            // Faction friendly-fire cancel first
            if (attDef != null && vicDef != null) {
                String af = attDef.faction() == null ? "mobs" : attDef.faction();
                String vf = vicDef.faction() == null ? "mobs" : vicDef.faction();
                if (af.equalsIgnoreCase(vf) || (factionManager != null && factionManager.areAllied(af, vf))) {
                    e.setCancelled(true); return;
                }
            }

            // Behavior enforcement for attacker (custom mob only)
            if (attDef != null) {
                MobBehavior beh = attDef.behavior();
                if (beh == MobBehavior.PASSIVE) { e.setCancelled(true); return; }
                if (beh == MobBehavior.NEUTRAL) {
                    boolean provoked = attacker.getPersistentDataContainer().has(provokedKey(), PersistentDataType.BYTE);
                    if (!provoked) { e.setCancelled(true); return; }
                }
            }

            // Handle provocation: if victim is NEUTRAL custom mob and not yet provoked and attacker qualifies
            if (vicDef != null && vicDef.behavior() == MobBehavior.NEUTRAL) {
                boolean alreadyProvoked = victim.getPersistentDataContainer().has(provokedKey(), PersistentDataType.BYTE);
                if (!alreadyProvoked) {
                    boolean provoke = false;
                    if (attDef == null) {
                        // Player or vanilla mob hits it -> provoke
                        if (attacker instanceof Player) provoke = true; else provoke = true; // treat all external mobs as provoking
                    } else {
                        // Custom mob attacker: provoke if hostile by behavior or faction hostility
                        if (attDef.behavior() == MobBehavior.HOSTILE) provoke = true;
                        if (!provoke && factionManager != null) {
                            String af = attDef.faction() == null ? "mobs" : attDef.faction();
                            String vf = vicDef.faction() == null ? "mobs" : vicDef.faction();
                            if (factionManager.areHostile(af, vf)) provoke = true;
                        }
                    }
                    if (provoke) {
                        victim.getPersistentDataContainer().set(provokedKey(), PersistentDataType.BYTE, (byte)1);
                        if (victim instanceof Mob mob && attacker instanceof LivingEntity living && !living.isDead()) {
                            mob.setTarget(living);
                        }
                    }
                }
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

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTarget(EntityTargetLivingEntityEvent e) {
        if (!(e.getEntity() instanceof LivingEntity le)) return;
        if (!(e.getTarget() instanceof LivingEntity)) return;
        MobDefinition def = mobManager.getDefinitionFromEntity(le);
        if (def == null) return;
        MobBehavior beh = def.behavior();
        if (beh == MobBehavior.PASSIVE) {
            e.setCancelled(true);
            return;
        }
        if (beh == MobBehavior.NEUTRAL) {
            boolean provoked = le.getPersistentDataContainer().has(provokedKey(), PersistentDataType.BYTE);
            if (!provoked) {
                e.setCancelled(true);
            }
        }
    }
}
