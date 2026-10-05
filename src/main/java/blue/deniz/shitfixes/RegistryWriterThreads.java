package blue.deniz.shitfixes;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import org.slf4j.Logger;

// MappedRegistry keeps its entries in plain HashMaps. Two threads writing one registry at the same
// time can drop entries without an error, and the lost entry only shows up later as
// "Trying to access unbound value" from a DeferredHolder.
@EventBusSubscriber(modid = Shitfixes.MOD_ID)
public final class RegistryWriterThreads {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<Registry<?>, RegistryWriters> WRITERS_BY_REGISTRY = new ConcurrentHashMap<>();
    private static volatile boolean recording = true;

    public static <R> R recordWrite(Registry<?> registry, Supplier<R> write) {
        if (!recording) return write.get();
        RegistryWriters writers = WRITERS_BY_REGISTRY.get(registry);
        if (writers == null) writers = WRITERS_BY_REGISTRY.computeIfAbsent(registry, RegistryWriters::new);
        ThreadWrites own = writers.enter();
        try {
            return write.get();
        } finally {
            own.depth--;
        }
    }

    @SubscribeEvent
    public static void reportWritersAfterLoading(FMLLoadCompleteEvent event) {
        recording = false;
        Set<String> threadNames = new TreeSet<>();
        WRITERS_BY_REGISTRY.values().forEach(writers -> {
            writers.reportThreadsWritingAtOnce();
            writers.byThread.values().forEach(writes -> threadNames.add(writes.threadName));
        });
        LOGGER.info("Recorded writes to {} registries from threads {}", WRITERS_BY_REGISTRY.size(), threadNames);
        WRITERS_BY_REGISTRY.clear();
    }

    private static final class RegistryWriters {
        private final Registry<?> registry;
        private final Map<Thread, ThreadWrites> byThread = new ConcurrentHashMap<>();
        private final Set<String> reportedCollisions = ConcurrentHashMap.newKeySet();

        RegistryWriters(Registry<?> registry) {
            this.registry = registry;
        }

        ThreadWrites enter() {
            Thread current = Thread.currentThread();
            ThreadWrites own = byThread.get(current);
            if (own == null) {
                own = new ThreadWrites(current.getName(), new Throwable("first write from " + current.getName()));
                byThread.put(current, own);
            }
            own.depth++;
            own.count++;
            own.lastNanos = System.nanoTime();
            for (Map.Entry<Thread, ThreadWrites> other : byThread.entrySet()) {
                if (other.getKey() != current && other.getValue().depth > 0) reportCollision(current, other.getKey());
            }
            return own;
        }

        private void reportCollision(Thread current, Thread other) {
            if (!reportedCollisions.add(current.getName() + " / " + other.getName())) return;
            Throwable otherWrite = new Throwable(other.getName() + " was writing at");
            otherWrite.setStackTrace(other.getStackTrace());
            Throwable currentWrite = new Throwable(current.getName() + " started writing at");
            currentWrite.addSuppressed(otherWrite);
            LOGGER.error("{} written by {} while {} was still writing it",
                    registry.key().location(), current.getName(), other.getName(), currentWrite);
        }

        void reportThreadsWritingAtOnce() {
            List<ThreadWrites> writers = new ArrayList<>(byThread.values());
            for (int i = 0; i < writers.size(); i++) {
                for (int j = i + 1; j < writers.size(); j++) {
                    ThreadWrites a = writers.get(i), b = writers.get(j);
                    if (a.firstNanos > b.lastNanos || b.firstNanos > a.lastNanos) continue;
                    Throwable firstWrites = new Throwable("first writes");
                    firstWrites.addSuppressed(a.firstWrite);
                    firstWrites.addSuppressed(b.firstWrite);
                    LOGGER.warn("{} was written by {} ({} writes) and {} ({} writes) during the same stretch",
                            registry.key().location(), a.threadName, a.count, b.threadName, b.count, firstWrites);
                }
            }
        }
    }

    private static final class ThreadWrites {
        final String threadName;
        final Throwable firstWrite;
        final long firstNanos = System.nanoTime();
        volatile long lastNanos = firstNanos;
        volatile int depth;
        long count;

        ThreadWrites(String threadName, Throwable firstWrite) {
            this.threadName = threadName;
            this.firstWrite = firstWrite;
        }
    }
}
