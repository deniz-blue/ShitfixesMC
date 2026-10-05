package blue.deniz.shitfixes.mixin;

import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Sable writes the sublevel's orientation into xRot/yRot inside setRotation, and Figura calls
// setRotation(yRot, xRot) again after setup, so Sable applies the orientation a second time.
// FiguraMC/Figura#520, Sable #1563.
@Mixin(Camera.class)
abstract class CameraRotationMixin {
    @Shadow private float xRot;
    @Shadow private float yRot;

    @Inject(method = "setRotation(FF)V", at = @At("HEAD"), cancellable = true)
    private void shitfixes$skipUnchangedRotation(float yRot, float xRot, CallbackInfo ci) {
        if (yRot == this.yRot && xRot == this.xRot) ci.cancel();
    }
}
