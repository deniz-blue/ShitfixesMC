package blue.deniz.shitfixes.mixin;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

// While connected, NeoForge numbers sound events by the server's list, so a sound from a client-only
// mod has no number and a sound packet can only carry it by name.
@Pseudo
@Mixin(targets = "com.moulberry.flashback.record.Recorder")
abstract class FlashbackRecorderClientOnlySoundsMixin {
    @ModifyVariable(method = {"writeSound", "writeEntitySound"}, at = @At("HEAD"), argsOnly = true, require = 0)
    private Holder<SoundEvent> shitfixes$inlineSoundWithoutServerId(Holder<SoundEvent> sound) {
        return BuiltInRegistries.SOUND_EVENT.getId(sound.value()) == -1 ? Holder.direct(sound.value()) : sound;
    }
}
