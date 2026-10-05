package blue.deniz.shitfixes.mixin;

import blue.deniz.shitfixes.RecordedRegistryIds;
import java.nio.file.Path;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Recordings store blocks, items and entities by the numbers the server assigned. NeoForge switches
// the client to the server's numbering on join and back to its own on disconnect, so a replay reads
// those numbers against a different list unless the server's numbering travels with the recording.
@Pseudo
@Mixin(targets = "com.moulberry.flashback.record.Recorder")
abstract class FlashbackRecorderRegistryIdsMixin {
    @Unique
    private byte[] shitfixes$serverRegistryIds;

    @Inject(method = "writeSnapshot", at = @At("HEAD"), require = 0)
    private void shitfixes$captureServerRegistryIds(boolean force, CallbackInfo ci) {
        if (shitfixes$serverRegistryIds == null) shitfixes$serverRegistryIds = RecordedRegistryIds.captureCurrent();
    }

    @Inject(method = "finish", at = @At("RETURN"), require = 0)
    private void shitfixes$saveServerRegistryIds(CallbackInfoReturnable<Path> cir) {
        if (shitfixes$serverRegistryIds != null) {
            RecordedRegistryIds.writeToRecording(cir.getReturnValue(), shitfixes$serverRegistryIds);
        }
    }
}
