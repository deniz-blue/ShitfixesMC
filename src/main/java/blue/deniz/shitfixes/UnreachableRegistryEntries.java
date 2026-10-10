package blue.deniz.shitfixes;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.slf4j.Logger;

@EventBusSubscriber(modid = Shitfixes.MOD_ID)
public final class UnreachableRegistryEntries {
    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void reportAfterLoading(FMLLoadCompleteEvent event) {
        report("mod loading", BuiltInRegistries.REGISTRY.stream());
    }

    @EventBusSubscriber(modid = Shitfixes.MOD_ID)
    public static final class AfterServerStart {
        @SubscribeEvent
        public static void report(ServerStartedEvent event) {
            UnreachableRegistryEntries.report("server start",
                    event.getServer().registryAccess().registries().map(RegistryAccess.RegistryEntry::value));
        }
    }

    // NeoForge's RegistrySnapshot copies a registry by registering its entries under their old ids
    // in map order, and the copy's by-id list only matches those ids once every entry is in.
    public static <T> void reportIfUnreachableOnceRegistered(Registry<T> registry, Holder.Reference<T> registered) {
        ResourceLocation name = registered.key().location();
        List<String> failed = failedLookupsByNameKeyAndValue(registry, name, registered);
        if (failed.isEmpty()) return;
        LOGGER.error("{} registered {} on thread {} and already cannot find it by {}",
                registry.key().location(), name, Thread.currentThread().getName(), String.join(" and ", failed),
                new Throwable("registered at"));
    }

    private static void report(String finishedStage, Stream<? extends Registry<?>> registries) {
        int checked = 0;
        boolean found = false;
        for (Registry<?> registry : registries.toList()) {
            checked++;
            List<String> unreachable = unreachableEntries(registry);
            if (unreachable.isEmpty()) continue;
            found = true;
            LOGGER.error("After {}, {} has {} entries that some lookups cannot find, restart before playing: {}",
                    finishedStage, registry.key().location(), unreachable.size(), unreachable);
        }
        if (!found) LOGGER.info("After {}, every lookup finds every entry of {} registries", finishedStage, checked);
    }

    // MappedRegistry keeps one map per kind of lookup and fills them one after another. NeoForge's
    // keySet() lists the by-name map, registryKeySet() the by-key map and holders() the by-id list,
    // so an entry missing from one of them still shows up in the other two.
    private static <T> List<String> unreachableEntries(Registry<T> registry) {
        Map<ResourceLocation, Holder.Reference<T>> entriesByName = new HashMap<>();
        registry.keySet().forEach(name -> entriesByName.put(name, registry.getHolder(name).orElse(null)));
        registry.registryKeySet().forEach(key ->
                entriesByName.putIfAbsent(key.location(), registry.getHolder(key).orElse(null)));
        registry.holders().filter(Objects::nonNull).forEach(entry ->
                entriesByName.putIfAbsent(entry.key().location(), entry));

        List<String> unreachable = new ArrayList<>();
        entriesByName.forEach((name, entry) -> {
            List<String> failed = failedLookupsByNameKeyAndValue(registry, name, entry);
            if (entry == null || !entry.isBound() || registry.getHolder(registry.getId(entry.value())).isEmpty()) {
                failed.add("id");
            }
            if (!failed.isEmpty()) unreachable.add(name + " by " + String.join(" and ", failed));
        });
        Collections.sort(unreachable);
        return unreachable;
    }

    private static <T> List<String> failedLookupsByNameKeyAndValue(
            Registry<T> registry, ResourceLocation name, @Nullable Holder.Reference<T> entry) {
        List<String> failed = new ArrayList<>();
        if (registry.getHolder(name).isEmpty()) failed.add("name");
        if (registry.getHolder(ResourceKey.create(registry.key(), name)).isEmpty()) failed.add("key");
        if (entry == null || !entry.isBound() || registry.getResourceKey(entry.value()).isEmpty()) failed.add("value");
        return failed;
    }
}
