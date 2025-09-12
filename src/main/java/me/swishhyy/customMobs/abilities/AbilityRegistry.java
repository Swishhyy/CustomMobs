package me.swishhyy.customMobs.abilities;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import org.bukkit.configuration.ConfigurationSection;

public class AbilityRegistry {
    private final Map<String, BiFunction<ConfigurationSection, String, Ability>> factories = new HashMap<>();

    public AbilityRegistry() {
        registerDefaults();
    }

    private void registerDefaults() {
        // id: POISON_TOUCH
        register("POISON_TOUCH", (section, path) -> {
            int duration = section.getInt(path + ".duration", 100);
            int amplifier = section.getInt(path + ".amplifier", 1);
            return new PoisonTouchAbility(duration, amplifier);
        });
    }

    public void register(String type, BiFunction<ConfigurationSection, String, Ability> factory) {
        factories.put(type.toUpperCase(), factory);
    }

    public Ability create(ConfigurationSection root, ConfigurationSection abilitySection) {
        String type = abilitySection.getString("type");
        if (type == null) return null;
        BiFunction<ConfigurationSection, String, Ability> f = factories.get(type.toUpperCase());
        if (f == null) return null;
        return f.apply(abilitySection, "");
    }
}
