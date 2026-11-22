package net.donutsim.gui;

import net.donutsim.command.DonutSimCommands;
import net.donutsim.config.DonutSimConfig;
import net.donutsim.hud.DonutSimHudRenderer;
import net.donutsim.util.NumberFormatter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class DonutSimConfigScreen extends Screen {
    private EditBox balanceBox;
    private EditBox shardsBox;
    private EditBox killsBox;
    private EditBox deathsBox;
    private EditBox teamBox;

    private EditBox payPlayerBox;
    private EditBox payAmountBox;

    private boolean draggingHud = false;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

    public DonutSimConfigScreen() {
        super(Component.literal("DonutSim Config & HUD Editor"));
    }

    @Override
    protected void init() {
        DonutSimConfig cfg = DonutSimConfig.get();
        int centerX = this.width / 2;
        int startY = 36;

        // Toggle HUD button
        this.addRenderableWidget(Button.builder(
                getToggleHudLabel(cfg.hudEnabled),
                btn -> {
                    cfg.hudEnabled = !cfg.hudEnabled;
                    btn.setMessage(getToggleHudLabel(cfg.hudEnabled));
                    DonutSimConfig.save();
                }
        ).bounds(centerX - 110, startY, 108, 20).build());

        // Reset HUD Position button
        this.addRenderableWidget(Button.builder(
                Component.literal("Reset HUD Pos"),
                btn -> {
                    cfg.hudX = -1;
                    cfg.hudY = 60;
                    cfg.hudScale = 1.0f;
                    DonutSimConfig.save();
                }
        ).bounds(centerX + 2, startY, 108, 20).build());

        // Scale - / + buttons
        this.addRenderableWidget(Button.builder(
                Component.literal("Scale -"),
                btn -> {
                    cfg.hudScale = Math.max(0.6f, Math.round((cfg.hudScale - 0.1f) * 10f) / 10f);
                    DonutSimConfig.save();
                }
        ).bounds(centerX - 110, startY + 24, 108, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Scale +"),
                btn -> {
                    cfg.hudScale = Math.min(2.0f, Math.round((cfg.hudScale + 0.1f) * 10f) / 10f);
                    DonutSimConfig.save();
                }
        ).bounds(centerX + 2, startY + 24, 108, 20).build());

        int fieldX = centerX - 10;
        int rowY = startY + 54;
        int step = 22;

        balanceBox = createBox(fieldX, rowY, 120, NumberFormatter.formatCompact(cfg.balance));
        rowY += step;

        shardsBox = createBox(fieldX, rowY, 120, String.valueOf(cfg.shards));
        rowY += step;

        killsBox = createBox(fieldX, rowY, 120, String.valueOf(cfg.kills));
        rowY += step;

        deathsBox = createBox(fieldX, rowY, 120, String.valueOf(cfg.deaths));
        rowY += step;

        teamBox = createBox(fieldX, rowY, 120, cfg.teamName);
        rowY += step + 4;

        // Apply Stats Button
        this.addRenderableWidget(Button.builder(
                Component.literal("Save Stats & Economy").withStyle(ChatFormatting.GREEN),
                btn -> applyStatsFromFields()
        ).bounds(centerX - 110, rowY, 220, 20).build());

        rowY += 34;

        // One-click Incoming Payment Simulator
        payPlayerBox = createBox(centerX - 110, rowY, 82, "DrDonut");
        payPlayerBox.setHint(Component.literal("Player"));

        payAmountBox = createBox(centerX - 24, rowY, 64, "50M");
        payAmountBox.setHint(Component.literal("Amount"));

        this.addRenderableWidget(Button.builder(
                Component.literal("Pay Me!").withStyle(ChatFormatting.AQUA),
                btn -> {
                    String sender = payPlayerBox.getValue().trim();
                    String amt = payAmountBox.getValue().trim();
                    if (!sender.isEmpty() && !amt.isEmpty()) {
                        DonutSimCommands.executePayMe(sender, amt);
                        balanceBox.setValue(NumberFormatter.formatCompact(DonutSimConfig.get().balance));
                    }
                }
        ).bounds(centerX + 44, rowY, 66, 20).build());
    }

    private EditBox createBox(int x, int y, int width, String initialValue) {
        EditBox box = new EditBox(this.font, x, y, width, 18, Component.empty());
        box.setMaxLength(32);
        box.setValue(initialValue);
        this.addRenderableWidget(box);
        return box;
    }

    private Component getToggleHudLabel(boolean enabled) {
        return Component.literal("HUD: ")
                .append(Component.literal(enabled ? "ON" : "OFF")
                        .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED));
    }

    private void applyStatsFromFields() {
        DonutSimConfig cfg = DonutSimConfig.get();
        try {
            cfg.balance = Math.max(0.0, NumberFormatter.parseAmount(balanceBox.getValue()));
        } catch (NumberFormatException ignored) {}

        try {
            cfg.shards = Math.max(0L, Math.round(NumberFormatter.parseAmount(shardsBox.getValue())));
        } catch (NumberFormatException ignored) {}

        try {
            cfg.kills = Math.max(0, Integer.parseInt(killsBox.getValue().trim()));
        } catch (NumberFormatException ignored) {}

        try {
            cfg.deaths = Math.max(0, Integer.parseInt(deathsBox.getValue().trim()));
        } catch (NumberFormatException ignored) {}

        String team = teamBox.getValue().trim();
        if (!team.isEmpty()) {
            cfg.teamName = team;
        }
        DonutSimConfig.save();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0x88000000);

        int centerX = this.width / 2;
        graphics.fill(centerX - 122, 16, centerX + 122, 266, 0xCC111827);
        graphics.fill(centerX - 122, 16, centerX + 122, 18, 0xFF00D4FF);

        graphics.drawCenteredString(this.font,
                Component.literal("DONUTSMP CLIENT CONFIG").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
                centerX, 22, 0xFFFFFFFF);

        int labelX = centerX - 108;
        int rowY = 94;
        int step = 22;

        graphics.drawString(this.font, "Balance ($):", labelX, rowY, 0xFF55FF55, false);
        rowY += step;
        graphics.drawString(this.font, "Shards:", labelX, rowY, 0xFFFF55FF, false);
        rowY += step;
        graphics.drawString(this.font, "Kills:", labelX, rowY, 0xFFFF5555, false);
        rowY += step;
        graphics.drawString(this.font, "Deaths:", labelX, rowY, 0xFFAA0000, false);
        rowY += step;
        graphics.drawString(this.font, "Team Tag:", labelX, rowY, 0xFF55FFFF, false);
        rowY += step + 28;

        graphics.drawString(this.font,
                Component.literal("Simulate Incoming Payment:").withStyle(ChatFormatting.YELLOW),
                labelX, rowY, 0xFFFFFFFF, false);

        graphics.drawCenteredString(this.font,
                Component.literal("Tip: Drag the DonutSMP HUD box directly to reposition it").withStyle(ChatFormatting.GRAY),
                centerX, 253, 0xFF9CA3AF);

        super.render(graphics, mouseX, mouseY, partialTick);

        // Render live draggable preview of the HUD box if enabled
        DonutSimConfig cfg = DonutSimConfig.get();
        if (cfg.hudEnabled && this.minecraft != null) {
            int hudX = DonutSimHudRenderer.getResolvedX(this.width, cfg);
            int hudY = DonutSimHudRenderer.getResolvedY(this.height, cfg);
            DonutSimHudRenderer.renderPanel(graphics, this.minecraft, hudX, hudY, cfg.hudScale);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        DonutSimConfig cfg = DonutSimConfig.get();
        if (cfg.hudEnabled && event.button() == 0) {
            int hudX = DonutSimHudRenderer.getResolvedX(this.width, cfg);
            int hudY = DonutSimHudRenderer.getResolvedY(this.height, cfg);
            int scaledW = Math.round(DonutSimHudRenderer.PANEL_WIDTH * cfg.hudScale);
            int scaledH = Math.round(DonutSimHudRenderer.PANEL_HEIGHT * cfg.hudScale);

            double mx = event.x();
            double my = event.y();
            if (mx >= hudX && mx <= hudX + scaledW && my >= hudY && my <= hudY + scaledH) {
                draggingHud = true;
                dragOffsetX = (int) mx - hudX;
                dragOffsetY = (int) my - hudY;
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (draggingHud && event.button() == 0) {
            DonutSimConfig cfg = DonutSimConfig.get();
            int scaledW = Math.round(DonutSimHudRenderer.PANEL_WIDTH * cfg.hudScale);
            int scaledH = Math.round(DonutSimHudRenderer.PANEL_HEIGHT * cfg.hudScale);

            cfg.hudX = Math.max(0, Math.min((int) event.x() - dragOffsetX, this.width - scaledW));
            cfg.hudY = Math.max(0, Math.min((int) event.y() - dragOffsetY, this.height - scaledH));
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (draggingHud && event.button() == 0) {
            draggingHud = false;
            DonutSimConfig.save();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public void removed() {
        applyStatsFromFields();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
