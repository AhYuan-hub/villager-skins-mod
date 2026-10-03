package villageskins;

import villageskins.SkinAssignments;
import villageskins.SkinLibrary;
import villageskins.VillagerSkinsClient;
import java.util.List;
import java.util.UUID;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Util;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.text.Text;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.text.StringVisitable;

public final class SkinScreen
extends Screen {
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
        super((Text)Text.literal((String)"\u6751\u6c11\u6362\u80a4"));
        this.target = target;
        SkinAssignments.Choice choice = VillagerSkinsClient.assignments.get(target.getUuid());
        if (choice != null) {
            this.selected = choice.file();
            this.slim = choice.slim();
        } else {
            this.selected = VillagerSkinsClient.library.get("\u732b\u7fbd.png") != null ? "\u732b\u7fbd.png" : null;
            this.slim = true;
        }
    }

    public UUID targetId() {
        return this.target.getUuid();
    }

    public SkinAssignments.Choice previewChoice() {
        return this.selected == null ? null : new SkinAssignments.Choice(this.selected, this.slim);
    }

    protected void init() {
        int index;
        this.listed = VillagerSkinsClient.library.all();
        this.panelWidth = Math.min(500, this.width - 16);
        this.left = (this.width - this.panelWidth) / 2;
        this.split = this.left + this.panelWidth * 55 / 100;
        int listWidth = this.split - this.left - 10;
        this.rows = Math.max(1, (this.height - 166) / 22);
        int pages = Math.max(1, (this.listed.size() + this.rows - 1) / this.rows);
        this.page = Math.min(this.page, pages - 1);
        for (int row = 0; row < this.rows && (index = this.page * this.rows + row) < this.listed.size(); ++row) {
            String name = this.listed.get(index).file();
            String label = (name.equals(this.selected) ? "\u2713 " : "") + name;
            this.addDrawableChild(ButtonWidget.builder((Text)Text.literal((String)this.textRenderer.trimToWidth(label, listWidth - 10)), b -> {
                this.selected = name;
                this.message = "";
                this.clearAndInit();
            }).dimensions(this.left, 49 + row * 22, listWidth, 20).build());
        }
        int pagesY = this.height - 110;
        ButtonWidget previous = (ButtonWidget)this.addDrawableChild(ButtonWidget.builder((Text)Text.literal((String)"\u4e0a\u4e00\u9875"), b -> {
            --this.page;
            this.clearAndInit();
        }).dimensions(this.left, pagesY, listWidth / 2 - 2, 20).build());
        previous.active = this.page > 0;
        ButtonWidget next = (ButtonWidget)this.addDrawableChild(ButtonWidget.builder((Text)Text.literal((String)"\u4e0b\u4e00\u9875"), b -> {
            ++this.page;
            this.clearAndInit();
        }).dimensions(this.left + listWidth / 2 + 2, pagesY, listWidth / 2 - 2, 20).build());
        next.active = this.page + 1 < pages;
        this.addDrawableChild(ButtonWidget.builder((Text)Text.literal((String)(this.slim ? "\u624b\u81c2\uff1aAlex \u7ec6\u624b\u81c2" : "\u624b\u81c2\uff1a\u666e\u901a")), b -> {
            this.slim = !this.slim;
            this.clearAndInit();
        }).dimensions(this.split, pagesY, this.left + this.panelWidth - this.split, 20).build());
        int third = (this.panelWidth - 8) / 3;
        this.addDrawableChild(ButtonWidget.builder((Text)Text.literal((String)"\u6253\u5f00\u76ae\u80a4\u6587\u4ef6\u5939"), b -> Util.getOperatingSystem().open(VillagerSkinsClient.library.directory().toFile())).dimensions(this.left, this.height - 84, third, 20).build());
        this.addDrawableChild(ButtonWidget.builder((Text)Text.literal((String)"\u5237\u65b0\u76ae\u80a4"), b -> {
            VillagerSkinsClient.library.refresh(true);
            this.message = VillagerSkinsClient.library.status();
            this.clearAndInit();
        }).dimensions(this.left + third + 4, this.height - 84, third, 20).build());
        this.addDrawableChild(ButtonWidget.builder((Text)Text.literal((String)"\u9884\u89c8\u539f\u6709\u5916\u89c2"), b -> {
            this.selected = null;
            this.message = "\u70b9\u51fb\u201c\u4fdd\u5b58\u201d\u540e\u6062\u590d\u6751\u6c11\u539f\u6709\u5916\u89c2";
            this.clearAndInit();
        }).dimensions(this.left + 2 * (third + 4), this.height - 84, third, 20).build());
        ButtonWidget save = (ButtonWidget)this.addDrawableChild(ButtonWidget.builder((Text)Text.literal((String)"\u4fdd\u5b58\u5e76\u8fd4\u56de\u6e38\u620f"), b -> {
            if (this.selected != null && VillagerSkinsClient.library.get(this.selected) == null) {
                this.message = "\u76ae\u80a4\u6587\u4ef6\u5df2\u5220\u9664\uff0c\u8bf7\u91cd\u65b0\u9009\u62e9\u6216\u6062\u590d\u539f\u6709\u5916\u89c2";
                return;
            }
            if (VillagerSkinsClient.assignments.set(this.target.getUuid(), this.previewChoice())) {
                this.close();
            } else {
                this.message = VillagerSkinsClient.assignments.error;
            }
        }).dimensions(this.left, this.height - 32, this.panelWidth / 2 - 2, 20).build());
        save.active = this.selected == null || VillagerSkinsClient.library.get(this.selected) != null;
        this.addDrawableChild(ButtonWidget.builder((Text)Text.literal((String)"\u53d6\u6d88"), b -> this.close()).dimensions(this.left + this.panelWidth / 2 + 2, this.height - 32, this.panelWidth / 2 - 2, 20).build());
    }

    public void tick() {
        if (this.client.world == null || !this.target.isAlive() || this.target.getEntityWorld() != this.client.world) {
            this.close();
            return;
        }
        List<SkinLibrary.Skin> now = VillagerSkinsClient.library.all();
        if (!now.equals(this.listed)) {
            this.clearAndInit();
        }
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 12, -1);
        context.drawTextWithShadow(this.textRenderer, "\u9009\u62e9\u76ae\u80a4\uff0864\u00d764 PNG\uff09", this.left, 34, -4662803);
        context.drawCenteredTextWithShadow(this.textRenderer, "\u6a21\u578b\u9884\u89c8", (this.split + this.left + this.panelWidth) / 2, 34, -4662803);
        if (this.listed.isEmpty()) {
            context.drawWrappedTextWithShadow(this.textRenderer, (StringVisitable)Text.literal((String)"\u6587\u4ef6\u5939\u91cc\u6ca1\u6709\u53ef\u7528\u76ae\u80a4\u3002\u653e\u5165 64\u00d764 PNG \u540e\u5237\u65b0\u3002"), this.left + 4, 55, this.split - this.left - 18, -2236963);
        }
        if (this.target.isAlive()) {
            int bottom = this.height - 118;
            int scale = Math.max(14, Math.min(60, (bottom - 49) / 2));
            InventoryScreen.drawEntity((DrawContext)context, (int)this.split, (int)46, (int)(this.left + this.panelWidth), (int)bottom, (int)scale, (float)0.0625f, (float)mouseX, (float)mouseY, (LivingEntity)this.target);
        }
        String status = this.message.isEmpty() ? VillagerSkinsClient.library.status() : this.message;
        context.drawTextWithShadow(this.textRenderer, this.textRenderer.trimToWidth(status, this.panelWidth), this.left, this.height - 58, -9833);
        context.drawCenteredTextWithShadow(this.textRenderer, "\u4ec5\u81ea\u5df1\u53ef\u89c1 \u00b7 \u6dfb\u52a0\u6216\u5220\u9664 PNG \u540e\u81ea\u52a8\u66f4\u65b0", this.width / 2, this.height - 45, -5654843);
    }

    public void close() {
        this.client.setScreen(null);
    }

    public boolean shouldPause() {
        return false;
    }
}

