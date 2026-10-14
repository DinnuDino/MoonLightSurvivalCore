/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.minimessage.MiniMessage
 *  net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
 */
package com.moonlight.vanilla.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class MessageUtil {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final Pattern LEGACY_PATTERN = Pattern.compile("(?i)[&\u00a7]([0-9a-fk-or])");

    private MessageUtil() {
    }

    public static Component format(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        String normalized = MessageUtil.convertLegacyToMiniMessage(text);
        try {
            return MINI_MESSAGE.deserialize(normalized);
        }
        catch (Exception e) {
            return Component.text(text);
        }
    }

    public static String convertLegacyToMiniMessage(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        if (!input.contains("&") && !input.contains("\u00a7")) {
            return input;
        }
        Matcher matcher = LEGACY_PATTERN.matcher(input);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            char code = Character.toLowerCase(matcher.group(1).charAt(0));
            String tag = switch (code) {
                case '0' -> "<black>";
                case '1' -> "<dark_blue>";
                case '2' -> "<dark_green>";
                case '3' -> "<dark_aqua>";
                case '4' -> "<dark_red>";
                case '5' -> "<dark_purple>";
                case '6' -> "<gold>";
                case '7' -> "<gray>";
                case '8' -> "<dark_gray>";
                case '9' -> "<blue>";
                case 'a' -> "<green>";
                case 'b' -> "<aqua>";
                case 'c' -> "<red>";
                case 'd' -> "<light_purple>";
                case 'e' -> "<yellow>";
                case 'f' -> "<white>";
                case 'k' -> "<obfuscated>";
                case 'l' -> "<bold>";
                case 'm' -> "<strikethrough>";
                case 'n' -> "<underlined>";
                case 'o' -> "<italic>";
                case 'r' -> "<reset>";
                default -> "";
            };
            matcher.appendReplacement(sb, Matcher.quoteReplacement(tag));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public static String toLegacy(Component component) {
        if (component == null) {
            return "";
        }
        try {
            return LegacyComponentSerializer.legacySection().serialize(component);
        }
        catch (Exception e) {
            return "";
        }
    }

    public static String toLegacy(String miniMessageText) {
        if (miniMessageText == null || miniMessageText.isEmpty()) {
            return "";
        }
        try {
            return MessageUtil.toLegacy(MessageUtil.format(miniMessageText));
        }
        catch (Exception e) {
            return miniMessageText;
        }
    }
}

