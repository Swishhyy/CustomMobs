package me.swishhyy.customMobs.skills;

import java.util.Collections;
import java.util.List;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

public final class SkillNode {
    private final String name;
    private final SkillAction action;
    private final Targeter targeter; // optional
    private final List<SkillCondition> conditions; // all must pass
    private final long cooldownMillis; // 0 = none

    public SkillNode(String name, SkillAction action, Targeter targeter, List<SkillCondition> conditions) {
        this(name, action, targeter, conditions, 0L);
    }

    public SkillNode(String name, SkillAction action, Targeter targeter, List<SkillCondition> conditions, long cooldownMillis) {
        this.name = name;
        this.action = action;
        this.targeter = targeter;
        this.conditions = conditions == null ? Collections.emptyList() : Collections.unmodifiableList(conditions);
        this.cooldownMillis = cooldownMillis;
    }

    public void execute(SkillContext base) {
        if (cooldownMillis > 0 && !SkillRuntime.tryAcquire(base.caster(), name, cooldownMillis)) return;
        for (SkillCondition c : conditions) if (!c.test(base)) return; // fail fast
        if (targeter == null) {
            action.execute(base);
            return;
        }
        Collection<org.bukkit.entity.LivingEntity> targets = targeter.resolve(base);
        if (targets == null || targets.isEmpty()) return;
        for (org.bukkit.entity.LivingEntity t : targets) {
            SkillContext ctx = new SkillContext(base.caster(), t, base.event(), base.definition());
            action.execute(ctx);
        }
    }

    public String name() { return name; }
}
