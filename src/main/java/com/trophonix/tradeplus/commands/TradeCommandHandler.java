package com.trophonix.tradeplus.commands;

import com.Ben12345rocks.VotingPlugin.Main;
import com.pirroproductions.devutils.player.Players;
import com.trophonix.tradeplus.TradePlus;
import com.trophonix.tradeplus.events.TradeAcceptEvent;
import com.trophonix.tradeplus.events.TradeRequestEvent;
import com.trophonix.tradeplus.hooks.FactionsHook;
import com.trophonix.tradeplus.hooks.WorldGuardHook;
import com.trophonix.tradeplus.trade.Trade;
import com.trophonix.tradeplus.trade.TradeRequest;
import com.trophonix.tradeplus.util.MsgUtils;
import com.trophonix.tradeplus.util.PlayerUtil;
import net.tecnocraft.utils.utils.CommandFramework;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.InetSocketAddress;
import java.text.DecimalFormat;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;

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
}
