package blue.deniz.shitfixes.mixin;

import blue.deniz.shitfixes.PolytoneDynamicEntries;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

// Polytone registers resource pack particles and sounds into the built-in registries, and leaves
// them out of NeoForge's registry snapshot, so they disappear after leaving a server until the next
// resource reload. Flashback compares registry namespaces at record time with the current game, so
// a replay recorded on a server with Angel's Weather loaded always shows a registry mismatch.
@Pseudo
@Mixin(targets = "com.moulberry.flashback.RegistryMetaHelper")
abstract class FlashbackRegistryNamespacesMixin {
    @WrapOperation(
            method = "calculateNamespacesForRegistries",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/Registry;entrySet()Ljava/util/Set;"),
            require = 0)
    private static <T> Set<Map.Entry<ResourceKey<T>, T>> shitfixes$skipPolytoneEntries(
            Registry<T> registry, Operation<Set<Map.Entry<ResourceKey<T>, T>>> original) {
        Set<Map.Entry<ResourceKey<T>, T>> entries = new LinkedHashSet<>(original.call(registry));
        entries.removeIf(entry -> PolytoneDynamicEntries.contains(registry, entry.getKey().location()));
        return entries;
    }
}
