package dev.simplehammers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class SimpleHammers extends JavaPlugin {

    private NamespacedKey hammerKey;
    private final List<NamespacedKey> recipeKeys = new ArrayList<>();
    private final Set<Material> blacklist = EnumSet.noneOf(Material.class);

    @Override
    public void onEnable() {
        saveDefaultConfig();
        hammerKey = new NamespacedKey(this, "hammer");
        loadSettings();
        registerRecipes();

        getServer().getPluginManager().registerEvents(new HammerListener(this), this);
        HammerCommand cmd = new HammerCommand(this);
        getCommand("hammer").setExecutor(cmd);
        getCommand("hammer").setTabCompleter(cmd);
    }

    @Override
    public void onDisable() {
        unregisterRecipes();
    }

    public void reload() {
        reloadConfig();
        loadSettings();
        unregisterRecipes();
        registerRecipes();
    }

    private void loadSettings() {
        blacklist.clear();
        for (String s : getConfig().getStringList("blacklist")) {
            Material m = Material.matchMaterial(s);
            if (m != null) blacklist.add(m);
        }
    }

    public Set<Material> blacklist() { return blacklist; }

    // ---------- Items ----------

    public boolean isEnabled(HammerType t) {
        return getConfig().getBoolean("hammers." + t.id() + ".enabled", true);
    }

    public int durability(HammerType t) {
        return Math.max(1, getConfig().getInt("hammers." + t.id() + ".durability", t.defaultDurability()));
    }

    public ItemStack createHammer(HammerType t) {
        ItemStack item = new ItemStack(t.baseMaterial());
        ItemMeta meta = item.getItemMeta();
        // item_name: the hammer's own name (not italic, still renamable in an anvil)
        meta.itemName(Component.text(t.displayName(), t.color()));
        meta.lore(List.of(Component.text("Breaks a 3x3x1 area", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
        if (meta instanceof Damageable d) d.setMaxDamage(durability(t));
        meta.getPersistentDataContainer().set(hammerKey, PersistentDataType.STRING, t.id());
        item.setItemMeta(meta);
        return item;
    }

    /** Returns the hammer type of an item, or null if it is not a hammer. */
    public HammerType getHammer(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return null;
        String id = item.getItemMeta().getPersistentDataContainer().get(hammerKey, PersistentDataType.STRING);
        return HammerType.fromId(id);
    }

    // ---------- Recipes ----------

    private void registerRecipes() {
        for (HammerType t : HammerType.values()) {
            // Netherite hammer is made in the smithing table (see HammerListener).
            if (t == HammerType.NETHERITE || !isEnabled(t)) continue;

            NamespacedKey key = new NamespacedKey(this, t.id() + "_hammer");
            ShapedRecipe r = new ShapedRecipe(key, createHammer(t));
            r.shape("XXX", "X#X", " # ");
            r.setIngredient('#', Material.STICK);
            r.setIngredient('X', ingredientFor(t));
            Bukkit.addRecipe(r);
            recipeKeys.add(key);
        }
    }

    private void unregisterRecipes() {
        for (NamespacedKey k : recipeKeys) Bukkit.removeRecipe(k);
        recipeKeys.clear();
    }

    public List<NamespacedKey> recipeKeys() { return recipeKeys; }

    private RecipeChoice ingredientFor(HammerType t) {
        return switch (t) {
            case STONE -> new RecipeChoice.MaterialChoice(Material.COBBLESTONE, Material.COBBLED_DEEPSLATE, Material.BLACKSTONE);
            case COPPER -> new RecipeChoice.MaterialChoice(Material.COPPER_INGOT);
            case IRON -> new RecipeChoice.MaterialChoice(Material.IRON_INGOT);
            case GOLDEN -> new RecipeChoice.MaterialChoice(Material.GOLD_INGOT);
            case DIAMOND -> new RecipeChoice.MaterialChoice(Material.DIAMOND);
            case NETHERITE -> new RecipeChoice.MaterialChoice(Material.NETHERITE_INGOT);
        };
    }
}
