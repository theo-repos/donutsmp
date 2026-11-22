package net.donutsim.hud;

import net.donutsim.config.DonutSimConfig;
import net.donutsim.util.NumberFormatter;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

public final class DonutSimHudRenderer {
    public static final int PANEL_WIDTH = 138;
    public static final int PANEL_HEIGHT = 122;

    private DonutSimHudRenderer() {}

    public static void register() {
        HudRenderCallback.EVENT.register(DonutSimHudRenderer::onRenderHud);
    }

    public static int getResolvedX(int screenWidth, DonutSimConfig cfg) {
        int scaledWidth = Math.round(PANEL_WIDTH * cfg.hudScale);
        if (cfg.hudX < 0) {
            return Math.max(4, screenWidth - scaledWidth - 6);
        }
        return Math.max(0, Math.min(cfg.hudX, screenWidth - scaledWidth));
    }

    public static int getResolvedY(int screenHeight, DonutSimConfig cfg) {
        int scaledHeight = Math.round(PANEL_HEIGHT * cfg.hudScale);
        return Math.max(0, Math.min(cfg.hudY, screenHeight - scaledHeight));
    }

    private static void onRenderHud(GuiGraphics graphics, DeltaTracker deltaTracker) {
        DonutSimConfig cfg = DonutSimConfig.get();
        if (!cfg.hudEnabled) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) {
            return;
        }

        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();
        int x = getResolvedX(screenWidth, cfg);
        int y = getResolvedY(screenHeight, cfg);

        renderPanel(graphics, mc, x, y, cfg.hudScale);
    }

    public static void renderPanel(GuiGraphics graphics, Minecraft mc, int x, int y, float scale) {
        DonutSimConfig cfg = DonutSimConfig.get();
        Font font = mc.font;

        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);

        // Semi-transparent dark DonutSMP sidebar background + subtle aqua top accent
        graphics.fill(0, 0, PANEL_WIDTH, PANEL_HEIGHT, 0xA80D1117);
        graphics.fill(0, 0, PANEL_WIDTH, 2, 0xFF00D4FF);
        graphics.fill(0, PANEL_HEIGHT - 1, PANEL_WIDTH, PANEL_HEIGHT, 0x6600D4FF);

        // Header: Bold DONUTSMP title
        Component title = Component.literal("DONUTSMP").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);
        int titleW = font.width(title);
        graphics.drawString(font, title, (PANEL_WIDTH - titleW) / 2, 6, 0xFF55FFFF, true);

        // Divider line under title
        graphics.fill(8, 18, PANEL_WIDTH - 8, 19, 0x44FFFFFF);

        int pingMs = resolveClientPing(mc);
        ChatFormatting pingColor = pingMs <= 60 ? ChatFormatting.GREEN : (pingMs <= 140 ? ChatFormatting.YELLOW : ChatFormatting.GRAY);

        int rowY = 24;
        int rowStep = 12;

        drawStatRow(graphics, font, rowY, "$", ChatFormatting.GREEN, "Money", "$" + NumberFormatter.formatCompact(cfg.balance), ChatFormatting.GREEN);
        rowY += rowStep;

        drawStatRow(graphics, font, rowY, "✦", ChatFormatting.LIGHT_PURPLE, "Shards", NumberFormatter.formatWhole(cfg.shards), ChatFormatting.LIGHT_PURPLE);
        rowY += rowStep;

        drawStatRow(graphics, font, rowY, "⚔", ChatFormatting.RED, "Kills", NumberFormatter.formatWhole(cfg.kills), ChatFormatting.RED);
        rowY += rowStep;

        drawStatRow(graphics, font, rowY, "☠", ChatFormatting.DARK_RED, "Deaths", NumberFormatter.formatWhole(cfg.deaths), ChatFormatting.DARK_RED);
        rowY += rowStep;

        drawStatRow(graphics, font, rowY, "⏱", ChatFormatting.YELLOW, "Keyall", NumberFormatter.formatTimer(cfg.keyallTimerSeconds), ChatFormatting.YELLOW);
        rowY += rowStep;

        drawStatRow(graphics, font, rowY, "⚑", ChatFormatting.AQUA, "Team", cfg.teamName, ChatFormatting.AQUA);
        rowY += rowStep;

        drawStatRow(graphics, font, rowY, "⚡", pingColor, "Ping", pingMs + "ms", pingColor);

        // Footer: subtle donutsmp.net
        Component footer = Component.literal("donutsmp.net").withStyle(ChatFormatting.GRAY);
        int footerW = font.width(footer);
        graphics.drawString(font, footer, (PANEL_WIDTH - footerW) / 2, PANEL_HEIGHT - 12, 0xFF9CA3AF, false);

        graphics.pose().popMatrix();
    }

    private static void drawStatRow(
            GuiGraphics graphics,
            Font font,
            int y,
            String icon,
            ChatFormatting iconColor,
            String label,
            String value,
            ChatFormatting valueColor
    ) {
        Component left = Component.empty()
                .append(Component.literal(icon + " ").withStyle(iconColor))
                .append(Component.literal(label + ": ").withStyle(ChatFormatting.WHITE));
        Component right = Component.literal(value).withStyle(valueColor);

        graphics.drawString(font, left, 7, y, 0xFFFFFFFF, true);
        int rightW = font.width(right);
        graphics.drawString(font, right, PANEL_WIDTH - rightW - 7, y, 0xFFFFFFFF, true);
    }

    private static int resolveClientPing(Minecraft mc) {
        if (mc.getConnection() != null && mc.player != null) {
            PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
            if (info != null) {
                return Math.max(1, info.getLatency());
            }
        }
        return 24;
    }
}
