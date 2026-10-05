package blue.deniz.shitfixes;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import org.lwjgl.opengl.GL11;

// NeoForge moves each HUD layer 200 units up in z, and once a pack has more than 50 layers
// ClientHooks.getGuiFarPlane() clips the GUI at exactly 200 * layer count. Figura registers
// its overlay above all others, so anything it draws more than 200 units over its own layer
// (action wheel titles, emojis, the paper doll) is clipped away. FiguraMC/Figura#461.
@EventBusSubscriber(modid = Shitfixes.MOD_ID, value = Dist.CLIENT)
public final class FiguraHudDepth {
    private static final ResourceLocation FIGURA_OVERLAY =
            ResourceLocation.fromNamespaceAndPath("figura", "figura_overlay");

    @SubscribeEvent
    public static void resetDepthBeforeFigura(RenderGuiLayerEvent.Pre event) {
        if (!event.getName().equals(FIGURA_OVERLAY)) return;

        var graphics = event.getGuiGraphics();
        graphics.flush();
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(0, 0, -pose.last().pose().m32());
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
    }

    @SubscribeEvent
    public static void restoreDepthAfterFigura(RenderGuiLayerEvent.Post event) {
        if (!event.getName().equals(FIGURA_OVERLAY)) return;

        var graphics = event.getGuiGraphics();
        graphics.flush();
        graphics.pose().popPose();
    }
}
