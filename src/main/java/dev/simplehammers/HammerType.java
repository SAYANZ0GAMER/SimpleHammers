package dev.simplehammers;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;

public enum HammerType {
    STONE("stone", "Stone Hammer", NamedTextColor.GRAY, "STONE_PICKAXE", Material.STONE_PICKAXE, 330),
    COPPER("copper", "Copper Hammer", TextColor.color(0xE77C56), "COPPER_PICKAXE", Material.IRON_PICKAXE, 474),
    IRON("iron", "Iron Hammer", NamedTextColor.WHITE, "IRON_PICKAXE", Material.IRON_PICKAXE, 624),
    GOLDEN("golden", "Golden Hammer", NamedTextColor.GOLD, "GOLDEN_PICKAXE", Material.GOLDEN_PICKAXE, 79),
    DIAMOND("diamond", "Diamond Hammer", NamedTextColor.AQUA, "DIAMOND_PICKAXE", Material.DIAMOND_PICKAXE, 2601),
    NETHERITE("netherite", "Netherite Hammer", TextColor.color(0x4D494D), "NETHERITE_PICKAXE", Material.NETHERITE_PICKAXE, 3385);

    private final String id;
    private final String displayName;
    private final TextColor color;
    private final Material baseMaterial;
    private final int defaultDurability;

    HammerType(String id, String displayName, TextColor color, String baseName, Material fallback, int defaultDurability) {
        this.id = id;
        this.displayName = displayName;
        this.color = color;
        Material m = Material.matchMaterial(baseName); // COPPER_PICKAXE only exists on 1.21.9+
        this.baseMaterial = m != null ? m : fallback;
        this.defaultDurability = defaultDurability;
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public TextColor color() { return color; }
    public Material baseMaterial() { return baseMaterial; }
    public int defaultDurability() { return defaultDurability; }

    public static HammerType fromId(String s) {
        if (s == null) return null;
        for (HammerType t : values()) if (t.id.equalsIgnoreCase(s)) return t;
        return null;
    }
}
