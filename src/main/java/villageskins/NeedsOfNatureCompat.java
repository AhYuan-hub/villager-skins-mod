package villageskins;

import villageskins.SkinAssignments;
import villageskins.SkinLibrary;
import villageskins.VillagerSkinsClient;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NeedsOfNatureCompat {
    private static final Logger LOG = LoggerFactory.getLogger((String)"villager_skins");
    private static final Identifier PLAYER_MODEL = Identifier.ofVanilla((String)"player");
    private static final Identifier PHASE = Identifier.of((String)"villageskins", (String)"skin_after_default");
    private static boolean initialized;

    private NeedsOfNatureCompat() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        if (!FabricLoader.getInstance().isModLoaded("needsofnature")) {
            return;
        }
        try {
            Bridge bridge = new Bridge();
            bridge.register();
            LOG.info("\u5df2\u542f\u7528 NeedsOfNature \u52a8\u4f5c\u6362\u80a4\u517c\u5bb9\uff1b\u52a8\u4f5c\u9aa8\u9abc\u6cbf\u7528\u5f53\u524d\u52a8\u4f5c\u5305\u3002");
        }
        catch (LinkageError | ReflectiveOperationException | RuntimeException e) {
            LOG.warn("\u65e0\u6cd5\u542f\u7528 NeedsOfNature \u52a8\u4f5c\u6362\u80a4\u517c\u5bb9\uff0c\u4fdd\u7559\u539f\u52a8\u4f5c\u663e\u793a\uff1b\u666e\u901a\u6362\u80a4\u4e0d\u53d7\u5f71\u54cd\u3002", e);
        }
    }

    private static final class Bridge {
        private static final String EVENTS = "com.nonid.api.client.NonAnimationRenderEvents";
        private static final String GECKO = "com.nonid.internal.animation.client.render.gecko.";
        private final Class<?> modelResolver = Class.forName("com.nonid.api.client.NonAnimationRenderEvents$ModelResolver");
        private final Class<?> renderResolver = Class.forName("com.nonid.api.client.NonAnimationRenderEvents$RenderResolver");
        private final Constructor<?> modelOverride = Class.forName("com.nonid.api.client.NonAnimationRenderEvents$ModelOverride").getConstructor(Identifier.class, Identifier.class);
        private final Constructor<?> renderOverride = Class.forName("com.nonid.api.client.NonAnimationRenderEvents$RenderOverride").getConstructor(Identifier.class, Identifier.class, List.class, Map.class, Map.class, Map.class, Map.class);
        private final Class<?> runtime = Class.forName("com.nonid.internal.animation.client.runtime.ClientAnimationRuntime");
        private final Method active = this.runtime.getMethod("isActorActive", UUID.class);
        private final Method roots = this.runtime.getMethod("findLatestModelRootsForActor", UUID.class);
        private final Class<?> genderHolder = Class.forName("com.nonid.GenderHolder");
        private final Method genderMask = this.genderHolder.getMethod("getGenderMask", new Class[0]);
        private final Class<?> gender = Class.forName("com.nonid.internal.animation.client.render.gecko.GenderedModelResolver$ModelGender");
        private final Method fromMask = Class.forName("com.nonid.internal.animation.client.render.gecko.GenderedModelResolver").getMethod("fromMask", Integer.TYPE);
        private final Object defaultGender = this.gender.getField("MALE").get(null);
        private final Method resolve = Class.forName("com.nonid.internal.animation.client.render.gecko.GeckoResourceResolver").getMethod("resolveModelAndTexture", Identifier.class, List.class, List.class, this.gender);
        private final Class<?> modelAndTexture = Class.forName("com.nonid.internal.animation.client.render.gecko.GeckoResourceResolver$ModelAndTexture");
        private final Method missing = this.modelAndTexture.getMethod("missingModel", new Class[0]);
        private final Method model = this.modelAndTexture.getMethod("model", new Class[0]);
        private final Set<String> warnedModels = new HashSet<String>();
        private boolean enabled;

        Bridge() throws ReflectiveOperationException {
        }

        void register() throws ReflectiveOperationException {
            Class<?> events = Class.forName(EVENTS);
            Event early = (Event)events.getField("RESOLVE").get(null);
            Event late = (Event)events.getField("RESOLVE_RENDER").get(null);
            Object earlyListener = this.listener(this.modelResolver, false);
            Object lateListener = this.listener(this.renderResolver, true);
            early.addPhaseOrdering(Event.DEFAULT_PHASE, PHASE);
            late.addPhaseOrdering(Event.DEFAULT_PHASE, PHASE);
            early.register(PHASE, earlyListener);
            late.register(PHASE, lateListener);
            this.enabled = true;
        }

        private Object listener(Class<?> callback, boolean finalPass) {
            return Proxy.newProxyInstance(callback.getClassLoader(), new Class[]{callback}, (proxy, method, args) -> {
                Object patt0$temp;
                if (method.getDeclaringClass() == Object.class) {
                    return switch (method.getName()) {
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        case "toString" -> "\u6751\u6c11\u6362\u80a4\u52a8\u4f5c\u517c\u5bb9\u56de\u8c03";
                        default -> null;
                    };
                }
                if (!(this.enabled && method.getName().equals("resolve") && args != null && args.length == 4 && (patt0$temp = args[0]) instanceof VillagerEntity)) {
                    return null;
                }
                VillagerEntity villager = (VillagerEntity)patt0$temp;
                try {
                    Appearance appearance = this.appearance(villager);
                    if (appearance == null) {
                        return null;
                    }
                    if (!finalPass) {
                        return this.modelOverride.newInstance(appearance.model(), appearance.texture());
                    }
                    return this.renderOverride.newInstance(appearance.model(), appearance.texture(), List.of(), null, null, null, null);
                }
                catch (LinkageError | ReflectiveOperationException | RuntimeException e) {
                    this.enabled = false;
                    LOG.warn("NeedsOfNature \u52a8\u4f5c\u63a5\u53e3\u8c03\u7528\u5931\u8d25\uff0c\u5df2\u505c\u6b62\u672c\u6b21\u542f\u52a8\u7684\u52a8\u4f5c\u6362\u80a4\u6865\u63a5\uff0c\u4fdd\u7559\u539f\u52a8\u4f5c\u663e\u793a\u3002", e);
                    return null;
                }
            });
        }

        private Appearance appearance(VillagerEntity villager) throws ReflectiveOperationException {
            SkinLibrary.Skin skin;
            SkinAssignments.Choice choice = VillagerSkinsClient.choiceFor(villager);
            SkinLibrary.Skin skin2 = skin = choice == null ? null : VillagerSkinsClient.library.get(choice.file());
            if (skin == null || !Boolean.TRUE.equals(this.active.invoke(null, villager.getUuid()))) {
                return null;
            }
            Object actorRoots = this.roots.invoke(null, villager.getUuid());
            if (!(actorRoots instanceof List)) {
                return null;
            }
            List modelRoots = (List)actorRoots;
            Object actorGender = this.genderHolder.isInstance(villager) ? this.fromMask.invoke(null, this.genderMask.invoke((Object)villager, new Object[0])) : this.defaultGender;
            String path = choice.slim() ? "player_slim" : "player";
            Object resolved = this.resolve.invoke(null, PLAYER_MODEL, modelRoots, List.of(path), actorGender);
            if (resolved == null || Boolean.TRUE.equals(this.missing.invoke(resolved, new Object[0]))) {
                this.warnMissing(path, actorGender, modelRoots);
                return null;
            }
            Object resolvedModel = this.model.invoke(resolved, new Object[0]);
            if (!(resolvedModel instanceof Identifier)) {
                return null;
            }
            Identifier modelId = (Identifier)resolvedModel;
            return new Appearance(modelId, skin.texture());
        }

        private void warnMissing(String path, Object actorGender, List<?> modelRoots) {
            String key = path + "/" + String.valueOf(actorGender) + "/" + String.valueOf(modelRoots);
            if (this.warnedModels.size() < 32 && this.warnedModels.add(key)) {
                LOG.warn("\u5f53\u524d\u52a8\u4f5c\u5305\u7f3a\u5c11\u5339\u914d\u7684 {} \u73a9\u5bb6\u52a8\u4f5c\u6a21\u578b\uff0c\u4fdd\u7559\u8be5\u52a8\u4f5c\u7684\u539f\u6709\u5916\u89c2\uff0c\u907f\u514d\u76ae\u80a4\u9519\u4f4d\uff1b\u6a21\u578b\u6765\u6e90\uff1a{}\u3002", (Object)path, modelRoots);
            }
        }
    }

    private record Appearance(Identifier model, Identifier texture) {
    }
}

