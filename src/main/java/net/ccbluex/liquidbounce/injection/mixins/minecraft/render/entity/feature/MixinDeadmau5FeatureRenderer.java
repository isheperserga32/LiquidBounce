package net.ccbluex.liquidbounce.injection.mixins.minecraft.render.entity.feature;

import net.minecraft.client.render.entity.feature.Deadmau5FeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Deadmau5FeatureRenderer.class)
public class MixinDeadmau5FeatureRenderer {

    // todo: what the flip
//    @ModifyExpressionValue(
//            method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/state/PlayerEntityRenderState;FF)V",
//            at = @At(value = "INVOKE", target = "Ljava/lang/String;equals(Ljava/lang/Object;)Z")
//    )
//    private boolean onRender(
//            boolean original,
//            MatrixStack matrixStack,
//            VertexConsumerProvider vertexConsumerProvider,
//            int i,
//            AbstractClientPlayerEntity abstractClientPlayerEntity,
//            float f,
//            float g,
//            float h,
//            float j,
//            float k,
//            float l
//    ) {
//        return original || CosmeticService.INSTANCE.hasCosmetic(abstractClientPlayerEntity.getUuid(),
//                CosmeticCategory.DEADMAU5_EARS);
//    }

}
