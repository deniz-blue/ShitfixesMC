package blue.deniz.shitfixes.mixin;

import blue.deniz.shitfixes.RecordedRegistryIds;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import java.nio.file.Path;
import java.util.zip.ZipOutputStream;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "com.moulberry.flashback.record.ReplayExporter")
abstract class FlashbackExporterRegistryIdsMixin {
    @WrapOperation(
            method = "export",
            at = @At(value = "INVOKE", target = "Ljava/util/zip/ZipOutputStream;close()V"),
            require = 0)
    private static void shitfixes$addRegistryIdsToReplay(
            ZipOutputStream replay, Operation<Void> original, @Local(argsOnly = true, ordinal = 0) Path recordFolder) {
        RecordedRegistryIds.copyIntoReplay(recordFolder, replay);
        original.call(replay);
    }
}
