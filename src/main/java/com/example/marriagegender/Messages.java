package com.example.marriagegender;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;

public final class Messages {

    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final String prefix = "<dark_gray>[</dark_gray><gold>Marriage</gold><dark_gray>]</dark_gray> ";

    public void send(CommandSender sender, String miniMessageText) {
        sender.sendMessage(miniMessage.deserialize(prefix + miniMessageText));
    }

    public void error(CommandSender sender, String miniMessageText) {
        send(sender, "<red>" + miniMessageText + "</red>");
    }

    public void success(CommandSender sender, String miniMessageText) {
        send(sender, "<green>" + miniMessageText + "</green>");
    }

    public void errorPlain(CommandSender sender, String plainText) {
        sender.sendMessage(Component.text(plainText, NamedTextColor.RED));
    }

    public Component marriageBroadcast(String player1, String player2) {
        return Component.text()
                .append(Component.text("❤ ", NamedTextColor.RED))
                .append(Component.text(player1, NamedTextColor.GOLD))
                .append(Component.text(" и ", NamedTextColor.GREEN))
                .append(Component.text(player2, NamedTextColor.GOLD))
                .append(Component.text(" поженились!", NamedTextColor.GREEN))
                .build();
    }
}
