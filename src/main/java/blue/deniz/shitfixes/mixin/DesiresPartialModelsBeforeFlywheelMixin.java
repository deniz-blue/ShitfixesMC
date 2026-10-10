package blue.deniz.shitfixes.mixin;

import net.neoforged.neoforge.client.event.ModelEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Flywheel registers the partial models that exist when ModelEvent.RegisterAdditional fires, and
// BakingCompleted gives every other partial created by then a null model. Dreams n' Desires creates
// its partials in FMLClientSetupEvent, which runs alongside the first resource reload, so on some
// launches all of them come out null and the first one rendered crashes the client.
@Pseudo
@Mixin(targets = "dev.engine_room.flywheel.lib.model.baked.PartialModelEventHandler")
abstract class DesiresPartialModelsBeforeFlywheelMixin {
    @Inject(
            method = "onRegisterAdditional(Lnet/neoforged/neoforge/client/event/ModelEvent$RegisterAdditional;)V",
            at = @At("HEAD"),
            require = 0)
    private static void shitfixes$createDesiresPartialModels(ModelEvent.RegisterAdditional event, CallbackInfo ci) {
        try {
            Class.forName("dev.lopyluna.dndesires.register.client.DesiresPartialModels");
        } catch (ClassNotFoundException withoutDesires) {
        }
    }
}
