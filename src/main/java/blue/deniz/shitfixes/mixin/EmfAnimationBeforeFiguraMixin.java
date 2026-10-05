package blue.deniz.shitfixes.mixin;

import blue.deniz.shitfixes.EmfAnimation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// EMF applies resource pack animations (Fresh Animations and its player extension) only when the
// model is drawn, inside renderToBuffer. Figura copies the vanilla pose and renders the avatar right
// after setupAnim, so avatar parts attached to vanilla parts miss those animations.
// EMF runs a model's animation at most once per entity render, so running it here turns the call in
// renderToBuffer into a no-op. Traben-0/Entity_Model_Features#544.
@Mixin(LivingEntityRenderer.class)
abstract class EmfAnimationBeforeFiguraMixin {
    @WrapOperation(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;setupAnim(Lnet/minecraft/world/entity/Entity;FFFFF)V"),
            require = 0)
    private void shitfixes$animateEmfWithVanillaPose(
            EntityModel<Entity> model, Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch, Operation<Void> original) {
        original.call(model, entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        if (entity instanceof Player) EmfAnimation.applyNow(model);
    }
}
