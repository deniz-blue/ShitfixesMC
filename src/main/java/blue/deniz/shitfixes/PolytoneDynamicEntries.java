package blue.deniz.shitfixes;

import com.mojang.logging.LogUtils;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;

public final class PolytoneDynamicEntries {
    private static final MethodHandle IS_ENTRY_DYNAMIC = findIsEntryDynamic();

    private PolytoneDynamicEntries() {}

    public static boolean contains(Registry<?> registry, ResourceLocation id) {
        if (IS_ENTRY_DYNAMIC == null) return false;
        try {
            return (boolean) IS_ENTRY_DYNAMIC.invokeExact(registry, id);
        } catch (Throwable e) {
            return false;
        }
    }

    private static MethodHandle findIsEntryDynamic() {
        if (!ModList.get().isLoaded("polytone")) return null;
        try {
            Class<?> stub = Class.forName("net.mehvahdjukaar.polytone.PolytoneStub");
            return MethodHandles.publicLookup().findStatic(stub, "isEntryDynamic",
                    MethodType.methodType(boolean.class, Registry.class, ResourceLocation.class));
        } catch (ReflectiveOperationException e) {
            LogUtils.getLogger().warn("Polytone is loaded but PolytoneStub.isEntryDynamic is missing", e);
            return null;
        }
    }
}
