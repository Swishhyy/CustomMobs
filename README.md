# CustomMobs – Documentation

---
## 1. Mob Definition (`custom/<id>.yml`)
Filename (without .yml) = mob id (lowercased internally). Fields may appear at root or under `attributes:` (faction & behavior allowed in either; root wins on conflict).

| Field | Type | Example | Note |
|-------|------|---------|------|
| type | EntityType | `ZOMBIE` | Bukkit enum |
| display | String | `"&aBoss"` | Legacy `&` colors |
| faction | String | `UNDEAD` | Must exist in factions.yml |
| behavior | Enum | `HOSTILE` | PASSIVE / NEUTRAL / HOSTILE |
| passive_ai | Bool | `false` | Placeholder |
| max_health | Number | `60` | Base health |
| attack_damage | Number | `8` | Base attack |
| movement_speed | Number | `0.30` | Movement speed |
| follow_range | Number | `40` | Aggro radius |
| armor | Number | `4` | Armor points |
| armor_toughness | Number | `2` | Armor toughness |
| knockback_resistance | Number | `0.1` | 0–1 |
| min_level | Int | `1` | Level roll min |
| max_level | Int | `5` | Level roll max |
| health_per_level | Number | `4` | Added each level >1 |
| attack_per_level | Number | `1` | Added each level >1 |

Random range keys (string `min-max` inside `attributes:`):  
`max_health_range`, `attack_damage_range`, `movement_speed_range`, `follow_range_range`, `armor_range`, `armor_toughness_range`, `knockback_resistance_range`.

### Abilities
`abilities.onSpawn.*`, `abilities.onHit.*`, `abilities.onDamaged.*` → each child needs `type: <ABILITY_KEY>` plus its options.

### Skills (optional / advanced)
`skills.<TRIGGER>.<nodeName>` with: `action`, optional `cooldownMs|cooldown|cooldownSeconds`, optional `conditions: {}`, optional `targeter: {}`.  
Triggers: `ON_SPAWN`, `ON_HIT`, `ON_DAMAGED`.

### Damage Modifiers
`damage_modifiers.<DAMAGE_CAUSE>: <number>` → multiplier (≤0 immune, 1 normal, >1 amplify).

### Equipment
```
equipment:
  set: { token: IRON_ARMOR, chance: 35, dropChance: 5 }
  pieces:
    mainhand:
      type: IRON_SWORD
      chance: 60
      dropChance: 10
      enchantFile: zombie_sword_enchants.yml
```
Piece keys: helmet/head, chest/chestplate, legs/leggings, boots/feet, mainhand/hand/weapon, offhand/shield.

### Natural Spawn (optional)
```
natural:
  enabled: true
  chance: 0.08     # fraction or percent >1
  replace: false
  weight: 1.5
  cap_chunk: 6
  biomes: [ FOREST, PLAINS ]
```

---
## 2. Factions (`factions.yml`)
| Key | Meaning |
|-----|---------|
| `<faction>.allies` | Friendly factions (no custom mob damage) |
| `<faction>.hostiles` | Hostile factions (provokes NEUTRAL if attacked) |
| `<faction>.default_behavior` | Behavior fallback |

Behavior resolution: mob `behavior` → faction `default_behavior` → HOSTILE.

---
## 3. Enchant Config (`enchants/<name>.yml`)
```
config:
  max_enchants: 3          # 0/omit = unlimited
  groups:
    offensive:
      pick: 1              # weighted picks, no replacement
      options:
        SHARPNESS: { weight: 6, min: 3, max: 5, chance: 90 }
        FIRE_ASPECT: { weight: 2, level: 2, chance: 35 }
# After groups (skips duplicates)
enchantments:
  LOOTING: { min: 2, max: 3, chance: 40 }
```
Fields: `weight`, `level` OR `min`+`max`, `chance` (fraction or >1%); `pick` = number chosen per group.

---
## 4. Ability & Skill Triggers Mapping
| Simple Abilities | Skill System |
|------------------|-------------|
| onSpawn | ON_SPAWN |
| onHit | ON_HIT |
| onDamaged | ON_DAMAGED |

---
## 5. Behavior Summary
| Behavior | Targets? | Deals Damage? | Transition |
|----------|----------|---------------|------------|
| PASSIVE | No | No | Static |
| NEUTRAL (fresh) | No | No | Becomes provoked when hit by qualifying source |
| NEUTRAL (provoked) | Yes (attacker) | Yes | Stays provoked until death |
| HOSTILE | Yes | Yes | Always |

Provoked state stored per entity (not cleared automatically).

---
## 6. Examples
### 6.1 Minimal Hostile
```
type: ZOMBIE
attributes:
  max_health: 30
  attack_damage: 5
  faction: UNDEAD
  behavior: HOSTILE
```

### 6.2 Passive Decorative
```
type: COW
attributes:
  max_health: 25
  behavior: PASSIVE
  faction: villagers
```

### 6.3 Neutral Guard (Provokes on Hit)
```
type: IRON_GOLEM
attributes:
  max_health: 120
  attack_damage: 18
  faction: villagers
  behavior: NEUTRAL
```

### 6.4 Attribute Ranges + Level Scaling
```
type: HUSK
attributes:
  max_health: 50
  max_health_range: 45-65
  attack_damage: 7
  attack_damage_range: 6-10
  movement_speed: 0.30
  movement_speed_range: 0.26-0.34
min_level: 2
max_level: 6
health_per_level: 4
attack_per_level: 1
```

### 6.5 Abilities + Damage Modifiers
```
type: SPIDER
attributes:
  max_health: 32
  faction: mobs
abilities:
  onHit:
    venom:
      type: POISON_TOUCH
      duration: 60
      amplifier: 1
damage_modifiers:
  POISON: 0
  FALL: 0.5
```

### 6.6 Skills Example (Cooldown Action)
```
skills:
  ON_HIT:
    slowingPulse:
      action: SLOW_FIELD
      cooldownSeconds: 8
      targeter: { radius: 5 }
```

### 6.7 Equipment + Enchants + Natural
```
type: ZOMBIE
attributes:
  max_health: 70
  faction: UNDEAD
  behavior: HOSTILE
equipment:
  set:
    token: IRON_ARMOR
    chance: 25
    dropChance: 5
  pieces:
    mainhand:
      type: IRON_SWORD
      chance: 55
      dropChance: 8
      enchantFile: zombie_sword_enchants.yml
natural:
  enabled: true
  chance: 0.05
  weight: 2.0
  biomes: [ FOREST, PLAINS ]
```

### 6.8 Factions Extension Example
```
forest:
  allies: [ forest, villagers ]
  hostiles: [ undead ]
  default_behavior: NEUTRAL
raiders:
  allies: [ raiders ]
  hostiles: [ villagers, forest ]
  default_behavior: HOSTILE
```

### 6.9 Sword Enchant File
```
config:
  max_enchants: 3
  groups:
    offensive:
      pick: 1
      options:
        SHARPNESS: { weight: 6, min: 3, max: 5, chance: 90 }
        FIRE_ASPECT: { weight: 2, level: 2, chance: 35 }
    utility:
      pick: 1
      options:
        UNBREAKING: { weight: 5, min: 2, max: 3 }
        MENDING: { weight: 1, level: 1, chance: 10 }
enchantments:
  LOOTING: { min: 2, max: 3, chance: 40 }
```

### 6.10 Armor Enchant File
```
config:
  max_enchants: 2
  groups:
    defense:
      pick: 1
      options:
        PROTECTION: { weight: 5, min: 3, max: 4 }
        BLAST_PROTECTION: { weight: 2, min: 3, max: 4 }
    sustain:
      pick: 1
      options:
        UNBREAKING: { weight: 4, min: 2, max: 3 }
        MENDING: { weight: 1, level: 1, chance: 20 }
enchantments:
  THORNS: { min: 1, max: 3, chance: 25 }
```

### 6.11 Simple Ability Only (Drop‑in)
```
abilities:
  onHit:
    burn:
      type: POISON_TOUCH
      duration: 40
      amplifier: 1
```

---
