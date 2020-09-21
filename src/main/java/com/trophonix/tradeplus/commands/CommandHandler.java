package com.trophonix.tradeplus.commands;

import com.trophonix.tradeplus.TradePlus;
import com.trophonix.tradeplus.trade.Trade;
import com.trophonix.tradeplus.util.Perms;
import net.tecnocraft.utils.utils.SubCommandFramework;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class CommandHandler extends SubCommandFramework {

	public CommandHandler(JavaPlugin plugin, String label, String... aliases) {
		super(plugin, label, aliases);
	}

	private static final TradePlus pl = TradePlus.getInstance();

	@Override
	public void noArgs(CommandSender sender) {
		this.sendHelp(sender);
	}

	@SubCommand("reload")
	@SubCommandMinArgs(0)
	@SubCommandPermission(Perms.ADMIN)
	@SubCommandDescription("Ricarica il plugin")
	public void reload(CommandSender sender, String label, String[] args) {
		pl.reload();
		pl.getTradeConfig().getAdminConfigReloaded().send(sender);
	}

	@SubCommand("force")
	@SubCommandMinArgs(2)
	@SubCommandPermission(Perms.ADMIN)
	@SubCommandDescription("Forza un trade fra 2 giocatori")
	@SubCommandUsage("<player1> <player>")
	public void force(CommandSender sender, String label, String[] args) {
		Player p1 = Bukkit.getPlayer(args[0]);
		Player p2 = Bukkit.getPlayer(args[1]);
		if (p1 == null || p2 == null || !p1.isOnline() || !p2.isOnline() || p1.equals(p2)) {
			pl.getTradeConfig().getAdminInvalidPlayers().send(sender);
			return;
		}
		pl.getTradeConfig().getAdminForcedTrade().send(sender, "%PLAYER1%", p1.getName(), "%PLAYER2%", p2.getName());
		pl.getTradeConfig().getForcedTrade().send(p1, "%PLAYER%", p2.getName());
		pl.getTradeConfig().getForcedTrade().send(p2, "%PLAYER%", p1.getName());
		Trade trade = new Trade(p1, p2);
		if (sender instanceof Player && !(sender.equals(p1) || sender.equals(p2)))
			((Player) sender).openInventory(trade.getSpectatorInv());
	}

	@SubCommand("spectate")
	@SubCommandMinArgs(1)
	@SubCommandPermission(Perms.ADMIN)
	@SubCommandDescription("Specta trade in corso.")
	@SubCommandUsage("<player(s)>>")
	public void spectate(CommandSender sender, String label, String[] args) {
		Player staff = Validator.getPlayerSender(sender);
		Player player = Bukkit.getPlayer(args[0]);
		if (player == null || !player.isOnline()) {
			pl.getTradeConfig().getAdminInvalidPlayers().send(sender);
			return;
		}
		Trade trade = pl.getTrade(player);
		if (trade == null) {
			pl.getTradeConfig().getAdminNoTrade().send(staff);
		}
		else {
			staff.openInventory(trade.getSpectatorInv());
		}
	}
}