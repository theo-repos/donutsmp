package net.donutsim.mixin;

import net.donutsim.config.DonutSimConfig;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.scores.Objective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class InGameHudMixin {
    @Inject(method = "displayScoreboardSidebar", at = @At("HEAD"), cancellable = true)
    private void donutsim$suppressVanillaScoreboard(GuiGraphics graphics, Objective objective, CallbackInfo ci) {
        if (DonutSimConfig.get().hudEnabled) {
            ci.cancel();
        }
    }
}
