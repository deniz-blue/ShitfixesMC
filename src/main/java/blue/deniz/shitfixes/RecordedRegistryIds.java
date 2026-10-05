package blue.deniz.shitfixes;

import blue.deniz.shitfixes.mixin.RegistrySnapshotAccessor;
import com.mojang.logging.LogUtils;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.RegistryManager;
import net.neoforged.neoforge.registries.RegistrySnapshot;
import org.slf4j.Logger;

public final class RecordedRegistryIds {
    public static final String FILE_NAME = "shitfixes_registry_ids";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final StreamCodec<FriendlyByteBuf, Map<ResourceLocation, RegistrySnapshot>> CODEC =
            ByteBufCodecs.map(HashMap::new, ResourceLocation.STREAM_CODEC, RegistrySnapshot.STREAM_CODEC);

    private RecordedRegistryIds() {}

    public static byte[] captureCurrent() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        CODEC.encode(buf, RegistryManager.takeSnapshot(RegistryManager.SnapshotType.SYNC_TO_CLIENT));
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        return bytes;
    }

    public static void writeToRecording(Path recordFolder, byte[] ids) {
        try {
            Files.write(recordFolder.resolve(FILE_NAME), ids);
        } catch (IOException e) {
            LOGGER.warn("Could not save registry IDs into {}", recordFolder, e);
        }
    }

    public static void copyIntoReplay(Path recordFolder, ZipOutputStream replay) {
        Path ids = recordFolder.resolve(FILE_NAME);
        if (!Files.exists(ids)) return;
        try {
            replay.putNextEntry(new ZipEntry(FILE_NAME));
            Files.copy(ids, replay);
            replay.closeEntry();
        } catch (IOException e) {
            LOGGER.warn("Could not add registry IDs to the replay", e);
        }
    }

    public static void applyFromReplay(Path replay) {
        byte[] ids;
        try (ZipFile zip = new ZipFile(replay.toFile())) {
            ZipEntry entry = zip.getEntry(FILE_NAME);
            if (entry == null) return;
            try (InputStream in = zip.getInputStream(entry)) {
                ids = in.readAllBytes();
            }
        } catch (IOException e) {
            LOGGER.warn("Could not read registry IDs from {}", replay, e);
            return;
        }
        try {
            Map<ResourceLocation, RegistrySnapshot> snapshots = CODEC.decode(new FriendlyByteBuf(Unpooled.wrappedBuffer(ids)));
            snapshots.values().forEach(RecordedRegistryIds::dropUnassignedIds);
            Set<ResourceKey<?>> missing = RegistryManager.applySnapshot(snapshots, false);
            if (missing.isEmpty()) {
                LOGGER.info("Applied the recorded registry IDs from {}", replay.getFileName());
                return;
            }
            LOGGER.warn("Kept this game's registry IDs for {}: {} recorded entries are not registered here: {}",
                    replay.getFileName(), missing.size(), missing);
        } catch (RuntimeException e) {
            LOGGER.warn("Kept this game's registry IDs for {}: the recorded ones could not be applied", replay.getFileName(), e);
        }
        RegistryManager.revertToFrozen();
    }

    // While connected, NeoForge leaves client-only entries the server did not send without an ID,
    // and a snapshot records them as -1.
    private static void dropUnassignedIds(RegistrySnapshot snapshot) {
        ((RegistrySnapshotAccessor) snapshot).shitfixes$getMutableIds().headMap(0).clear();
    }
}
