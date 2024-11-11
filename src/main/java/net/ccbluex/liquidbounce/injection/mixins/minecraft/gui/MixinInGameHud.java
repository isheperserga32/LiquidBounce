/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.injection.mixins.minecraft.gui;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.OverlayMessageEvent;
import net.ccbluex.liquidbounce.event.events.PerspectiveEvent;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleAntiBlind;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleFreeCam;
import net.ccbluex.liquidbounce.integration.theme.component.ComponentOverlay;
import net.ccbluex.liquidbounce.integration.theme.component.FeatureTweak;
import net.ccbluex.liquidbounce.integration.theme.component.types.IntegratedComponent;
import net.ccbluex.liquidbounce.render.engine.OverlayRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameMode;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class MixinInGameHud {

    @Final
    @Shadow
    private static Identifier POWDER_SNOW_OUTLINE;

    @Unique
    private static final Identifier PUMPKIN_BLUR = Identifier.ofVanilla("misc/pumpkinblur");

    @Shadow
    @Nullable
    protected abstract PlayerEntity getCameraPlayer();


    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    protected abstract void renderHotbarItem(DrawContext context, int x, int y, RenderTickCounter tickCounter, PlayerEntity player, ItemStack stack, int seed);

    /**
     * Hook render hud event at the top layer
     */
    @Inject(method = "renderMainHud", at = @At("HEAD"))
    private void hookRenderEventStart(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        OverlayRenderer.INSTANCE.start(context, tickCounter.getTickDelta(false));

        // Draw after overlay event
        var component = ComponentOverlay.getComponentWithTweak(FeatureTweak.TWEAK_HOTBAR);
        if (component != null && component.getEnabled() &&
                client.interactionManager.getCurrentGameMode() != GameMode.SPECTATOR) {
            drawHotbar(context, tickCounter, component);
        }
    }

    @Inject(method = "renderOverlay", at = @At("HEAD"), cancellable = true)
    private void injectPumpkinBlur(DrawContext context, Identifier texture, float opacity, CallbackInfo callback) {
        ModuleAntiBlind module = ModuleAntiBlind.INSTANCE;
        if (!module.getEnabled()) {
            return;
        }

        if (module.getPumpkinBlur() && PUMPKIN_BLUR.equals(texture)) {
            callback.cancel();
            return;
        }

        if (module.getPowderSnowFog() && POWDER_SNOW_OUTLINE.equals(texture)) {
            callback.cancel();
        }
    }

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void hookFreeCamRenderCrosshairInThirdPerson(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if ((ModuleFreeCam.INSTANCE.getEnabled() && ModuleFreeCam.INSTANCE.shouldDisableCrosshair())
                || ComponentOverlay.isTweakEnabled(FeatureTweak.DISABLE_CROSSHAIR)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderPortalOverlay", at = @At("HEAD"), cancellable = true)
    private void hookRenderPortalOverlay(CallbackInfo ci) {
        var antiBlind = ModuleAntiBlind.INSTANCE;
        if (antiBlind.getEnabled() && antiBlind.getPortalOverlay()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V", at = @At("HEAD"), cancellable = true)
    private void renderScoreboardSidebar(CallbackInfo ci) {
        if (ComponentOverlay.isTweakEnabled(FeatureTweak.DISABLE_SCOREBOARD)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true)
    private void hookRenderHotbar(CallbackInfo ci) {
        if (ComponentOverlay.isTweakEnabled(FeatureTweak.TWEAK_HOTBAR)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderStatusBars", at = @At("HEAD"), cancellable = true)
    private void hookRenderStatusBars(CallbackInfo ci) {
        if (ComponentOverlay.isTweakEnabled(FeatureTweak.DISABLE_STATUS_BAR)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderExperienceBar", at = @At("HEAD"), cancellable = true)
    private void hookRenderExperienceBar(CallbackInfo ci) {
        if (ComponentOverlay.isTweakEnabled(FeatureTweak.DISABLE_EXP_BAR)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderExperienceLevel", at = @At("HEAD"), cancellable = true)
    private void hookRenderExperienceLevel(CallbackInfo ci) {
        if (ComponentOverlay.isTweakEnabled(FeatureTweak.DISABLE_EXP_BAR)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderHeldItemTooltip", at = @At("HEAD"), cancellable = true)
    private void hookRenderHeldItemTooltip(CallbackInfo ci) {
        if (ComponentOverlay.isTweakEnabled(FeatureTweak.DISABLE_HELD_ITEM_TOOL_TIP)) {
            ci.cancel();
        }
    }

    @Inject(method = "setOverlayMessage", at = @At("HEAD"), cancellable = true)
    private void hookSetOverlayMessage(Text message, boolean tinted, CallbackInfo ci) {
        EventManager.INSTANCE.callEvent(new OverlayMessageEvent(message, tinted));

        if (ComponentOverlay.isTweakEnabled(FeatureTweak.DISABLE_OVERLAY_MESSAGE)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"), cancellable = true)
    private void hookRenderStatusEffectOverlay(CallbackInfo ci) {
        if (ComponentOverlay.isTweakEnabled(FeatureTweak.DISABLE_STATUS_EFFECT_OVERLAY)) {
            ci.cancel();
        }
    }

    @Unique
    private void drawHotbar(DrawContext context, RenderTickCounter tickCounter, IntegratedComponent component) {
        var playerEntity = this.getCameraPlayer();
        if (playerEntity == null) {
            return;
        }

        var itemWidth = 22.5;
        var offset = 98;
        var bounds = component.getAlignment().getBounds(0, 0);

        int center = (int) bounds.getXMin();
        var y = bounds.getYMin() - 12;

        int l = 1;
        for (int m = 0; m < 9; ++m) {
            var x = center - offset + m * itemWidth;
            this.renderHotbarItem(context, (int) x, (int) y, tickCounter, playerEntity,
                    playerEntity.getInventory().main.get(m), l++);
        }

        var offHandStack = playerEntity.getOffHandStack();
        if (!offHandStack.isEmpty()) {
            this.renderHotbarItem(context, center - offset - 32, (int) y, tickCounter, playerEntity, offHandStack, l++);
        }
    }

    @ModifyExpressionValue(method = "renderCrosshair",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/option/GameOptions;getPerspective()Lnet/minecraft/client/option/Perspective;"
            )
    )
    private Perspective hookPerspectiveEventOnCrosshair(Perspective original) {
        return EventManager.INSTANCE.callEvent(new PerspectiveEvent(original)).getPerspective();
    }

    @ModifyExpressionValue(method = "renderMiscOverlays",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/option/GameOptions;getPerspective()Lnet/minecraft/client/option/Perspective;"
            )
    )
    private Perspective hookPerspectiveEventOnMiscOverlays(Perspective original) {
        return EventManager.INSTANCE.callEvent(new PerspectiveEvent(original)).getPerspective();
    }

}
