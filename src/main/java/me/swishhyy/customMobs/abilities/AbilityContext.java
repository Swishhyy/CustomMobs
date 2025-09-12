package me.swishhyy.customMobs.abilities;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;

public class AbilityContext {
    private final LivingEntity caster;
    private final LivingEntity target;
    private final Event triggerEvent;

    public AbilityContext(LivingEntity caster, LivingEntity target, Event triggerEvent) {
        this.caster = caster;
        this.target = target;
        this.triggerEvent = triggerEvent;
    }

    public LivingEntity getCaster() { return caster; }
    public LivingEntity getTarget() { return target; }
    public Event getTriggerEvent() { return triggerEvent; }
}
