package me.swishhyy.customMobs.skills;

import java.util.Collections;
import java.util.List;
import java.util.Collection;

public final class SkillNode {
    private final String name;
    private final SkillAction action;
    private final Targeter targeter; // optional
    private final List<SkillCondition> conditions; // all must pass

    public SkillNode(String name, SkillAction action, Targeter targeter, List<SkillCondition> conditions) {
        this.name = name;
        this.action = action;
        this.targeter = targeter;
        this.conditions = conditions == null ? Collections.emptyList() : Collections.unmodifiableList(conditions);
    }

    public void execute(SkillContext base) {
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
