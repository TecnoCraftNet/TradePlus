package com.trophonix.tradeplus.util;

import com.pirroproductions.devutils.inventory.ItemBuilder;
import com.pirroproductions.devutils.threads.Tasks;
import com.tecnoroleplay.api.game.Roleplayer;
import com.trophonix.tradeplus.TradePlus;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

public class InvUtils {
    //  public static final List<Integer> leftSlots =
    //          new LinkedList<>(
    //                  Arrays.asList(0,
    //                          1, 2, 3, 9, 10, 11, 12, 18, 19, 20, 21, 27, 28, 29, 30, 36, 37, 38,
    // 39, 45, 46, 47,
    //                          48));

    private static TradePlus pl;

    public static void reloadItems(TradePlus pl) {
        InvUtils.pl = pl;
    }

    public static Inventory getTradeInventory(Player player1, Player player2) {

        Roleplayer rpP2 = Roleplayer.of(player2);

        Inventory inv = Bukkit.createInventory(player1.getInventory().getHolder(), 54, pl.getTradeConfig().getGuiTitle().replace("%PLAYER%", rpP2.getFullName()));
        ItemStack separator = pl.getTradeConfig().getSeparator().copy().replace("%PLAYER%", rpP2.getFullName()).build();
        for (int i = 4; i <= 49; i += 9)
            inv.setItem(i, separator);
        if (pl.getTradeConfig().isAcceptEnabled()) {
            if (pl.getTradeConfig().isForceEnabled() && player1.hasPermission("tradeplus.admin")) {
                inv.setItem(49, pl.getTradeConfig().getForce().build());
            }
        } else {
            inv.setItem(pl.getTradeConfig().getAcceptSlot(), separator);
            inv.setItem(pl.getTradeConfig().getTheirAcceptSlot(), separator);
        }
        if (pl.getTradeConfig().isHeadEnabled()) {
            ItemStack head = ItemBuilder.of(Material.PLAYER_HEAD, 1).name(pl.getTradeConfig().getHeadDisplayName().replace("%PLAYER%", rpP2.getFullName()));
            head.editMeta(meta -> meta.setCustomModelData(3));
            inv.setItem(4, head);
            Tasks.async(() -> {
                head.editMeta(meta -> ((SkullMeta) meta).setOwner(rpP2.getFullName()));
                inv.setItem(4, head);
            });
        }
        return inv;
    }

    public static Inventory getSpectatorInventory(Player player1, Player player2) {
        String title = ChatColor.translateAlternateColorCodes('&', pl.getTradeConfig().getSpectatorTitle());
        if (Sounds.version > 1.8)
            title = title.replace("%PLAYER1%", player1.getName()).replace("%PLAYER2%", player2.getName());
        Inventory inv = Bukkit.createInventory(player1.getInventory().getHolder(), 54, title);
        ItemStack separator = pl.getTradeConfig().getSeparator().build();
        for (int i = 4; i <= 49; i += 9)
            inv.setItem(i, separator);
        for (int i = 45; i <= 53; i++)
            inv.setItem(i, separator);
        inv.setItem(pl.getTradeConfig().getAcceptSlot(), ItemFactory.getPlayerSkull(player1, "&f" + player1.getName()));
        inv.setItem(pl.getTradeConfig().getTheirAcceptSlot(), ItemFactory.getPlayerSkull(player2, "&f" + player2.getName()));
        inv.setItem(4, pl.getTradeConfig().getTheirCancel().build());

        return inv;
    }
}
