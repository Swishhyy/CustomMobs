package me.swishhyy.customMobs.skills;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import me.swishhyy.customMobs.mob.MobDefinition;

public class SkillContext {
    private final LivingEntity caster;
    private final LivingEntity target;
    private final Event event;
    private final MobDefinition definition;

    public SkillContext(LivingEntity caster, LivingEntity target, Event event, MobDefinition def) {
        this.caster = caster; this.target = target; this.event = event; this.definition = def;
    }
    public LivingEntity caster() { return caster; }
    public LivingEntity target() { return target; }
    public Event event() { return event; }
    public MobDefinition definition() { return definition; }
}

