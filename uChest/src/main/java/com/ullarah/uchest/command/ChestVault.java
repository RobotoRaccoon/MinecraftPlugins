package com.ullarah.uchest.command;

import com.ullarah.uchest.ChestFunctions;
import com.ullarah.uchest.function.CommonString;
import com.ullarah.uchest.function.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;

import static com.ullarah.uchest.ChestFunctions.validStorage.VAULT;
import static com.ullarah.uchest.ChestInit.*;

public class ChestVault {

    public void runCommand(CommandSender sender, String[] args) {

        if (args.length == 0) if (!(sender instanceof Player)) new CommonString().messageNoConsole(getPlugin(), sender);
        else if (!getMaintenanceCheck()) {

            if (chestTypeEnabled.get("vchest")) ChestCreation.create(sender, VAULT, true);
            else
                new CommonString().messageSend(getPlugin(), sender, true, new String[]{"Vault Chest is currently unavailable."});

        } else new CommonString().messageMaintenance(getPlugin(), sender);

        else try {

            switch (ChestFunctions.validCommands.valueOf(args[0].toUpperCase())) {

                case VIEW:
                    if (!(sender instanceof Player)) new CommonString().messageNoConsole(getPlugin(), sender);
                    else if (sender.hasPermission("chest.view")) {
                        if (args.length == 2) {
                            try {
                                ChestPrepare.prepare(Bukkit.getPlayer(new PlayerProfile().lookup(args[1]).getId()),
                                        (Player) sender, VAULT);
                            } catch (Exception e) {
                                new CommonString().messageSend(getPlugin(), sender, true, new String[]{
                                        ChatColor.RED + "Cannot view vault chest at this time. Try again later!"
                                });
                            }
                        } else
                            new CommonString().messageSend(getPlugin(), sender, true, new String[]{ChatColor.YELLOW + "/vchest view <player>"});
                    } else new CommonString().messagePermDeny(getPlugin(), sender);
                    break;

                case UPGRADE:
                    if (!(sender instanceof Player)) new CommonString().messageNoConsole(getPlugin(), sender);
                    else {
                        Player player = (Player) sender;
                        if (player.getLevel() >= 50) {

                            File chestFile = new File(getPlugin().getDataFolder() + File.separator + "vault",
                                    player.getUniqueId().toString() + ".yml");

                            if (chestFile.exists()) {

                                FileConfiguration chestConfig = YamlConfiguration.loadConfiguration(chestFile);

                                int chestPlayerSlot = chestConfig.getInt("slots");

                                if (chestPlayerSlot >= 54) {

                                    new CommonString().messageSend(getPlugin(), player, true, new String[]{
                                            ChatColor.AQUA + "You have the maximum number of slots!"
                                    });

                                } else {

                                    if (player.getLevel() >= 50) {

                                        chestConfig.set("slot", chestPlayerSlot + 9);
                                        player.setLevel(player.getLevel() - 50);

                                        try {
                                            chestConfig.save(chestFile);
                                        } catch (IOException e) {
                                            new CommonString().messageSend(getPlugin(), player, true, new String[]{
                                                    ChatColor.RED + "Slot Update Error!"
                                            });
                                        }

                                        new CommonString().messageSend(getPlugin(), player, true, new String[]{
                                                ChatColor.YELLOW + "You upgraded your vault! You now have " +
                                                        ChatColor.GREEN + (chestPlayerSlot + 9) + ChatColor.YELLOW + " slots!"
                                        });

                                    } else {

                                        new CommonString().messageSend(getPlugin(), player, true, new String[]{
                                                ChatColor.YELLOW + "You need at least" +
                                                        ChatColor.GOLD + " 50xp levels " + ChatColor.YELLOW + "to upgrade!"
                                        });

                                    }

                                }

                            }

                        } else new CommonString().messageSend(getPlugin(), player, true, new String[]{
                                ChatColor.YELLOW + "You need at least" + ChatColor.GOLD +
                                        " 50xp levels " + ChatColor.YELLOW + "to upgrade!"
                        });
                    }
                    break;

                default:
                    if (!(sender instanceof Player)) new CommonString().messageNoConsole(getPlugin(), sender);
                    else ChestCreation.create(sender, VAULT, true);

            }

        } catch (IllegalArgumentException e) {

            if (!(sender instanceof Player)) new CommonString().messageNoConsole(getPlugin(), sender);
            else if (!args[0].equalsIgnoreCase("view")) DisplayHelp.runHelp(sender);

        }

    }

}