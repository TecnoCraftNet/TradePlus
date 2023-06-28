package com.trophonix.tradeplus.commands;

import com.pirroproductions.devutils.player.Players;
import net.tecnocraft.utils.utils.CommandFramework;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Created by @zPirroZ3007 (github.com/zPirroZ3007) on 21 settembre, 2020
 */
public class TradeCommandHandler extends CommandFramework {
	public TradeCommandHandler(JavaPlugin plugin, String label) {
		super(plugin, label);
	}

	public void execute(CommandSender sender, String label, String[] args) {
		Players.error(Validator.getPlayerSender(sender), "Per effettuare uno scambio usa le azioni rapide!");
	}

	public static boolean hasPassaMontagna(Player player) {
		ItemStack helmet = player.getInventory().getHelmet();
		if (helmet == null)
			return false;
		if (helmet.getType() != Material.FEATHER)
			return false;
		if (helmet.getItemMeta() == null)
			return false;
		if (!helmet.getItemMeta().hasDisplayName())
			return false;
		return helmet.getItemMeta().getDisplayName().equals("§9Passamontagna");
	}
}
