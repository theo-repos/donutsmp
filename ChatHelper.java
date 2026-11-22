package net.donutsim.util;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;

public final class ChatHelper {
    private ChatHelper() {}

    public static void sendLocalMessage(Component component) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(component, false);
        }
    }

    public static void sendActionBar(Component component) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(component, true);
        }
    }

    public static void sendError(String message) {
        sendLocalMessage(Component.literal(message).withStyle(ChatFormatting.RED));
    }

    public static void playOrbSound() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        }
    }

    public static void playPickupSound() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.playSound(SoundEvents.ITEM_PICKUP, 0.8f, 1.15f);
        }
    }

    public static Component prefix() {
        return Component.empty()
                .append(Component.literal("DONUTSMP").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
                .append(Component.literal(" » ").withStyle(ChatFormatting.DARK_GRAY));
    }

    public static MutableComponent greenMoney(double amount) {
        return Component.literal("$" + NumberFormatter.formatCompact(amount)).withStyle(ChatFormatting.GREEN);
    }
}
