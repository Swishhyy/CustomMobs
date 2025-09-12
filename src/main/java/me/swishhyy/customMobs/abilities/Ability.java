package me.swishhyy.customMobs.abilities;

public interface Ability {
    String getType();
    void execute(AbilityContext ctx);
}
