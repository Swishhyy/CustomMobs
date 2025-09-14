package me.swishhyy.customMobs.integration;

import me.swishhyy.customMobs.CustomMobs;
import me.swishhyy.customMobs.mob.MobManager;
import me.swishhyy.customMobs.mob.MobDefinition;
import me.swishhyy.customMobs.mob.MoneySpec;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

// MoneyFromMobs API imports (compileOnly)
import com.github.chocolf.moneyfrommobs.api.events.AttemptToDropMoneyEvent;
import com.github.chocolf.moneyfrommobs.api.events.DropMoneyEvent;

/** Hooks into MoneyFromMobs if present to override per-mob money drops. */
public final class MoneyFromMobsHook implements Listener {
    private final CustomMobs plugin;
    private final MobManager mobManager;

    public MoneyFromMobsHook(CustomMobs plugin, MobManager mobManager) {
        this.plugin = plugin;
        this.mobManager = mobManager;
    }

    public void register() { plugin.getServer().getPluginManager().registerEvents(this, plugin); }

    @EventHandler
    public void onAttempt(AttemptToDropMoneyEvent e) {
        MobDefinition def = mobManager.getDefinitionFromEntity(e.getEntity());
        if (def == null) return;
        MoneySpec ms = def.money();
        if (ms == null) return;
        e.setDropChance(ms.chancePercent());
    }

    @EventHandler
    public void onDrop(DropMoneyEvent e) {
        MobDefinition def = mobManager.getDefinitionFromEntity(e.getEntity());
        if (def == null) return;
        MoneySpec ms = def.money();
        if (ms == null) return;
        e.setAmount(ms.randomAmount());
        e.setNumberOfDrops(ms.randomDrops());
    }
}

