package blue.deniz.shitfixes.mixin;

import com.bawnorton.mixinsquared.TargetHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// MixinSquared only finds handlers merged before this mixin applies, so the priority has to be above ServerCore's 1000.
@Mixin(value = PlayerList.class, priority = 1500)
abstract class SkipServerCoreMoveToSpawnInReplayMixin {
    @Shadow @Final private MinecraftServer server;

    @TargetHandler(
            mixin = "me.wesley1808.servercore.mixin.optimizations.players.PlayerListMixin",
            name = "servercore$moveToSpawn")
    @Inject(method = "@MixinSquared:Handler", at = @At("HEAD"), cancellable = true, require = 0)
    private void shitfixes$skipMoveToSpawnInReplay(CallbackInfo ci) {
        if (server.getClass().getName().equals("com.moulberry.flashback.playback.ReplayServer")) ci.cancel();
    }
}
