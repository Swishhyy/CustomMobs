package me.swishhyy.customMobs.abilities;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.bukkit.configuration.ConfigurationSection;

public class AbilityRegistry {
    private final Map<String, Function<ConfigurationSection, Ability>> factories = new HashMap<>();

    public AbilityRegistry() {
        registerDefaults();
    }

    private void registerDefaults() {
        register("POISON_TOUCH", section -> {
            int duration = section.getInt("duration", 100);
            int amplifier = section.getInt("amplifier", 1);
            return new PoisonTouchAbility(duration, amplifier);
        });
    }

    public void register(String type, Function<ConfigurationSection, Ability> factory) {
        factories.put(type.toUpperCase(), factory);
    }

    // New preferred method
    public Ability create(ConfigurationSection abilitySection) {
        if (abilitySection == null) return null;
        String type = abilitySection.getString("type");
        if (type == null) return null;
        Function<ConfigurationSection, Ability> f = factories.get(type.toUpperCase());
        if (f == null) return null;
        return f.apply(abilitySection);
    }

    // Backwards compatibility with old signature (root, abilitySection)
    public Ability create(ConfigurationSection root, ConfigurationSection abilitySection) {
        return create(abilitySection);
    }
}
