package blue.deniz.shitfixes.mixin;

import com.mojang.logging.LogUtils;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// ImmediatelyFast removes the texture from byPath after release, and atlases are only registered
// again on a resource reload. The next lookup of a released atlas registers a placeholder
// DynamicTexture in its place, and Sodium crashes casting it to an atlas.
@Mixin(TextureManager.class)
abstract class KeepTextureAtlasesMixin {
    @Unique
    private static final Set<ResourceLocation> shitfixes$reportedAtlasReleases = ConcurrentHashMap.newKeySet();

    @Shadow @Final private Map<ResourceLocation, AbstractTexture> byPath;

    @Inject(method = "release", at = @At("HEAD"), cancellable = true)
    private void shitfixes$refuseAtlasRelease(ResourceLocation path, CallbackInfo ci) {
        if (!(byPath.get(path) instanceof TextureAtlas)) return;
        ci.cancel();
        if (shitfixes$reportedAtlasReleases.add(path)) {
            LogUtils.getLogger().warn("Refused to release texture atlas {}", path, new Throwable("released by"));
        }
    }
}
