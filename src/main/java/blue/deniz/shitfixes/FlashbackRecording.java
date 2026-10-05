package blue.deniz.shitfixes;

import com.mojang.logging.LogUtils;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.Packet;
import net.neoforged.fml.ModList;

public final class FlashbackRecording {
    private static final MethodHandle GET_RECORDER;
    private static final MethodHandle WRITE_PACKET;
    private static boolean failed;

    static {
        MethodHandle getRecorder = null, writePacket = null;
        if (ModList.get().isLoaded("flashback")) {
            try {
                Class<?> flashback = Class.forName("com.moulberry.flashback.Flashback");
                Class<?> recorder = Class.forName("com.moulberry.flashback.record.Recorder");
                MethodHandles.Lookup lookup = MethodHandles.publicLookup();
                getRecorder = lookup.findStaticGetter(flashback, "RECORDER", recorder)
                        .asType(MethodType.methodType(Object.class));
                writePacket = lookup.findVirtual(recorder, "writePacketAsync",
                                MethodType.methodType(void.class, Packet.class, ConnectionProtocol.class))
                        .asType(MethodType.methodType(void.class, Object.class, Packet.class, ConnectionProtocol.class));
            } catch (ReflectiveOperationException e) {
                LogUtils.getLogger().warn("Flashback is loaded but its recorder is missing", e);
                getRecorder = writePacket = null;
            }
        }
        GET_RECORDER = getRecorder;
        WRITE_PACKET = writePacket;
    }

    private FlashbackRecording() {}

    public static void writeReceivedPacket(Packet<?> packet) {
        if (WRITE_PACKET == null || failed) return;
        try {
            Object recorder = (Object) GET_RECORDER.invokeExact();
            if (recorder != null) WRITE_PACKET.invokeExact(recorder, packet, ConnectionProtocol.PLAY);
        } catch (Throwable e) {
            failed = true;
            LogUtils.getLogger().warn("Writing a packet into the Flashback recording failed, no longer trying", e);
        }
    }
}
