package com.ullarah.urocket;

import com.ullarah.ulib.function.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.ullarah.urocket.RocketFunctions.Variant.*;
import static com.ullarah.urocket.RocketInit.*;

public class RocketFunctions {

    public static void disableRocketBoots(Player player, Boolean keepUsage, Boolean keepPower, Boolean keepFlight,
                                          Boolean keepVariant, Boolean keepEnhancement) {

        UUID playerUUID = player.getUniqueId();

        if (!keepUsage && rocketUsage.contains(playerUUID)) rocketUsage.remove(playerUUID);
        if (rocketSprint.containsKey(playerUUID)) rocketSprint.remove(playerUUID);
        if (rocketLowFuel.contains(playerUUID)) rocketLowFuel.remove(playerUUID);
        if (!keepPower && rocketPower.containsKey(playerUUID)) rocketPower.remove(playerUUID);

        if (!rocketFire.isEmpty()) rocketFire.clear();
        if (rocketWater.contains(playerUUID)) rocketWater.remove(playerUUID);
        if (rocketRepair.containsKey(playerUUID)) rocketRepair.remove(playerUUID);

        if (!keepEnhancement && rocketHealer.containsKey(playerUUID)) rocketHealer.remove(playerUUID);
        if (!keepEnhancement && rocketEfficient.containsKey(playerUUID)) rocketEfficient.remove(playerUUID);
        if (!keepEnhancement && rocketSolar.containsKey(playerUUID)) rocketSolar.remove(playerUUID);

        if (!keepVariant && rocketVariant.containsKey(playerUUID)) {
            switch (rocketVariant.get(playerUUID)) {
                case ENDER:
                    player.removePotionEffect(PotionEffectType.NIGHT_VISION);
                    player.removePotionEffect(PotionEffectType.HEALTH_BOOST);
                    break;
                case ZERO:
                    player.removePotionEffect(PotionEffectType.CONFUSION);
                    player.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
                    player.removePotionEffect(PotionEffectType.INCREASE_DAMAGE);
                    break;
                case STEALTH:
                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) onlinePlayer.showPlayer(player);
                    break;
                case DRUNK:
                    player.removePotionEffect(PotionEffectType.CONFUSION);
                    player.removePotionEffect(PotionEffectType.FAST_DIGGING);
                    break;
                case BOOST:
                    player.removePotionEffect(PotionEffectType.HEAL);
                    break;
                case RUNNER:
                    player.removePotionEffect(PotionEffectType.SPEED);
                    break;
            }
            rocketVariant.remove(playerUUID);
        }

        if (player.isOnline())
            if (GamemodeCheck.check(player, GameMode.SURVIVAL, GameMode.ADVENTURE)) {

                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) onlinePlayer.showPlayer(player);

                player.setFlySpeed(0.1f);
                player.setFlying(false);
                if (!keepFlight) player.setAllowFlight(false);

                player.setNoDamageTicks(60);
                player.setFallDistance(0);

                player.sendMessage(getMsgPrefix() + "Rocket Boots Deactivated!");

            }

    }

    public static void interactRocketBoots(InventoryClickEvent event, ItemStack boots) {

        Player player = (Player) event.getWhoClicked();
        ClickType click = event.getClick();
        Boolean hasRocketMeta = boots.hasItemMeta();

        if (GamemodeCheck.check(player, GameMode.CREATIVE, GameMode.SPECTATOR)) {
            event.setCancelled(true);
            player.closeInventory();
            disableRocketBoots(player, false, false, false, false, false);
            player.sendMessage(getMsgPrefix() + "Rocket Boots do not work in this gamemode!");
            return;
        }

        if (hasRocketMeta) {

            ItemMeta rocketMeta = boots.getItemMeta();

            if (rocketMeta.hasDisplayName())
                if (rocketMeta.getDisplayName().matches(ChatColor.RED + "Rocket Boots")) if (rocketMeta.hasLore()) {

                    Boolean validBoots = true;
                    String rocketLore = rocketMeta.getLore().get(0);
                    String variantLore = null;

                    if (rocketMeta.getLore().size() >= 2)
                        variantLore = ChatColor.stripColor(rocketMeta.getLore().get(1));

                    Set<String> specialVariants = new HashSet<>(Collections.singletonList("Robin Hood"));

                    if (variantLore != null) if (specialVariants.contains(variantLore)) {

                        Variant variantType = getEnum(variantLore);

                        if (variantType != null) {
                            switch (variantType) {

                                case MONEY:
                                    if (getVaultEconomy() == null) {
                                        validBoots = false;
                                        player.sendMessage(getMsgPrefix() + "These Rocket Boots cannot be equipped!");
                                    }
                                    break;

                                default:
                                    validBoots = true;
                                    break;

                            }
                        }
                    }

                    if (validBoots && rocketLore.matches(ChatColor.YELLOW + "Rocket Level I{0,3}V?X?"))
                        if (!rocketUsage.contains(player.getUniqueId()))
                            if (click == ClickType.MIDDLE) event.setCancelled(true);
                            else attachRocketBoots(player, rocketMeta, rocketLore);

                }

        } else if (rocketSprint.containsKey(player.getUniqueId())) {

            player.sendMessage(new String[]{
                    getMsgPrefix() + ChatColor.RED + "Ouch! You cannot take your boots of yet!",
                    getMsgPrefix() + ChatColor.RESET + "You need to land for them to cool down!"
            });

            event.setCancelled(true);
            player.closeInventory();

        } else if (rocketPower.containsKey(player.getUniqueId()))
            disableRocketBoots(player, false, false, false, false, false);

    }

    public static void attachRocketBoots(Player player, ItemMeta rocketMeta, String rocketLore) {

        if (GamemodeCheck.check(player, GameMode.CREATIVE, GameMode.SPECTATOR)) {
            disableRocketBoots(player, false, false, false, false, false);
            player.sendMessage(getMsgPrefix() + "Rocket Boots do not work in this gamemode!");
            return;
        }
        
        Block blockMiddle = player.getLocation().getBlock().getRelative(BlockFace.SELF);
        String variantLore = null;
        String enhancementLore = null;
        Boolean isWaterVariant = false;
        Boolean isRunnerVariant = false;

        if (rocketMeta.getLore().size() == 2) {

            String loreLine = ChatColor.stripColor(rocketMeta.getLore().get(1));
            assert loreLine != null;

            switch (loreLine) {

                case "Self Repair":
                    enhancementLore = loreLine;
                    rocketHealer.put(player.getUniqueId(), 0);
                    break;

                case "Fuel Efficient":
                    enhancementLore = loreLine;
                    rocketEfficient.put(player.getUniqueId(), true);
                    break;

                case "Solar Powered":
                    enhancementLore = loreLine;
                    rocketSolar.put(player.getUniqueId(), true);
                    break;

                default:
                    variantLore = loreLine;
                    break;

            }

        }

        if (rocketMeta.getLore().size() == 3) {

            variantLore = ChatColor.stripColor(rocketMeta.getLore().get(1));
            enhancementLore = ChatColor.stripColor(rocketMeta.getLore().get(2));

            Variant variantType = getEnum(variantLore);

            if (!rocketEnhancementBlacklist.contains(variantType)) {

                assert enhancementLore != null;
                switch (enhancementLore) {

                    case "Self Repair":
                        rocketHealer.put(player.getUniqueId(), 0);
                        break;

                    case "Fuel Efficient":
                        rocketEfficient.put(player.getUniqueId(), true);
                        break;

                    case "Solar Powered":
                        rocketSolar.put(player.getUniqueId(), true);
                        break;

                }
                
            }

        }

        if (variantLore != null) {

            Variant variantType = getEnum(variantLore);

            if (variantType != null) {
                rocketVariant.put(player.getUniqueId(), variantType);
                if (rocketVariant.get(player.getUniqueId()) == WATER) isWaterVariant = true;
            }

        } else rocketVariant.put(player.getUniqueId(), ORIGINAL);

        if (!isWaterVariant && blockMiddle.isLiquid()) {

            rocketWater.add(player.getUniqueId());
            player.sendMessage(getMsgPrefix() + "These Rocket Boots will not start in water!");

        } else if (player.getInventory().getBoots() == null)
            if (GamemodeCheck.check(player, GameMode.SURVIVAL, GameMode.ADVENTURE)) {

                player.sendMessage(getMsgPrefix() + "Rocket Boots Activated!");

                if (variantLore != null) {
                    player.sendMessage(getMsgPrefix() + "Variant: " + ChatColor.AQUA + variantLore);
                    if (Variant.getEnum(variantLore) == Variant.RUNNER) isRunnerVariant = true;
                } else player.sendMessage(getMsgPrefix() + "Variant: " + ChatColor.RED + "Not Found");

                if (enhancementLore != null)
                    player.sendMessage(getMsgPrefix() + "Enhancement: " + ChatColor.AQUA + enhancementLore);
                else
                    player.sendMessage(getMsgPrefix() + "Enhancement: " + ChatColor.RED + "Not Found");

                if (!isRunnerVariant) player.setAllowFlight(true);

                Integer powerLevel = RomanNumeralToInteger.decode(
                        rocketLore.replaceFirst(
                                ChatColor.YELLOW + "Rocket Level ", ""));

                rocketPower.put(player.getUniqueId(), powerLevel);

            }

    }

    public static int getBootPowerLevel(ItemStack rocketBoots) {

        return RomanNumeralToInteger.decode(
                rocketBoots.getItemMeta().getLore().get(0)
                        .replaceFirst(ChatColor.YELLOW + "Rocket Level ", ""));

    }

    public static void changeBootDurability(Player player, ItemStack rocketBoots) {

        Short rocketDurability = rocketBoots.getDurability();
        Material rocketMaterial = rocketBoots.getType();

        int bootMaterialDurability = 0;
        short changedDurability = 0;

        switch (rocketMaterial) {

            case LEATHER_BOOTS:
                bootMaterialDurability = 65;
                break;

            case IRON_BOOTS:
                bootMaterialDurability = 195;
                break;

            case GOLD_BOOTS:
                bootMaterialDurability = 91;
                break;

            case DIAMOND_BOOTS:
                bootMaterialDurability = 429;
                break;

        }

        switch (getBootPowerLevel(rocketBoots)) {

            case 1:
                changedDurability = (short) (rocketDurability + 7);
                break;

            case 2:
                changedDurability = (short) (rocketDurability + 6);
                break;

            case 3:
                changedDurability = (short) (rocketDurability + 5);
                break;

            case 4:
                changedDurability = (short) (rocketDurability + 4);
                break;

            case 5:
                changedDurability = (short) (rocketDurability + 3);
                break;

            case 10:
                changedDurability = (short) (rocketDurability + new Random().nextInt(10));
                break;

        }

        rocketBoots.setDurability(changedDurability);

        int newBootDurability = bootMaterialDurability - changedDurability;

        if (newBootDurability < 0) {
            rocketBoots.setDurability((short) bootMaterialDurability);
            newBootDurability = 0;
        }

        String totalDurability = ChatColor.YELLOW + "Rocket Boot Durability: "
                + newBootDurability + " / " + bootMaterialDurability;

        player.sendMessage(getMsgPrefix() + totalDurability);
        TitleSubtitle.subtitle(player, 2, ChatColor.YELLOW + totalDurability);

    }

    public static void reloadFlyZones(boolean showMessage) {

        rocketZoneLocations.clear();
        List<String> zoneList = getPlugin().getConfig().getStringList("zones");

        if (zoneList.size() > 0) {

            for (String zone : zoneList) {

                String[] zoneSection = zone.split("\\|");

                final Location zoneLocationStart = new Location(Bukkit.getWorld(zoneSection[1]),
                        Integer.parseInt(zoneSection[2]) - 25,
                        Integer.parseInt(zoneSection[3]) - 5,
                        Integer.parseInt(zoneSection[4]) - 25);

                final Location zoneLocationEnd = new Location(Bukkit.getWorld(zoneSection[1]),
                        Integer.parseInt(zoneSection[2]) + 25,
                        Integer.parseInt(zoneSection[3]) + 50,
                        Integer.parseInt(zoneSection[4]) + 25);

                ConcurrentHashMap<Location, Location> zoneLocation = new ConcurrentHashMap<Location, Location>() {{
                    put(zoneLocationStart, zoneLocationEnd);
                }};

                rocketZoneLocations.put(UUID.fromString(zoneSection[0]), zoneLocation);

            }

            if (showMessage) registerMap.put("zone", zoneList.size());

        }

    }

    public static void zoneCrystalCreation(Player player, Location blockLocation) {

        World world = player.getWorld();

        Location centerBlock = CenterBlock.variable(player, blockLocation, 0.475);

        centerBlock.getBlock().setType(Material.AIR);
        world.spawn(centerBlock, EnderCrystal.class);

        int cBX = centerBlock.getBlockX();
        int cBY = centerBlock.getBlockY();
        int cBZ = centerBlock.getBlockZ();

        List<String> zoneList = getPlugin().getConfig().getStringList("zones");
        String activeZone = player.getUniqueId().toString() + "|" + world.getName() + "|" + cBX + "|" + cBY + "|" + cBZ;
        zoneList.add(activeZone);

        getPlugin().getConfig().set("zones", zoneList);
        getPlugin().saveConfig();

        reloadFlyZones(false);

        Location particleLocation = new Location(world, cBX + 0.5, cBY + 1.2, cBZ + 0.5);

        world.playSound(centerBlock, Sound.WITHER_IDLE, 1.25f, 0.55f);
        Particles.show(Particles.ParticleType.PORTAL, particleLocation, new Float[]{0.0f, 0.0f, 0.0f}, 2, 2500);

        player.sendMessage(getMsgPrefix() + ChatColor.YELLOW + "Rocket Fly Zone Controller is now activated!");

    }

    public static boolean rocketSaddleCheck(ItemStack saddle) {

        if (saddle != null && saddle.hasItemMeta()) {

            ItemMeta saddleMeta = saddle.getItemMeta();

            if (saddleMeta.hasDisplayName()) {

                String saddleName = saddleMeta.getDisplayName();

                if (saddleName.equals(ChatColor.RED + "Rocket Saddle")) {

                    return true;

                }

            }

        }

        return false;

    }

    public enum Variant {

        ORIGINAL("Original"), ENDER("Essence of Ender"), HEALTH("Health Zapper"), KABOOM("TNT Overload"),
        RAINBOW("Radical Rainbows"), WATER("Water Slider"), ZERO("Patient Zero"), NOTE("Musical Madness"),
        STEALTH("Super Stealth"), AGENDA("Gay Agenda"), MONEY("Robin Hood"), DRUNK("Glazed Over"),
        BOOST("Pole Vaulter"), COAL("Coal Miner"), REDSTONE("Red Fury"), RUNNER("Rocket Runner"),
        GLOW("Shooting Star"), SOUND("Loud Silence");

        private final String type;

        Variant(String getType) {
            type = getType;
        }

        public static Variant getEnum(String variant) {
            for (Variant v : values()) if (variant.equals(v.type)) return v;
            return null;
        }

        public String toString() {
            return type;
        }

    }

}
