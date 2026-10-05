package blue.deniz.shitfixes.mixin;

import blue.deniz.shitfixes.RegistryWriterThreads;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(MappedRegistry.class)
abstract class RecordRegistryWriterThreadsMixin<T> {
    @WrapMethod(method = "register(ILnet/minecraft/resources/ResourceKey;Ljava/lang/Object;Lnet/minecraft/core/RegistrationInfo;)Lnet/minecraft/core/Holder$Reference;")
    private Holder.Reference<T> shitfixes$recordRegister(
            int id, ResourceKey<T> key, T value, RegistrationInfo info, Operation<Holder.Reference<T>> original) {
        return RegistryWriterThreads.recordWrite((Registry<?>) (Object) this, () -> original.call(id, key, value, info));
    }

    @WrapMethod(method = "getOrCreateHolderOrThrow")
    private Holder.Reference<T> shitfixes$recordHolderCreation(
            ResourceKey<T> key, Operation<Holder.Reference<T>> original) {
        return RegistryWriterThreads.recordWrite((Registry<?>) (Object) this, () -> original.call(key));
    }

    @WrapMethod(method = "createIntrusiveHolder")
    private Holder.Reference<T> shitfixes$recordIntrusiveHolderCreation(
            T value, Operation<Holder.Reference<T>> original) {
        return RegistryWriterThreads.recordWrite((Registry<?>) (Object) this, () -> original.call(value));
    }
}
