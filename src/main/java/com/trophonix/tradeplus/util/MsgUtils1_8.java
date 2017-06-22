package com.trophonix.tradeplus.util;

import net.md_5.bungee.api.chat.*;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class MsgUtils1_8 {

    public static void send(Player player, String onHover, String onClick, String[] messages) {
        for (String m : messages) {
            BaseComponent[] comps = TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', m));
            for (BaseComponent comp : comps) {
                comp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', onHover))));
                comp.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, onClick));
            }
            player.spigot().sendMessage(comps);
        }
    }

}