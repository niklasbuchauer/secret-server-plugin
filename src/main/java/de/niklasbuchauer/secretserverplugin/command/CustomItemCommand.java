package de.niklasbuchauer.secretserverplugin.command;

import de.niklasbuchauer.secretserverplugin.customitem.CustomItemConstants;
import de.niklasbuchauer.secretserverplugin.customitem.CustomItemService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class CustomItemCommand implements CommandExecutor, TabCompleter {
    private static final String USAGE = "Benutzung: /customitem <spyglass|blindstick>";

    private final CustomItemService customItemService;

    public CustomItemCommand(CustomItemService customItemService) {
        this.customItemService = customItemService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Nur Spieler können diesen Command verwenden.");
            return true;
        }

        if (!player.hasPermission("customitems.give")) {
            player.sendMessage(ChatColor.RED + "Dafür hast du keine Berechtigung.");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(ChatColor.RED + USAGE);
            return true;
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);
        Optional<ItemStack> item;
        switch (subCommand) {
            case "spyglass" -> item = customItemService.createItem(CustomItemConstants.SPYGLASS_ID);
            case "blindstick" -> item = customItemService.createItem(CustomItemConstants.BLINDNESS_STICK_ID);
            default -> {
                player.sendMessage(ChatColor.RED + USAGE);
                return true;
            }
        }

        item.ifPresent(stack -> {
            var leftovers = player.getInventory().addItem(stack);
            leftovers.values().forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
        });

        player.sendMessage(ChatColor.GREEN + "Custom Item erhalten.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("customitems.give")) {
            return List.of();
        }

        if (args.length != 1) {
            return List.of();
        }

        List<String> options = List.of("spyglass", "blindstick");
        String prefix = args[0].toLowerCase(Locale.ROOT);

        List<String> matches = new ArrayList<>();
        for (String option : options) {
            if (option.startsWith(prefix)) {
                matches.add(option);
            }
        }
        return matches;
    }
}
