package blue.deniz.shitfixes.mixin;

import blue.deniz.shitfixes.RecordedRegistryIds;
import java.nio.file.Path;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.moulberry.flashback.Flashback")
abstract class FlashbackReplayRegistryIdsMixin {
    @Inject(
            method = "openReplayWorld",
            at = @At(value = "NEW", target = "net/minecraft/world/level/storage/LevelStorageSource"),
            require = 0)
    private static void shitfixes$applyRecordedRegistryIds(Path replay, CallbackInfo ci) {
        RecordedRegistryIds.applyFromReplay(replay);
    }
}
