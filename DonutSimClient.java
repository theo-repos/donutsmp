package net.donutsim;

import com.mojang.blaze3d.platform.InputConstants;
import net.donutsim.command.DonutSimCommands;
import net.donutsim.config.DonutSimConfig;
import net.donutsim.entity.FakeDropManager;
import net.donutsim.gui.DonutSimConfigScreen;
import net.donutsim.hud.DonutSimHudRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DonutSimClient implements ClientModInitializer {
    public static final String MOD_ID = "donutsim";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static KeyMapping openConfigKey;
    private int tickCounter = 0;

    @Override
    public void onInitializeClient() {
        DonutSimConfig.load();

        // Register 100% client-side intercepted commands (never sent to server)
        DonutSimCommands.register();

        // Register custom DonutSMP sidebar HUD renderer
        DonutSimHudRenderer.register();

        // Bind Right Shift to open the DonutSim GUI screen
        openConfigKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.donutsim.open_config",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                KeyMapping.Category.MISC
        ));

        // Client tick loop: Keyall countdown, fake item pickup checks, and GUI hotkey
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openConfigKey.consumeClick()) {
                if (client.screen == null) {
                    client.setScreen(new DonutSimConfigScreen());
                }
            }

            if (client.level != null && client.player != null) {
                FakeDropManager.tick(client);

                tickCounter++;
                if (tickCounter >= 20) {
                    tickCounter = 0;
                    DonutSimConfig cfg = DonutSimConfig.get();
                    if (cfg.keyallTimerSeconds > 0) {
                        cfg.keyallTimerSeconds--;
                    } else {
                        cfg.keyallTimerSeconds = 900; // Reset to 15m 00s
                    }
                }
            }
        });

        // Clean up local-only visual entities on disconnect (zero network packets)
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            FakeDropManager.clearAll();
            DonutSimConfig.save();
        });

        LOGGER.info("DonutSim Client initialized (100% client-side stealth mode for MC 26.3).");
    }
}
