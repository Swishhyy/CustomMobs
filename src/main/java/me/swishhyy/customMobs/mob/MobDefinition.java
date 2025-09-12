package me.swishhyy.customMobs.mob;

import java.util.ArrayList;
import java.util.List;

import me.swishhyy.customMobs.abilities.Ability;

public class MobDefinition {
    public String id;
    public String type; // ZOMBIE, SKELETON, etc
    public double health = 20.0;
    public double attack = 0.0;
    public String displayName;
    public List<Ability> onSpawn = new ArrayList<>();
    public List<Ability> onHit = new ArrayList<>();
    public List<Ability> onDamaged = new ArrayList<>();
}
