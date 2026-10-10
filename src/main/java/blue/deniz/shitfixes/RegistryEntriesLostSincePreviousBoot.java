package blue.deniz.shitfixes;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

@EventBusSubscriber(modid = Shitfixes.MOD_ID)
public final class RegistryEntriesLostSincePreviousBoot {
    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void reportAfterLoading(FMLLoadCompleteEvent event) {
        Path entriesOfPreviousBoot = FMLPaths.GAMEDIR.get().resolve("shitfixes").resolve("registry-entries.txt");
        String mods = installedMods();
        List<String> entries = registryEntries();
        try {
            List<String> previous = Files.exists(entriesOfPreviousBoot) ? Files.readAllLines(entriesOfPreviousBoot) : List.of();
            if (!previous.isEmpty() && previous.getFirst().equals(mods)) {
                Set<String> present = new HashSet<>(entries);
                List<String> lost = previous.stream().skip(1).filter(entry -> !present.contains(entry)).toList();
                if (!lost.isEmpty()) {
                    LOGGER.error("{} registry entries of the previous boot are gone although the mods are the same,"
                                    + " restart before playing: {}. Delete {} if a config or script change removed them.",
                            lost.size(), lost, entriesOfPreviousBoot);
                    return;
                }
            }
            List<String> lines = new ArrayList<>(entries.size() + 1);
            lines.add(mods);
            lines.addAll(entries);
            Files.createDirectories(entriesOfPreviousBoot.getParent());
            Files.write(entriesOfPreviousBoot, lines);
        } catch (IOException e) {
            LOGGER.warn("Could not compare the registry entries with {}", entriesOfPreviousBoot, e);
        }
    }

    private static String installedMods() {
        return ModList.get().getMods().stream()
                .map(mod -> mod.getModId() + " " + mod.getVersion())
                .sorted()
                .collect(Collectors.joining(", "));
    }

    private static List<String> registryEntries() {
        List<String> entries = new ArrayList<>();
        for (Registry<?> registry : BuiltInRegistries.REGISTRY) {
            for (ResourceLocation name : registry.keySet()) entries.add(registry.key().location() + " " + name);
        }
        Collections.sort(entries);
        return entries;
    }
}
