package cn.blockforge.generated.mod5c31df19;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

public final class VillagerSkinsClient implements ClientModInitializer {
    public static SkinLibrary library;
    public static SkinAssignments assignments;
    private VillagerEntity lastInteracted;
    private VillagerEntity pending;
    private long interactionTime;
    private int refreshTicks;

    @Override public void onInitializeClient() {
        MinecraftClient client = MinecraftClient.getInstance();
        library = new SkinLibrary(client.runDirectory.toPath());
        assignments = new SkinAssignments(client.runDirectory.toPath());
        // 按村民的皮肤选择切换渲染器，不覆盖原版或其他模组的注册。
        ClientLifecycleEvents.CLIENT_STARTED.register(c -> {
            library.refresh(true);
            NeedsOfNatureCompat.initialize();
        });
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (!world.isClient() || player != client.player || player.isSpectator()) return ActionResult.PASS;
            if (!(entity instanceof VillagerEntity villager)) {
                lastInteracted = null;
                return ActionResult.PASS;
            }
            if (player.isSneaking()) {
                if (hand == Hand.MAIN_HAND && client.currentScreen == null) pending = villager;
                // FAIL 在客户端截断此次使用，不发送交易/物品使用包给服务器。
                return ActionResult.FAIL;
            }
            if (hand == Hand.MAIN_HAND) {
                lastInteracted = villager;
                interactionTime = System.nanoTime();
            }
            return ActionResult.PASS;
        });
        ScreenEvents.AFTER_INIT.register((c, screen, width, height) -> {
            if (!(screen instanceof MerchantScreen) || lastInteracted == null) return;
            VillagerEntity target = lastInteracted;
            if (!target.isAlive() || target.getEntityWorld() != c.world || System.nanoTime() - interactionTime > 15_000_000_000L) return;
            Screens.getButtons(screen).add(ButtonWidget.builder(Text.literal("村民换肤"), button -> {
                if (c.player == null || !target.isAlive()) return;
                // 先正常关闭交易容器，避免换肤期间交易格和背包不同步。
                c.player.closeHandledScreen();
                open(target);
            }).dimensions(Math.max(4, width / 2 + 62), Math.max(3, (height - 166) / 2 - 24), 90, 20).build());
        });
        ClientTickEvents.END_CLIENT_TICK.register(c -> {
            assignments.updateScope(c);
            if (c.world == null) {
                pending = null;
                lastInteracted = null;
            }
            if (pending != null) {
                VillagerEntity target = pending;
                pending = null;
                if (c.currentScreen == null && target.isAlive() && target.getEntityWorld() == c.world) open(target);
            }
            if (++refreshTicks >= 40) {
                refreshTicks = 0;
                library.refresh(false);
            }
        });
    }

    private static void open(VillagerEntity target) {
        MinecraftClient client = MinecraftClient.getInstance();
        assignments.updateScope(client);
        library.refresh(false);
        client.setScreen(new SkinScreen(target));
    }

    public static SkinAssignments.Choice choiceFor(VillagerEntity entity) {
        if (assignments == null || library == null) return null;
        if (MinecraftClient.getInstance().currentScreen instanceof SkinScreen screen && screen.targetId().equals(entity.getUuid()))
            return screen.previewChoice();
        return assignments.get(entity.getUuid());
    }
}
