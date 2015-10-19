package com.ullarah.uchest;

import com.ullarah.ulib.function.Experience;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.ullarah.uchest.ChestFunctions.validStorage.VAULT;
import static com.ullarah.uchest.ChestInit.*;

public class ChestFunctions {

    public static void convertItem(Player player, String type, ItemStack[] items) {

        DecimalFormat decimalFormat = new DecimalFormat(".##");

        double amount = 0.0;
        boolean hasItems = false;

        for (ItemStack item : items) {

            if (item != null) {

                String itemMaterial = item.getType().name();

                int itemAmount = item.getAmount();

                double itemValue = getPlugin().getConfig().getConfigurationSection("materials").getDouble(itemMaterial);
                double maxDurability = item.getType().getMaxDurability();
                double durability = item.getDurability();

                if (type.equals("MONEY")) itemValue /= 5;

                if (item.getType().getMaxDurability() > 0)
                    itemValue = itemValue - (itemValue * (durability / maxDurability));

                amount += itemValue * itemAmount;

                hasItems = true;

            }

        }

        if (hasItems) {

            if (type.equals("MONEY")) {

                getVaultEconomy().depositPlayer(player, amount);

                player.sendMessage(amount > 0 ? getMsgPrefix() + "You gained " + ChatColor.GREEN +
                        "$" + decimalFormat.format(amount) : getMsgPrefix() + "You gained no money.");

            }

            if (type.equals("XP")) {

                Experience.addExperience(player, amount);

                player.sendMessage(amount > 0 ? getMsgPrefix() + "You gained " + ChatColor.GREEN +
                        decimalFormat.format(amount) + " XP" : getMsgPrefix() + "You gained no XP.");

            }

        }

    }

    public static void openConvertChest(CommandSender sender, String type) {

        final Player player = (Player) sender;
        int playerLevel = player.getLevel();

        if (playerLevel < chestAccessLevel)
            player.sendMessage(getMsgPrefix() + "You need more than " +
                    chestAccessLevel + " levels to open this chest.");
        else if (chestLockout.contains(player.getUniqueId())) {

            player.sendMessage(getMsgPrefix() + "You are currently locked out from this chest.");
            player.sendMessage(getMsgPrefix() + "Don't panic. Just try again later!");

            Bukkit.getServer().getScheduler().runTaskLaterAsynchronously(getPlugin(), () -> {

                if (chestLockout.contains(player.getUniqueId())) chestLockout.remove(player.getUniqueId());

            }, 6000);

        } else {

            Inventory chestExpInventory = Bukkit.createInventory(
                    player, 27, ChatColor.DARK_GREEN + (type.equals("MONEY") ? "Money" : "Experience") + " Chest");
            player.openInventory(chestExpInventory);

            chestLockoutEntry(player);

        }

    }

    public static void openPerkChest(CommandSender sender) {

        final Player player = (Player) sender;
        int playerLevel = player.getLevel();

        if (playerLevel < chestAccessLevel)
            player.sendMessage(getMsgPrefix() + "You need more than " +
                    chestAccessLevel + " levels to open this chest.");
        else if (chestPerkLockout.contains(player.getUniqueId())) {

            player.sendMessage(getMsgPrefix() + "You have already used this chest today!");

        } else {

            Inventory chestPerkInventory = Bukkit.createInventory(player, 54, ChatColor.DARK_GREEN + "Perk Chest");
            ArrayList<ItemStack> perkStack = new ArrayList<>();

            for (int i = 0; i < 54; i++)
                perkStack.add(perkItemStacks.get(new Random().nextInt(perkItemStacks.size())));

            chestPerkInventory.setContents(perkStack.toArray(new ItemStack[perkStack.size()]));
            player.openInventory(chestPerkInventory);

            chestPerkLockoutEntry(player);

            Bukkit.getServer().getScheduler().runTaskLaterAsynchronously(getPlugin(), () -> {

                if (player.getOpenInventory().getTopInventory().getTitle()
                        .equals(ChatColor.DARK_GREEN + "Perk Chest")) player.closeInventory();

            }, 10000);

        }

    }

    public static void openRandomChest(CommandSender sender) {

        final Player player = (Player) sender;
        int playerLevel = player.getLevel();

        if (playerLevel < chestAccessLevel)
            player.sendMessage(getMsgPrefix() + "You need more than " +
                    chestAccessLevel + " levels to open this chest.");
        else if (chestLockout.contains(player.getUniqueId())) {

            player.sendMessage(getMsgPrefix() + "You are currently locked out from this chest.");
            player.sendMessage(getMsgPrefix() + "Don't panic. Just try again later!");

            Bukkit.getServer().getScheduler().runTaskLaterAsynchronously(getPlugin(), () -> chestLockout.remove(player.getUniqueId()), 6000);

        } else {

            player.openInventory(getChestRandomHolder().getInventory());
            chestLockoutEntry(player);

        }

    }

    public static void chestView(Player player, Player viewer, Inventory inventory, validStorage type) {

        if (viewer == null) {

            if (player.getLevel() <= holdingAccessLevel) {

                player.sendMessage(getMsgPrefix() + "You need more than " +
                        holdingAccessLevel + " levels to open this chest.");

            } else {

                if (type == VAULT) player.setLevel(player.getLevel() - holdingAccessLevel);
                player.openInventory(inventory);

            }

        } else player.openInventory(inventory);

    }

    public static ItemStack createItemStack(Material material, String name, List<String> lore) {

        ItemStack chestIcon = new ItemStack(material);
        ItemMeta chestIconMeta = chestIcon.getItemMeta();

        chestIconMeta.setDisplayName(name);
        chestIconMeta.setLore(lore);

        chestIcon.setItemMeta(chestIconMeta);

        return chestIcon;

    }

    public static void openChestDelay(final Player player, final String type) {

        Bukkit.getServer().getScheduler().runTaskLater(getPlugin(), () -> Bukkit.dispatchCommand(player, type), 1);

    }

    public static void chestLockoutEntry(Player player) {

        if (chestLockoutCount.containsKey(player.getUniqueId())) {

            Integer count = chestLockoutCount.get(player.getUniqueId());

            if (count < 2) chestLockoutCount.put(player.getUniqueId(), count + 1);
            else chestLockout.add(player.getUniqueId());

        } else chestLockoutCount.put(player.getUniqueId(), 1);

    }

    public static void chestPerkLockoutEntry(Player player) {

        if (!chestPerkLockoutCount.containsKey(player.getUniqueId()))
            chestPerkLockoutCount.put(player.getUniqueId(), 1);

        switch (chestPerkLockoutCount.get(player.getUniqueId())) {

            case 1:
                if (player.hasPermission("chest.perk2"))
                    chestPerkLockoutCount.put(player.getUniqueId(), 2);
                else chestPerkLockout.add(player.getUniqueId());
                break;

            case 2:
                chestPerkLockout.add(player.getUniqueId());

        }

    }

    public enum validCommands {
        HELP, MAINTENANCE, TOGGLE, RANDOM, RESET, VIEW, UPGRADE, REMOTE
    }

    public enum validStorage {
        HOLD("hold"), VAULT("vault"), REMOTE("remote");

        private final String type;

        validStorage(String getType) {
            type = getType;
        }

        public String toString() {
            return type;
        }
    }

}
