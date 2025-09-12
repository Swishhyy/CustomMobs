package me.swishhyy.customMobs.abilities;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.bukkit.configuration.ConfigurationSection;

public final class AbilityRegistry {
    public static final AbilityRegistry INSTANCE = new AbilityRegistry();
    private final Map<String, Function<ConfigurationSection, Ability>> factories = new HashMap<>();

    private AbilityRegistry() { registerDefaults(); }

    private void registerDefaults() {
        register("POISON_TOUCH", section -> new PoisonTouchAbility(
                section.getInt("duration", 100),
                section.getInt("amplifier", 1)
        ));
    }

    public void register(String type, Function<ConfigurationSection, Ability> factory) {
        factories.put(type.toUpperCase(), factory);
    }

    public Ability create(ConfigurationSection abilitySection) {
        if (abilitySection == null) return null;
        String type = abilitySection.getString("type");
        if (type == null) return null;
        Function<ConfigurationSection, Ability> f = factories.get(type.toUpperCase());
        return f == null ? null : f.apply(abilitySection);
    }
}
