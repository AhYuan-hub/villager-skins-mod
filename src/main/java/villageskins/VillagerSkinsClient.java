package villageskins;

import villageskins.NeedsOfNatureCompat;
import villageskins.SkinAssignments;
import villageskins.SkinLibrary;
import villageskins.SkinScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;

public final class VillagerSkinsClient
implements ClientModInitializer {
    public static SkinLibrary library;
    public static SkinAssignments assignments;
    private VillagerEntity lastInteracted;
    private VillagerEntity pending;
    private long interactionTime;
    private int refreshTicks;

    public void onInitializeClient() {
        MinecraftClient client = MinecraftClient.getInstance();
        library = new SkinLibrary(client.runDirectory.toPath());
        assignments = new SkinAssignments(client.runDirectory.toPath());
        ClientLifecycleEvents.CLIENT_STARTED.register(c -> {
            library.refresh(true);
            NeedsOfNatureCompat.initialize();
        });
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (!world.isClient() || player != client.player || player.isSpectator()) {
                return ActionResult.PASS;
            }
            if (!(entity instanceof VillagerEntity)) {
                this.lastInteracted = null;
                return ActionResult.PASS;
            }
            VillagerEntity villager = (VillagerEntity)entity;
            if (player.isSneaking()) {
                if (hand == Hand.MAIN_HAND && client.currentScreen == null) {
                    this.pending = villager;
                }
                return ActionResult.FAIL;
            }
            if (hand == Hand.MAIN_HAND) {
                this.lastInteracted = villager;
                this.interactionTime = System.nanoTime();
            }
            return ActionResult.PASS;
        });
        ScreenEvents.AFTER_INIT.register((c, screen, width, height) -> {
            if (!(screen instanceof MerchantScreen) || this.lastInteracted == null) {
                return;
            }
            VillagerEntity target = this.lastInteracted;
            if (!target.isAlive() || target.getEntityWorld() != c.world || System.nanoTime() - this.interactionTime > 15000000000L) {
                return;
            }
            Screens.getButtons((Screen)screen).add(ButtonWidget.builder((Text)Text.literal((String)"\u6751\u6c11\u6362\u80a4"), button -> {
                if (c.player == null || !target.isAlive()) {
                    return;
                }
                c.player.closeHandledScreen();
                VillagerSkinsClient.open(target);
            }).dimensions(Math.max(4, width / 2 + 62), Math.max(3, (height - 166) / 2 - 24), 90, 20).build());
        });
        ClientTickEvents.END_CLIENT_TICK.register(c -> {
            assignments.updateScope(c);
            if (c.world == null) {
                this.pending = null;
                this.lastInteracted = null;
            }
            if (this.pending != null) {
                VillagerEntity target = this.pending;
                this.pending = null;
                if (c.currentScreen == null && target.isAlive() && target.getEntityWorld() == c.world) {
                    VillagerSkinsClient.open(target);
                }
            }
            if (++this.refreshTicks >= 40) {
                this.refreshTicks = 0;
                library.refresh(false);
            }
        });
    }

    private static void open(VillagerEntity target) {
        MinecraftClient client = MinecraftClient.getInstance();
        assignments.updateScope(client);
        library.refresh(false);
        client.setScreen((Screen)new SkinScreen(target));
    }

    public static SkinAssignments.Choice choiceFor(VillagerEntity entity) {
        SkinScreen screen;
        if (assignments == null || library == null) {
            return null;
        }
        Screen current = MinecraftClient.getInstance().currentScreen;
        if (current instanceof SkinScreen && (screen = (SkinScreen)current).targetId().equals(entity.getUuid())) {
            return screen.previewChoice();
        }
        return assignments.get(entity.getUuid());
    }
}

