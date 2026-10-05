package blue.deniz.shitfixes;

import com.mojang.logging.LogUtils;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import net.neoforged.fml.ModList;

public final class EmfAnimation {
    private static final MethodHandle GET_ROOT;
    private static final MethodHandle PREPARE_ROOT;
    private static final MethodHandle ANIMATE_ROOT;
    private static boolean failed;

    static {
        MethodHandle getRoot = null, prepareRoot = null, animateRoot = null;
        if (ModList.get().isLoaded("entity_model_features")) {
            try {
                Class<?> model = Class.forName("traben.entity_model_features.models.IEMFModel");
                Class<?> root = Class.forName("traben.entity_model_features.models.parts.EMFModelPartRoot");
                MethodHandles.Lookup lookup = MethodHandles.publicLookup();
                getRoot = lookup.findVirtual(model, "emf$getEMFRootModel", MethodType.methodType(root))
                        .asType(MethodType.methodType(Object.class, Object.class));
                prepareRoot = lookup.findVirtual(root, "oneTimeRunnable", MethodType.methodType(void.class))
                        .asType(MethodType.methodType(void.class, Object.class));
                animateRoot = lookup.findVirtual(root, "animate", MethodType.methodType(void.class))
                        .asType(MethodType.methodType(void.class, Object.class));
            } catch (ReflectiveOperationException e) {
                LogUtils.getLogger().warn("EMF is loaded but its model root methods are missing", e);
                getRoot = prepareRoot = animateRoot = null;
            }
        }
        GET_ROOT = getRoot;
        PREPARE_ROOT = prepareRoot;
        ANIMATE_ROOT = animateRoot;
    }

    private EmfAnimation() {}

    public static void applyNow(Object model) {
        if (GET_ROOT == null || failed) return;
        try {
            Object root = GET_ROOT.invokeExact(model);
            if (root == null) return;
            PREPARE_ROOT.invokeExact(root);
            ANIMATE_ROOT.invokeExact(root);
        } catch (Throwable e) {
            failed = true;
            LogUtils.getLogger().warn("Running EMF animations early failed, leaving them to EMF", e);
        }
    }
}
