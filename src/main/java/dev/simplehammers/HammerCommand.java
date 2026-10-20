package dev.simplehammers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class HammerCommand implements CommandExecutor, TabCompleter {

    private final SimpleHammers plugin;

    public HammerCommand(SimpleHammers plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String label, String[] a) {
        if (a.length >= 1 && a[0].equalsIgnoreCase("reload")) {
            plugin.reload();
            s.sendMessage(Component.text("SimpleHammers reloaded.", NamedTextColor.GREEN));
            return true;
        }
        if (a.length >= 2 && a[0].equalsIgnoreCase("give")) {
            // /hammer give <type> [player] [amount]
            HammerType t = HammerType.fromId(a[1]);
            if (t == null) {
                s.sendMessage(Component.text("Unknown hammer: " + a[1], NamedTextColor.RED));
                return true;
            }
            Player target = a.length >= 3 ? Bukkit.getPlayerExact(a[2]) : (s instanceof Player p ? p : null);
            if (target == null) {
                s.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
                return true;
            }
            int amount = 1;
            if (a.length >= 4) {
                try { amount = Math.max(1, Math.min(64, Integer.parseInt(a[3]))); }
                catch (NumberFormatException ex) { amount = 1; }
            }
            for (int i = 0; i < amount; i++) {
                ItemStack item = plugin.createHammer(t);
                target.getInventory().addItem(item).values()
                        .forEach(left -> target.getWorld().dropItemNaturally(target.getLocation(), left));
            }
            s.sendMessage(Component.text("Gave " + amount + "x " + t.displayName() + " to " + target.getName(), NamedTextColor.GREEN));
            return true;
        }
        s.sendMessage(Component.text("Usage: /" + label + " give <type> [player] [amount] | reload", NamedTextColor.YELLOW));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String label, String[] a) {
        List<String> out = new ArrayList<>();
        if (a.length == 1) {
            out.addAll(List.of("give", "reload"));
        } else if (a.length == 2 && a[0].equalsIgnoreCase("give")) {
            for (HammerType t : HammerType.values()) out.add(t.id());
        } else if (a.length == 3 && a[0].equalsIgnoreCase("give")) {
            Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
        }
        String last = a[a.length - 1].toLowerCase(Locale.ROOT);
        out.removeIf(x -> !x.toLowerCase(Locale.ROOT).startsWith(last));
        return out;
    }
}
