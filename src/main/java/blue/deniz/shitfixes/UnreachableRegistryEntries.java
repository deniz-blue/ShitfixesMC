package blue.deniz.shitfixes;

import com.mojang.logging.LogUtils;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import org.slf4j.Logger;

@EventBusSubscriber(modid = Shitfixes.MOD_ID)
public final class UnreachableRegistryEntries {
    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void reportAfterLoading(FMLLoadCompleteEvent event) {
        int registries = 0;
        boolean found = false;
        for (Registry<?> registry : BuiltInRegistries.REGISTRY) {
            registries++;
            List<ResourceLocation> unreachable = unreachableEntries(registry);
            if (unreachable.isEmpty()) continue;
            found = true;
            LOGGER.error("{} has {} entries that cannot be looked up by key, restart before playing: {}",
                    registry.key().location(), unreachable.size(), unreachable);
        }
        if (!found) LOGGER.info("Every entry of {} registries can be looked up by key", registries);
    }

    private static <T> List<ResourceLocation> unreachableEntries(Registry<T> registry) {
        return registry.keySet().stream()
                .filter(location -> registry.getHolder(ResourceKey.create(registry.key(), location)).isEmpty())
                .sorted()
                .toList();
    }
}
