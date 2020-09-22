package com.trophonix.tradeplus.commands;

import com.Ben12345rocks.VotingPlugin.Main;
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
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.InetSocketAddress;
import java.text.DecimalFormat;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;

/**
 * Created by @zPirroZ3007 (github.com/zPirroZ3007) on 21 settembre, 2020
 */
public class TradeCommandHandler extends CommandFramework {

	private static TradePlus pl = TradePlus.getInstance();
	private final ConcurrentLinkedQueue<TradeRequest> requests = new ConcurrentLinkedQueue<>();
	private static final DecimalFormat format = new DecimalFormat("0.##");

	public TradeCommandHandler(JavaPlugin plugin, String label) {
		super(plugin, label);
	}

	public void execute(CommandSender sender, String label, String[] args) {
		long start = System.currentTimeMillis();
		execute2(sender, label, args);
		long end = System.currentTimeMillis() - start;

		if (end > 50) {
			StringBuilder sb = new StringBuilder();
			sb.append("/").append(label).append(" ");
			for(String x : args)
				sb.append(x).append(" ");
			Main.getInstance().getLogger().log(Level.INFO, "§4[Avviso] §cIl comando " + sb.toString() + " ha impiegato " + end + "ms!");
			for (Player player : Bukkit.getOnlinePlayers())
				if (player.hasPermission("tecnoroleplay.admin"))
					player.sendMessage("§4[Avviso] §cIl comando " + sb.toString() + " ha impiegato " + end + "ms!");
		}
	}

	public void execute2(CommandSender sender, String label, String[] args) {

		Validator.notCondition(args.length == 1, "Parametri errati! Utilizza: §7/trade <nome>");
		final Player player = Validator.getPlayerSender(sender);

		try {
			if (pl.getTradeConfig().isWorldguardTradingFlag()) {
				if (Bukkit.getServer().getPluginManager().isPluginEnabled("WorldGuard")) {
					if (!WorldGuardHook.isTradingAllowed(player, player.getLocation())) {
						pl.getTradeConfig().getWorldguardTradingNotAllowed().send(player);
						return;
					}
				}
			}
		}
		catch (Throwable ignored) {

		}

		try {
			if (!pl.getTradeConfig().isFactionsAllowTradeInEnemyTerritory()) {
				if (FactionsHook.isPlayerInEnemyTerritory(player)) {
					pl.getTradeConfig().getFactionsEnemyTerritory().send(player);
					return;
				}
			}
		}
		catch (Throwable ignored) {
		}

		boolean permissionRequired = pl.getConfig().getBoolean("permissions.required", false);

		if (args.length == 1) {
			final Player receiver = Bukkit.getPlayer(args[0]);
			if (receiver == null || PlayerUtil.isVanished(receiver)) {
				if (args[0].equalsIgnoreCase("deny")) {
					requests.forEach(req -> {
						if (req.receiver == player) {
							requests.remove(req);
							if (req.sender.isOnline()) {
								pl.getTradeConfig().getTheyDenied().send(req.sender, "%PLAYER%", player.getName());
							}
						}
					});
					pl.getTradeConfig().getYouDenied().send(player);
					return;
				}
				pl.getTradeConfig().getErrorsPlayerNotFound().send(player);
				return;
			}

			if (player == receiver) {
				pl.getTradeConfig().getErrorsSelfTrade().send(player);
				return;
			}

			if (!pl.getTradeConfig().isAllowSameIpTrade()) {
				InetSocketAddress address = player.getAddress();
				InetSocketAddress receiverAddress = receiver.getAddress();
				if (address != null && receiverAddress != null && address.getHostName().equals(receiverAddress.getHostName())) {
					pl.getTradeConfig().getErrorsSameIp().send(player);
					return;
				}
			}

			if (!pl.getTradeConfig().isAllowTradeInCreative()) {
				if (player.getGameMode().equals(GameMode.CREATIVE)) {
					pl.getTradeConfig().getErrorsCreative().send(player);
					return;
				}
				else if (receiver.getGameMode().equals(GameMode.CREATIVE)) {
					pl.getTradeConfig().getErrorsCreativeThem().send(player, "%PLAYER%", receiver.getName());
					return;
				}
			}

			if (player.getWorld().equals(receiver.getWorld())) {
				double amount = pl.getTradeConfig().getSameWorldRange();
				if (amount != 0.0 && player.getLocation().distanceSquared(receiver.getLocation()) > Math.pow(amount, 2)) {
					pl.getTradeConfig().getErrorsSameWorldRange().send(player, "%PLAYER%", receiver.getName(), "%AMOUNT%", format.format(amount));
					return;
				}
			}
			else {
				if (pl.getTradeConfig().isAllowCrossWorld()) {
					double amount = Math.pow(pl.getTradeConfig().getCrossWorldRange(), 2);
					Location test = receiver.getLocation().clone();
					test.setWorld(player.getWorld());
					if (amount != 0.0 && player.getLocation().distanceSquared(test) > amount) {
						pl.getTradeConfig().getErrorsCrossWorldRange().send(player, "%PLAYER%", receiver.getName(), "%AMOUNT%", format.format(amount));
						return;
					}
				}
				else {
					pl.getTradeConfig().getErrorsNoCrossWorld().send(player, "%PLAYER%", receiver.getName());
					return;
				}
			}

			for (TradeRequest req : requests) {
				if (req.sender == player) {
					pl.getTradeConfig().getErrorsWaitForExpire().send(player, "%PLAYER%", receiver.getName());
					return;
				}
			}

			boolean accept = false;
			for (TradeRequest req : requests) {
				if (req.contains(player) && req.contains(receiver))
					accept = true;
			}
			if (accept) {
				TradeAcceptEvent tradeAcceptEvent = new TradeAcceptEvent(receiver, player);
				Bukkit.getPluginManager().callEvent(tradeAcceptEvent);
				if (tradeAcceptEvent.isCancelled())
					return;
				pl.getTradeConfig().getAcceptSender().send(receiver, "%PLAYER%", player.getName());
				pl.getTradeConfig().getAcceptReceiver().send(player, "%PLAYER%", receiver.getName());
				new Trade(receiver, player);
				requests.removeIf(req -> req.contains(player) && req.contains(receiver));
			}
			else {
				String sendPermission = pl.getTradeConfig().getSendPermission();
				if (permissionRequired) {
					if (!sender.hasPermission(sendPermission)) {
						pl.getTradeConfig().getErrorsNoPermsAccept().send(player);
						return;
					}
				}

				String acceptPermission = pl.getTradeConfig().getAcceptPermission();
				if (permissionRequired && !receiver.hasPermission(acceptPermission)) {
					pl.getTradeConfig().getErrorsNoPermsReceive().send(player, "%PLAYER%", receiver.getName());
					return;
				}

				TradeRequestEvent event = new TradeRequestEvent(player, receiver);
				Bukkit.getPluginManager().callEvent(event);
				if (event.isCancelled())
					return;
				final TradeRequest request = new TradeRequest(player, receiver);
				requests.add(request);
				pl.getTradeConfig().getRequestSent().send(player, "%PLAYER%", receiver.getName());
				pl.getTradeConfig().getRequestReceived().setOnClick("/trade " + player.getName()).send(receiver, "%PLAYER%", player.getName());
				Bukkit.getScheduler().runTaskLater(pl, () -> {
					boolean was = requests.remove(request);
					if (player.isOnline() && was) {
						pl.getTradeConfig().getExpired().send(player, "%PLAYER%", receiver.getName());
					}
				}, 20 * pl.getTradeConfig().getRequestCooldownSeconds());
			}
			return;
		}
		pl.getTradeConfig().getErrorsInvalidUsage().send(player);
	}
}
