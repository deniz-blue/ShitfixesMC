package blue.deniz.shitfixes.mixin;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Registrate queues a mod's registry entries in plain Guava collections, and FML constructs mods on
// several threads. In the Starsoul pack two of them add to Create's queue at once. When two puts
// into the item row (a LinkedHashMap) collide, entries drop out of its iteration order and are
// never registered: either the boot dies on "Trying to access unbound value", or the server starts
// without the item and only logs "Unknown registry key".
@Pseudo
@Mixin(targets = "com.tterrag.registrate.AbstractRegistrate")
abstract class SynchronizeRegistrateCollectionsMixin {
    @Shadow @Final @Mutable private Table<Object, Object, Object> registrations;
    @Shadow @Final @Mutable private Multimap<Object, Object> registerCallbacks;
    @Shadow @Final @Mutable private Multimap<Object, Object> afterRegisterCallbacks;
    @Shadow @Final @Mutable private Table<Object, Object, Object> datagensByEntry;
    @Shadow @Final @Mutable private ListMultimap<Object, Object> datagens;
    @Shadow @Final @Mutable private Multimap<Object, Object> creativeModeTabModifiers;

    @Inject(method = "<init>(Ljava/lang/String;)V", at = @At("RETURN"), require = 0)
    private void shitfixes$synchronizeCollections(String modId, CallbackInfo ci) {
        registrations = Tables.synchronizedTable(registrations);
        registerCallbacks = Multimaps.synchronizedMultimap(registerCallbacks);
        afterRegisterCallbacks = Multimaps.synchronizedMultimap(afterRegisterCallbacks);
        datagensByEntry = Tables.synchronizedTable(datagensByEntry);
        datagens = Multimaps.synchronizedListMultimap(datagens);
        creativeModeTabModifiers = Multimaps.synchronizedMultimap(creativeModeTabModifiers);
    }
}
