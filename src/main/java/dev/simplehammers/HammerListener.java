package dev.simplehammers;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.SmithingInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.RayTraceResult;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class HammerListener implements Listener {

    private final SimpleHammers plugin;
    /** Players whose extra blocks are being broken right now (prevents recursion). */
    private final Set<UUID> busy = new HashSet<>();

    public HammerListener(SimpleHammers plugin) {
        this.plugin = plugin;
    }

    // ---------- 3x3x1 mining ----------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if (busy.contains(p.getUniqueId())) return;
        if (p.getGameMode() != GameMode.SURVIVAL) return;
        if (!p.hasPermission("simplehammers.use")) return;
        if (plugin.getConfig().getBoolean("sneak-disables", false) && p.isSneaking()) return;

        ItemStack tool = p.getInventory().getItemInMainHand();
        if (plugin.getHammer(tool) == null) return;

        Block center = e.getBlock();
        if (!canHammer(center, tool)) return;

        BlockFace face = faceOf(p, center);

        busy.add(p.getUniqueId());
        try {
            for (int[] o : offsets(face)) {
                Block b = center.getRelative(o[0], o[1], o[2]);
                if (!canHammer(b, tool)) continue;

                BlockBreakEvent ev = new BlockBreakEvent(b, p);
                Bukkit.getPluginManager().callEvent(ev); // lets protection plugins veto it
                if (ev.isCancelled()) continue;

                b.breakNaturally(tool, true, true); // honours Silk Touch / Fortune of the hammer
                if (!damageTool(p, tool)) break;     // hammer broke
            }
        } finally {
            busy.remove(p.getUniqueId());
        }
    }

    private boolean canHammer(Block b, ItemStack tool) {
        Material t = b.getType();
        if (t.isAir() || b.isLiquid()) return false;
        if (plugin.blacklist().contains(t)) return false;
        if (t.getHardness() < 0) return false;          // bedrock, barriers...
        if (!Tag.MINEABLE_PICKAXE.isTagged(t)) return false;
        return b.isPreferredTool(tool);                  // only blocks this tier can really mine
    }

    /** Which face of the block the player is mining. */
    private BlockFace faceOf(Player p, Block center) {
        RayTraceResult r = p.rayTraceBlocks(7.0);
        if (r != null && center.equals(r.getHitBlock()) && r.getHitBlockFace() != null) {
            return r.getHitBlockFace();
        }
        float pitch = p.getLocation().getPitch();
        if (pitch > 45) return BlockFace.UP;
        if (pitch < -45) return BlockFace.DOWN;
        return p.getFacing().getOppositeFace();
    }

    private int[][] offsets(BlockFace face) {
        return switch (face) {
            case UP, DOWN -> new int[][]{
                    {-1, 0, -1}, {0, 0, -1}, {1, 0, -1}, {-1, 0, 0}, {1, 0, 0}, {-1, 0, 1}, {0, 0, 1}, {1, 0, 1}};
            case EAST, WEST -> new int[][]{
                    {0, -1, -1}, {0, -1, 0}, {0, -1, 1}, {0, 0, -1}, {0, 0, 1}, {0, 1, -1}, {0, 1, 0}, {0, 1, 1}};
            default -> new int[][]{ // NORTH / SOUTH
                    {-1, -1, 0}, {0, -1, 0}, {1, -1, 0}, {-1, 0, 0}, {1, 0, 0}, {-1, 1, 0}, {0, 1, 0}, {1, 1, 0}};
        };
    }

    /** Applies 1 durability damage (respecting Unbreaking). Returns false if the tool broke. */
    private boolean damageTool(Player p, ItemStack tool) {
        int unbreaking = tool.getEnchantmentLevel(Enchantment.UNBREAKING);
        if (unbreaking > 0 && ThreadLocalRandom.current().nextInt(unbreaking + 1) != 0) return true;

        ItemMeta meta = tool.getItemMeta();
        if (!(meta instanceof Damageable d)) return true;
        int max = d.hasMaxDamage() ? d.getMaxDamage() : tool.getType().getMaxDurability();
        d.setDamage(d.getDamage() + 1);
        tool.setItemMeta(d);

        if (d.getDamage() >= max) {
            p.getInventory().setItemInMainHand(null);
            p.playSound(p.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);
            return false;
        }
        return true;
    }

    // ---------- Netherite hammer = diamond hammer + ingot + template ----------

    @EventHandler
    public void onSmithing(PrepareSmithingEvent e) {
        SmithingInventory inv = e.getInventory();
        ItemStack base = inv.getInputEquipment();
        HammerType type = plugin.getHammer(base);
        if (type == null || e.getResult() == null) return;

        boolean ok = type == HammerType.DIAMOND
                && plugin.isEnabled(HammerType.NETHERITE)
                && inv.getInputTemplate() != null
                && inv.getInputTemplate().getType() == Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE
                && inv.getInputMineral() != null
                && inv.getInputMineral().getType() == Material.NETHERITE_INGOT;

        if (!ok) { // stops hammers turning into plain pickaxes
            e.setResult(null);
            return;
        }

        ItemStack result = plugin.createHammer(HammerType.NETHERITE);
        result.addUnsafeEnchantments(base.getEnchantments());
        if (base.getItemMeta() instanceof Damageable from && result.getItemMeta() instanceof Damageable to) {
            to.setDamage(from.getDamage());
            result.setItemMeta(to);
        }
        e.setResult(result);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        e.getPlayer().discoverRecipes(plugin.recipeKeys());
    }
}
