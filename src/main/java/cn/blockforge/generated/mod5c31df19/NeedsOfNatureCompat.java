package cn.blockforge.generated.mod5c31df19;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * NeedsOfNature 1.5.1 的可选客户端桥接。
 * 通过其公开显示事件提供模型与贴图，不替换动作、参与者类型或播放时间线。
 * 所有可选类仅在确认安装后解析，不把对方的 jar 或连接库打包进本模组。
 */
public final class NeedsOfNatureCompat {
    private static final Logger LOG = LoggerFactory.getLogger("villager_skins");
    private static final Identifier PLAYER_MODEL = Identifier.ofVanilla("player");
    private static final Identifier PHASE = Identifier.of(GeneratedMod.MOD_ID, "skin_after_default");
    private static boolean initialized;

    private NeedsOfNatureCompat() { }

    /** 在全部客户端入口执行后注册，避免初始化顺序影响皮肤覆盖。 */
    public static void initialize() {
        if (initialized) return;
        initialized = true;
        if (!FabricLoader.getInstance().isModLoaded("needsofnature")) return;
        try {
            Bridge bridge = new Bridge();
            bridge.register();
            LOG.info("已启用 NeedsOfNature 动作换肤兼容；动作骨骼沿用当前动作包。");
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOG.warn("无法启用 NeedsOfNature 动作换肤兼容，保留原动作显示；普通换肤不受影响。", e);
        }
    }

    private record Appearance(Identifier model, Identifier texture) { }

    private static final class Bridge {
        private static final String EVENTS = "com.nonid.api.client.NonAnimationRenderEvents";
        private static final String GECKO = "com.nonid.internal.animation.client.render.gecko.";
        private final Class<?> modelResolver = Class.forName(EVENTS + "$ModelResolver");
        private final Class<?> renderResolver = Class.forName(EVENTS + "$RenderResolver");
        private final Constructor<?> modelOverride = Class.forName(EVENTS + "$ModelOverride")
                .getConstructor(Identifier.class, Identifier.class);
        private final Constructor<?> renderOverride = Class.forName(EVENTS + "$RenderOverride")
                .getConstructor(Identifier.class, Identifier.class, List.class,
                        java.util.Map.class, java.util.Map.class, java.util.Map.class, java.util.Map.class);
        private final Class<?> runtime = Class.forName(
                "com.nonid.internal.animation.client.runtime.ClientAnimationRuntime");
        private final Method active = runtime.getMethod("isActorActive", UUID.class);
        private final Method roots = runtime.getMethod("findLatestModelRootsForActor", UUID.class);
        private final Class<?> genderHolder = Class.forName("com.nonid.GenderHolder");
        private final Method genderMask = genderHolder.getMethod("getGenderMask");
        private final Class<?> gender = Class.forName(GECKO + "GenderedModelResolver$ModelGender");
        private final Method fromMask = Class.forName(GECKO + "GenderedModelResolver")
                .getMethod("fromMask", int.class);
        private final Object defaultGender = gender.getField("MALE").get(null);
        private final Method resolve = Class.forName(GECKO + "GeckoResourceResolver")
                .getMethod("resolveModelAndTexture", Identifier.class, List.class, List.class, gender);
        private final Class<?> modelAndTexture = Class.forName(GECKO + "GeckoResourceResolver$ModelAndTexture");
        private final Method missing = modelAndTexture.getMethod("missingModel");
        private final Method model = modelAndTexture.getMethod("model");
        private final Set<String> warnedModels = new HashSet<>();
        private boolean enabled;

        Bridge() throws ReflectiveOperationException { }

        @SuppressWarnings("unchecked")
        void register() throws ReflectiveOperationException {
            Class<?> events = Class.forName(EVENTS);
            Event<Object> early = (Event<Object>) events.getField("RESOLVE").get(null);
            Event<Object> late = (Event<Object>) events.getField("RESOLVE_RENDER").get(null);
            Object earlyListener = listener(modelResolver, false);
            Object lateListener = listener(renderResolver, true);
            // 两个事件都注册完成才启用；中途失败时已注册的回调也只返回 null。
            early.addPhaseOrdering(Event.DEFAULT_PHASE, PHASE);
            late.addPhaseOrdering(Event.DEFAULT_PHASE, PHASE);
            early.register(PHASE, earlyListener);
            late.register(PHASE, lateListener);
            enabled = true;
        }

        private Object listener(Class<?> callback, boolean finalPass) {
            return Proxy.newProxyInstance(callback.getClassLoader(), new Class<?>[]{callback},
                    (proxy, method, args) -> {
                        if (method.getDeclaringClass() == Object.class) {
                            return switch (method.getName()) {
                                case "hashCode" -> System.identityHashCode(proxy);
                                case "equals" -> proxy == args[0];
                                case "toString" -> "村民换肤动作兼容回调";
                                default -> null;
                            };
                        }
                        if (!enabled || !method.getName().equals("resolve")
                                || args == null || args.length != 4
                                || !(args[0] instanceof VillagerEntity villager)) return null;
                        try {
                            Appearance appearance = appearance(villager);
                            if (appearance == null) return null;
                            if (!finalPass) {
                                return modelOverride.newInstance(appearance.model(), appearance.texture());
                            }
                            // 清除原村民的整身贴图叠层，避免职业衣服/旧外观覆盖玩家皮肤。
                            // null 保留动作包的骨骼道具、局部贴图与可见性，而不是清空动作。
                            return renderOverride.newInstance(appearance.model(), appearance.texture(),
                                    List.of(), null, null, null, null);
                        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                            enabled = false;
                            LOG.warn("NeedsOfNature 动作接口调用失败，已停止本次启动的动作换肤桥接，保留原动作显示。", e);
                            return null;
                        }
                    });
        }

        private Appearance appearance(VillagerEntity villager) throws ReflectiveOperationException {
            SkinAssignments.Choice choice = VillagerSkinsClient.choiceFor(villager);
            SkinLibrary.Skin skin = choice == null ? null : VillagerSkinsClient.library.get(choice.file());
            // 每次从当前选择读取，删除/替换 PNG、取消预览或恢复原貌立即停止覆盖。
            if (skin == null || !Boolean.TRUE.equals(active.invoke(null, villager.getUuid()))) return null;
            Object actorRoots = roots.invoke(null, villager.getUuid());
            if (!(actorRoots instanceof List<?> modelRoots)) return null;
            Object actorGender = genderHolder.isInstance(villager)
                    ? fromMask.invoke(null, genderMask.invoke(villager)) : defaultGender;
            String path = choice.slim() ? "player_slim" : "player";
            // 只查匹配的玩家骨骼，不把 Alex 贴图硬贴到宽手臂或原版村民模型上。
            // 调用对方的解析器，以保留动作包导入顺序、模型变体与资源重载缓存。
            Object resolved = resolve.invoke(null, PLAYER_MODEL, modelRoots, List.of(path), actorGender);
            if (resolved == null || Boolean.TRUE.equals(missing.invoke(resolved))) {
                warnMissing(path, actorGender, modelRoots);
                return null;
            }
            Object resolvedModel = model.invoke(resolved);
            if (!(resolvedModel instanceof Identifier modelId)) return null;
            return new Appearance(modelId, skin.texture());
        }

        private void warnMissing(String path, Object actorGender, List<?> modelRoots) {
            String key = path + "/" + actorGender + "/" + modelRoots;
            if (warnedModels.size() < 32 && warnedModels.add(key)) {
                LOG.warn("当前动作包缺少匹配的 {} 玩家动作模型，保留该动作的原有外观，避免皮肤错位；模型来源：{}。",
                        path, modelRoots);
            }
        }
    }
}
