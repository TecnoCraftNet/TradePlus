package com.trophonix.tradeplus.extras;

import com.google.common.base.Preconditions;
import com.trophonix.tradeplus.TradePlus;
import com.trophonix.tradeplus.trade.Trade;
import com.trophonix.tradeplus.util.AnvilGUI;
import com.trophonix.tradeplus.util.ItemFactory;
import com.trophonix.tradeplus.util.Sounds;
import lombok.AccessLevel;
import lombok.Setter;
import me.badbones69.crazycrates.api.objects.ItemBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.conversations.NumericPrompt;
import org.bukkit.conversations.Prompt;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;

public abstract class Extra {

  static final DecimalFormat decimalFormat = new DecimalFormat("###,##0.##");

  private TradePlus pl;
  public final ItemStack icon;
  public final String name;
  final Player player1;
  final Player player2;
  final double increment;
  final ItemStack theirIcon;
  final double taxPercent;
  @Setter(AccessLevel.PRIVATE) public double value1 = 0, value2 = 0;
  private double max1;
  private double max2;
  private long lastUpdatedMax = System.currentTimeMillis();
  double increment1;
  double increment2;
  private String mode;
  private Trade trade;

  Extra(String name, Player player1, Player player2, TradePlus pl, Trade trade) {
    this.pl = pl;
    this.name = name;
    ConfigurationSection section = Preconditions.checkNotNull(pl.getConfig().getConfigurationSection("extras." + name));
    this.player1 = player1;
    this.player2 = player2;
    this.increment = section.getDouble("increment", 1D);
    this.increment1 = increment;
    this.increment2 = increment;
    ItemFactory factory = new ItemFactory(section.getString("material", "PAPER"), Material.PAPER)
            .display('&', section.getString("display", "&4ERROR"));
    if (section.contains("lore"))
      factory.lore('&', section.getStringList("lore"));
    this.icon = factory.flag(ItemFlag.HIDE_ATTRIBUTES).build();
    this.theirIcon = new ItemFactory(section.getString("material", "PAPER"), Material.PAPER)
            .display('&', section.getString("theirdisplay", "&4ERROR")).build();
    this.taxPercent = section.getDouble("taxpercent", 0);
    this.mode = section.getString("mode", "increment").toLowerCase();
    if (mode.equals("type")) {
      mode = "anvil";
      section.set("mode", "anvil");
      pl.saveConfig();
    }
    this.trade = trade;
  }

  public void init() {
    this.max1 = getMax(player1);
    this.max2 = getMax(player2);
    this.pl.log("'" + name + "' extra initialized. Balances: [" + max1 + ", " + max2 + "]");
  }

  private AnvilGUI gui;

  public void onClick(Player player, ClickType click) {
    double offer = player1.equals(player) ? value1 : value2;
    if (mode.equals("anvil")) {
      ItemStack paper = new ItemStack(Material.PAPER);
      gui = new AnvilGUI(player, event -> {
        if (event.getSlot() != AnvilGUI.AnvilSlot.OUTPUT) return;
        String text = event.getText();
        ItemStack i = event.getItemStack();
        if (i == null || i.getType() == Material.AIR) {
          i = paper;
        }
        ItemMeta m = i.getItemMeta(); assert m != null;
        if (text == null || text.isEmpty()) {
          m.setDisplayName(pl.getTypeEmpty());
          return;
        }
        parse: try {
          double o = Double.parseDouble(text);
          double bal = getMax(player);
          if (o > bal) {
            m.setDisplayName(pl.getTypeMaximum().replace("%BALANCE%", decimalFormat.format(bal)));
            break parse;
          }
          event.setWillClose(true);
          if (player1.equals(player)) setValue1(o);
          else setValue2(o);
        } catch (NumberFormatException ignored) {
          m.setDisplayName(pl.getTypeInvalid());
        }
        i.setItemMeta(m);
        gui.setSlot(AnvilGUI.AnvilSlot.OUTPUT, i);
      }, () -> {
        trade.updateExtras();
        trade.open(player);
        trade.setCancelOnClose(player, true);
      });
      gui.setSlot(AnvilGUI.AnvilSlot.INPUT_LEFT, paper);
      gui.setSlotName(AnvilGUI.AnvilSlot.INPUT_LEFT, decimalFormat.format(offer));
      gui.setTitle(ChatColor.stripColor(pl.getTypeEmpty()));
      gui.open();
    } else if (mode.equals("chat")) {
      trade.setCancelOnClose(player, false);
      player.closeInventory();
      new ConversationFactory(pl).withFirstPrompt(new NumericPrompt() {
        @Override protected Prompt acceptValidatedInput(ConversationContext conversationContext, Number number) {
          if (trade.cancelled) return null;
          if (number.doubleValue() >= getMax(player)) {
            return new NumericPrompt() {
              @Override protected Prompt acceptValidatedInput(ConversationContext conversationContext, Number number) {
                if (trade.cancelled) return null;
                if (number.doubleValue() > getMax(player)) {
                  return this;
                }
                if (player1.equals(player)) {
                  value1 = number.doubleValue();
                } else if (player2.equals(player)) {
                  value2 = number.doubleValue();
                }
                return null;
              }
              @Override public String getPromptText(ConversationContext conversationContext) {
                return pl.getTypeMaximum().replace("%BALANCE%", decimalFormat.format(getMax(player)));
              }
            };
          }
          if (player1.equals(player)) {
            value1 = number.doubleValue();
          } else if (player2.equals(player)) {
            value2 = number.doubleValue();
          }
          return null;
        }

        @Override public String getPromptText(ConversationContext conversationContext) {
          return pl.getTypeEmpty().replace("%BALANCE%", decimalFormat.format(getMax(player)))
              .replace("%AMOUNT%", decimalFormat.format(offer));
        }
      }).withTimeout(30).addConversationAbandonedListener(event -> {
        if (trade.cancelled) return;
        if (!event.gracefulExit()) Sounds.villagerHmm(player, 1f);
        trade.open(player);
        trade.updateExtras();
        trade.setCancelOnClose(player, true);
      }).buildConversation(player)
          .begin();
    } else {
      if (click.isLeftClick()) {
        if (click.isShiftClick()) {
          if (player.equals(player1)) {
            increment1 -= increment;
          } else if (player.equals(player2)) {
            increment2 -= increment;
          }
        } else {
          if (player.equals(player1)) {
            value1 -= increment1;
          } else if (player.equals(player2)) {
            value2 -= increment2;
          }
        }
      } else if (click.isRightClick()) {
        if (click.isShiftClick()) {
          if (player.equals(player1)) {
            increment1 += increment;
          } else if (player.equals(player2)) {
            increment2 += increment;
          }
        } else {
          if (player.equals(player1)) {
            value1 += increment1;
          } else if (player.equals(player2)) {
            value2 += increment2;
          }
        }
      }

    }
    if (increment1 < 0) increment1 = 0;
    if (increment2 < 0) increment2 = 0;

    if (value1 < 0) value1 = 0;
    if (value2 < 0) value2 = 0;

    long now = System.currentTimeMillis();
    if (now > lastUpdatedMax + 5000) {
      max1 = getMax(player1);
      max2 = getMax(player2);
      lastUpdatedMax = now;
    }
    if (value1 > max1) value1 = max1;
    if (value2 > max2) value2 = max2;
  }

  protected abstract double getMax(Player player);

  public abstract void onTradeEnd();

  public abstract ItemStack getIcon(Player player);

  public abstract ItemStack getTheirIcon(Player player);

}
