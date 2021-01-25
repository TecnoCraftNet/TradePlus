package com.trophonix.tradeplus.config;

import com.trophonix.tradeplus.commands.TradeCommandHandler;
import com.trophonix.tradeplus.util.MsgUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class ConfigMessage {

    private String[] message;
    private String onHover;
    private String onClick;

    public ConfigMessage(ConfigurationSection yml, String key, String defaultText) {
        String text = null;
        if (yml.isString(key)) {
            text = yml.getString(key);
        } else if (yml.isConfigurationSection(key)) {
            text = yml.getString(key + ".text");
            onHover = yml.getString(key + ".hover");
        }
        if (text == null) text = defaultText;
        if (text.contains("%NEWLINE%")) {
            message = text.split("%NEWLINE%");
            for (int i = 0; i < message.length; i++) {
                message[i] = ChatColor.translateAlternateColorCodes('&', message[i]);
            }
        } else {
            message = new String[]{ChatColor.translateAlternateColorCodes('&', text)};
        }
    }

    public void send(CommandSender player, String... replacements) {
        String hover = this.onHover;
        if (hover != null) {
          hover = getString(hover, replacements, player);
        }
      for (String line : message) {
        line = getString(line, replacements, player);

        if (onHover == null && onClick == null) {
                player.sendMessage(line);
            } else {
                MsgUtils.send((Player) player, hover, onClick, line);
            }
        }
    }

  private String getString(String hover, String[] replacements, CommandSender player) {
    for (int i = 0; i < replacements.length - 1; i += 2) {
        if (replacements[i].contains("%PLAYER")) {
            Player p = Bukkit.getPlayer(replacements[i + 1]);
            if (p != null)
                hover = hover.replace(replacements[i], TradeCommandHandler.hasPassaMontagna(p) && !player.hasPermission("tecnoroleplay.admin") ? "Anonimo" : p.getName());
            else
                hover = hover.replace(replacements[i], replacements[i + 1]);
        } else hover = hover.replace(replacements[i], replacements[i + 1]);
    }
    return hover;
  }

  public ConfigMessage setOnClick(String command) {
        onClick = command;
        return this;
    }
}
