package com.trophonix.tradeplus.trade;

import com.pirroproductions.devutils.inventory.ItemBuilder;
import com.tecnoroleplay.api.events.QuickActionEvent;
import com.tecnoroleplay.api.hooks.ItemsAdder;
import com.trophonix.tradeplus.TradePlus;
import com.trophonix.tradeplus.events.TradeAcceptEvent;
import com.trophonix.tradeplus.events.TradeRequestEvent;
import com.trophonix.tradeplus.hooks.FactionsHook;
import com.trophonix.tradeplus.hooks.WorldGuardHook;
import com.trophonix.tradeplus.util.PlayerUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.net.InetSocketAddress;
import java.text.DecimalFormat;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TradeListener implements Listener {
    private static final TradePlus pl = TradePlus.getInstance();
    public static final ConcurrentLinkedQueue<TradeRequest> requests = new ConcurrentLinkedQueue<>();
    private static final DecimalFormat format = new DecimalFormat("0.##");

    @EventHandler
    public void onTrade(QuickActionEvent event) {
        var player = event.getPlayer();

        if (!event.hasEntity()) return;
        if (!(event.getEntity() instanceof Player receiver)) return;

        var icon = ItemBuilder.of(ItemsAdder.getCustomItem("icon_invisible_tick"))
                .name(Component.text("ᴇꜰꜰᴇᴛᴛᴜᴀ ᴜɴᴏ ꜱᴄᴀᴍʙɪᴏ").color(TextColor.fromHexString("#d79729")).decoration(TextDecoration.ITALIC, false))
                .desc("§7ᴄʟɪᴄᴄᴀ ᴘᴇʀ ꜰᴀʀᴇ ᴜɴᴏ ꜱᴄᴀᴍʙɪᴏ", "§7ᴄᴏɴ ꞯᴜᴇꜱᴛᴏ ᴄɪᴛᴛᴀᴅɪɴᴏ.");

        event.add(icon, "寧", () -> {
            try {
                if (pl.getTradeConfig().isWorldguardTradingFlag()) {
                    if (Bukkit.getServer().getPluginManager().isPluginEnabled("WorldGuard")) {
                        if (!WorldGuardHook.isTradingAllowed(player, player.getLocation())) {
                            pl.getTradeConfig().getWorldguardTradingNotAllowed().send(player);
                            return;
                        }
                    }
                }
            } catch (Throwable ignored) {

            }

            try {
                if (!pl.getTradeConfig().isFactionsAllowTradeInEnemyTerritory()) {
                    if (FactionsHook.isPlayerInEnemyTerritory(player)) {
                        pl.getTradeConfig().getFactionsEnemyTerritory().send(player);
                        return;
                    }
                }
            } catch (Throwable ignored) {
            }

            boolean permissionRequired = pl.getConfig().getBoolean("permissions.required", false);

            if (PlayerUtil.isVanished(receiver)) {
                pl.getTradeConfig().getErrorsPlayerNotFound().send(player);
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
                } else if (receiver.getGameMode().equals(GameMode.CREATIVE)) {
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
            } else {
                if (pl.getTradeConfig().isAllowCrossWorld()) {
                    double amount = Math.pow(pl.getTradeConfig().getCrossWorldRange(), 2);
                    Location test = receiver.getLocation().clone();
                    test.setWorld(player.getWorld());
                    if (amount != 0.0 && player.getLocation().distanceSquared(test) > amount) {
                        pl.getTradeConfig().getErrorsCrossWorldRange().send(player, "%PLAYER%", receiver.getName(), "%AMOUNT%", format.format(amount));
                        return;
                    }
                } else {
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
                pl.getTradeConfig().getAcceptReceiver().send(player,
                        "%PLAYER%", hasPassaMontagna(receiver) ? "Anonimo" : receiver.getName());
                new Trade(receiver, player);
                requests.removeIf(req -> req.contains(player) && req.contains(receiver));
            } else {
                String sendPermission = pl.getTradeConfig().getSendPermission();
                if (permissionRequired) {
                    if (!player.hasPermission(sendPermission)) {
                        pl.getTradeConfig().getErrorsNoPermsAccept().send(player);
                        return;
                    }
                }

                String acceptPermission = pl.getTradeConfig().getAcceptPermission();
                if (permissionRequired && !receiver.hasPermission(acceptPermission)) {
                    pl.getTradeConfig().getErrorsNoPermsReceive().send(player, "%PLAYER%", receiver.getName());
                    return;
                }

                TradeRequestEvent tre = new TradeRequestEvent(player, receiver);
                Bukkit.getPluginManager().callEvent(tre);
                if (tre.isCancelled())
                    return;
                final TradeRequest request = new TradeRequest(player, receiver);
                requests.add(request);
                pl.getTradeConfig().getRequestSent().send(player, "%PLAYER%",  hasPassaMontagna(receiver) ? "Anonimo" : receiver.getName());
                pl.getTradeConfig().getRequestReceived().setOnClick("/trade " + player.getName()).send(receiver, "%PLAYER%", hasPassaMontagna(player) ? "Anonimo" : player.getName());

                Bukkit.getScheduler().runTaskLater(pl, () -> {
                    boolean was = requests.remove(request);
                    if (player.isOnline() && was) {
                        pl.getTradeConfig().getExpired().send(player, "%PLAYER%", receiver.getName());
                    }
                }, 20L * pl.getTradeConfig().getRequestCooldownSeconds());
            }
        });
    }


    @EventHandler
    public void onInteract(PlayerInteractAtEntityEvent event){
        var player = event.getPlayer();

        if (!(event.getRightClicked() instanceof Player receiver)) return;
        if (!event.getHand().equals(EquipmentSlot.HAND)) return;
        if (player.getInventory().getItemInMainHand().getType() != Material.AIR) return;


        for (TradeRequest req : requests)
            if (req.sender == player)
                return;

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
            pl.getTradeConfig().getAcceptSender().send(receiver, "%PLAYER%", hasPassaMontagna(player) ? "Anonimo" : player.getName());
            pl.getTradeConfig().getAcceptReceiver().send(player, "%PLAYER%", hasPassaMontagna(receiver) ? "Anonimo" : receiver.getName());
            new Trade(receiver, player);
            requests.removeIf(req -> req.contains(player) && req.contains(receiver));
        }
    }

    public static boolean hasPassaMontagna(Player player) {
        ItemStack helmet = player.getInventory().getHelmet();

        if (helmet == null)
            return false;

        return ItemsAdder.isCustomItem("passamontagna", helmet);
    }
}
