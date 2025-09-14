package me.swishhyy.customMobs.integration;

import me.swishhyy.customMobs.CustomMobs;
import me.swishhyy.customMobs.mob.MobDefinition;
import me.swishhyy.customMobs.mob.MobManager;
import me.swishhyy.customMobs.mob.MoneySpec;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.lang.reflect.Method;

/**
 * MoneyFromMobs integration via reflection so the plugin compiles without the API present.
 * Listens to all events and filters the two MoneyFromMobs events by class name.
 */
public final class MoneyFromMobsHook implements Listener {
    private final CustomMobs plugin;
    private final MobManager mobManager;

    private static final String ATTEMPT_EVENT = "com.github.chocolf.moneyfrommobs.api.events.AttemptToDropMoneyEvent";
    private static final String DROP_EVENT = "com.github.chocolf.moneyfrommobs.api.events.DropMoneyEvent";

    public MoneyFromMobsHook(CustomMobs plugin, MobManager mobManager) { this.plugin = plugin; this.mobManager = mobManager; }

    public void register() { plugin.getServer().getPluginManager().registerEvents(this, plugin); }

    @EventHandler
    public void onAny(Event event) {
        String name = event.getClass().getName();
        try {
            if (ATTEMPT_EVENT.equals(name)) handleAttempt(event);
            else if (DROP_EVENT.equals(name)) handleDrop(event);
        } catch (Throwable t) {
            // swallow to avoid breaking other events
        }
    }

    private void handleAttempt(Object evt) throws Exception {
        MobDefinition def = getDefinition(evt, "getEntity");
        if (def == null) return;
        MoneySpec ms = def.money();
        if (ms == null) return;
        Method setChance = evt.getClass().getMethod("setDropChance", double.class);
        setChance.invoke(evt, ms.chancePercent());
    }

    private void handleDrop(Object evt) throws Exception {
        MobDefinition def = getDefinition(evt, "getEntity");
        if (def == null) return;
        MoneySpec ms = def.money();
        if (ms == null) return;
        Method setAmount = evt.getClass().getMethod("setAmount", double.class);
        Method setDrops = evt.getClass().getMethod("setNumberOfDrops", int.class);
        setAmount.invoke(evt, ms.randomAmount());
        setDrops.invoke(evt, ms.randomDrops());
    }

    private MobDefinition getDefinition(Object evt, String entityGetterName) throws Exception {
        Method getEntity = evt.getClass().getMethod(entityGetterName);
        Object entity = getEntity.invoke(evt);
        if (!(entity instanceof org.bukkit.entity.LivingEntity le)) return null;
        return mobManager.getDefinitionFromEntity(le);
    }
}
