package com.ullarah.urocket.task;

import com.ullarah.urocket.RocketInit;
import com.ullarah.urocket.data.RocketPlayer;
import com.ullarah.urocket.function.GamemodeCheck;
import com.ullarah.urocket.init.RocketVariant;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;

import java.util.Map;
import java.util.UUID;

public class ActiveEffects {

    public void task() {

        Plugin plugin = Bukkit.getPluginManager().getPlugin(RocketInit.pluginName);

        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin,
                () -> plugin.getServer().getScheduler().runTask(plugin, () -> {

                    for (Player player : RocketInit.getPlayers()) {
                        processEffectCheck(player);
                    }

                }), 0, 0);

    }

    private void processEffectCheck(Player player) {
        GamemodeCheck gamemodeCheck = new GamemodeCheck();
        if (!gamemodeCheck.check(player, GameMode.SURVIVAL, GameMode.ADVENTURE))
            return;

        RocketPlayer rp = RocketInit.getPlayer(player);
        if (rp.getBootData() == null)
            return;

        if (player.isFlying()) {

            RocketVariant.Variant variant = rp.getBootData().getVariant();
            PotionEffect[] effects = variant.getPotionEffects();

            for (PotionEffect effect : player.getActivePotionEffects())
                player.removePotionEffect(effect.getType());

            if (effects != null) {
                for (PotionEffect effect : effects) {
                    rp.setEffected(true);
                    player.addPotionEffect(effect);
                }
            }

            if (variant.equals(RocketVariant.Variant.STEALTH))
                for (Player onlinePlayer : Bukkit.getOnlinePlayers())
                    onlinePlayer.hidePlayer(player);

        }
        // Not flying, remove effects if required
        else if (rp.isEffected()) {

            for (PotionEffect effect : player.getActivePotionEffects())
                player.removePotionEffect(effect.getType());

            for (Player onlinePlayer : Bukkit.getOnlinePlayers())
                onlinePlayer.showPlayer(player);

            rp.setEffected(false);

        }
    }

}
