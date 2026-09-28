package cn.blockforge.generated.mod5c31df19;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import java.util.*;

/** 左侧翻页选皮肤，右侧显示当前村民的真实模型；保存前只作预览。 */
public final class SkinScreen extends Screen {
    private final VillagerEntity target;
    private String selected;
    private boolean slim;
    private int page;
    private int rows;
    private int left;
    private int panelWidth;
    private int split;
    private String message = "";
    private List<SkinLibrary.Skin> listed = List.of();

    public SkinScreen(VillagerEntity target) {
        super(Text.literal("村民换肤"));
        this.target = target;
        SkinAssignments.Choice choice = VillagerSkinsClient.assignments.get(target.getUuid());
        if (choice != null) {
            selected = choice.file();
            slim = choice.slim();
        } else {
            selected = VillagerSkinsClient.library.get("猫羽.png") != null ? "猫羽.png" : null;
            slim = true;
        }
    }

    public UUID targetId() { return target.getUuid(); }
    public SkinAssignments.Choice previewChoice() {
        return selected == null ? null : new SkinAssignments.Choice(selected, slim);
    }

    @Override protected void init() {
        listed = VillagerSkinsClient.library.all();
        panelWidth = Math.min(500, width - 16);
        left = (width - panelWidth) / 2;
        split = left + panelWidth * 55 / 100;
        int listWidth = split - left - 10;
        rows = Math.max(1, (height - 166) / 22);
        int pages = Math.max(1, (listed.size() + rows - 1) / rows);
        page = Math.min(page, pages - 1);
        for (int row = 0; row < rows; row++) {
            int index = page * rows + row;
            if (index >= listed.size()) break;
            String name = listed.get(index).file();
            String label = (name.equals(selected) ? "✓ " : "") + name;
            addDrawableChild(ButtonWidget.builder(Text.literal(textRenderer.trimToWidth(label, listWidth - 10)), b -> {
                selected = name;
                message = "";
                clearAndInit();
            }).dimensions(left, 49 + row * 22, listWidth, 20).build());
        }
        int pagesY = height - 110;
        ButtonWidget previous = addDrawableChild(ButtonWidget.builder(Text.literal("上一页"), b -> { page--; clearAndInit(); })
                .dimensions(left, pagesY, listWidth / 2 - 2, 20).build());
        previous.active = page > 0;
        ButtonWidget next = addDrawableChild(ButtonWidget.builder(Text.literal("下一页"), b -> { page++; clearAndInit(); })
                .dimensions(left + listWidth / 2 + 2, pagesY, listWidth / 2 - 2, 20).build());
        next.active = page + 1 < pages;
        addDrawableChild(ButtonWidget.builder(Text.literal(slim ? "手臂：Alex 细手臂" : "手臂：普通"), b -> {
            slim = !slim;
            clearAndInit();
        }).dimensions(split, pagesY, left + panelWidth - split, 20).build());
        int third = (panelWidth - 8) / 3;
        addDrawableChild(ButtonWidget.builder(Text.literal("打开皮肤文件夹"), b -> Util.getOperatingSystem().open(VillagerSkinsClient.library.directory().toFile()))
                .dimensions(left, height - 84, third, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("刷新皮肤"), b -> {
            VillagerSkinsClient.library.refresh(true);
            message = VillagerSkinsClient.library.status();
            clearAndInit();
        }).dimensions(left + third + 4, height - 84, third, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("预览原有外观"), b -> {
            selected = null;
            message = "点击“保存”后恢复村民原有外观";
            clearAndInit();
        }).dimensions(left + 2 * (third + 4), height - 84, third, 20).build());
        ButtonWidget save = addDrawableChild(ButtonWidget.builder(Text.literal("保存并返回游戏"), b -> {
            if (selected != null && VillagerSkinsClient.library.get(selected) == null) {
                message = "皮肤文件已删除，请重新选择或恢复原有外观";
                return;
            }
            if (VillagerSkinsClient.assignments.set(target.getUuid(), previewChoice())) close();
            else message = VillagerSkinsClient.assignments.error;
        }).dimensions(left, height - 32, panelWidth / 2 - 2, 20).build());
        save.active = selected == null || VillagerSkinsClient.library.get(selected) != null;
        addDrawableChild(ButtonWidget.builder(Text.literal("取消"), b -> close())
                .dimensions(left + panelWidth / 2 + 2, height - 32, panelWidth / 2 - 2, 20).build());
    }

    @Override public void tick() {
        if (client.world == null || !target.isAlive() || target.getEntityWorld() != client.world) { close(); return; }
        List<SkinLibrary.Skin> now = VillagerSkinsClient.library.all();
        if (!now.equals(listed)) clearAndInit();
    }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 12, 0xffffffff);
        context.drawTextWithShadow(textRenderer, "选择皮肤（64×64 PNG）", left, 34, 0xffb8d9ed);
        context.drawCenteredTextWithShadow(textRenderer, "模型预览", (split + left + panelWidth) / 2, 34, 0xffb8d9ed);
        if (listed.isEmpty()) context.drawWrappedTextWithShadow(textRenderer, Text.literal("文件夹里没有可用皮肤。放入 64×64 PNG 后刷新。"), left + 4, 55, split - left - 18, 0xffdddddd);
        if (target.isAlive()) {
            int bottom = height - 118;
            int scale = Math.max(14, Math.min(60, (bottom - 49) / 2));
            InventoryScreen.drawEntity(context, split, 46, left + panelWidth, bottom, scale, 0.0625F, mouseX, mouseY, target);
        }
        String status = message.isEmpty() ? VillagerSkinsClient.library.status() : message;
        context.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(status, panelWidth), left, height - 58, 0xffffd997);
        context.drawCenteredTextWithShadow(textRenderer, "仅自己可见 · 添加或删除 PNG 后自动更新", width / 2, height - 45, 0xffa9b6c5);
    }

    @Override public void close() { client.setScreen(null); }
    @Override public boolean shouldPause() { return false; }
}
