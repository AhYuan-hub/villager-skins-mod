package cn.blockforge.generated.mod5c31df19;

import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.*;
import net.minecraft.client.render.entity.model.*;
import net.minecraft.client.render.entity.state.*;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerSkinType;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.AssetInfo;
import net.minecraft.util.Identifier;

/** 复用游戏自带的普通/Alex 玩家模型；仅在选中有效皮肤时由兼容入口调用。 */
public final class SkinnedVillagerRenderer extends VillagerEntityRenderer {
    private final HumanRenderer normal;
    private final HumanRenderer slim;

    public SkinnedVillagerRenderer(EntityRendererFactory.Context context) {
        super(context);
        normal = new HumanRenderer(context, false);
        slim = new HumanRenderer(context, true);
    }

    public static final class State extends VillagerEntityRenderState {
        private final SkinnedVillagerRenderer renderer;
        final HumanState human = new HumanState();
        boolean custom;
        boolean slim;

        State(SkinnedVillagerRenderer renderer) { this.renderer = renderer; }
        public SkinnedVillagerRenderer renderer() { return renderer; }
    }

    public static final class HumanState extends PlayerEntityRenderState {
        Identifier texture;

        void setSkin(Identifier texture, boolean slim) {
            PlayerSkinType type = slim ? PlayerSkinType.SLIM : PlayerSkinType.WIDE;
            if (texture.equals(this.texture) && skinTextures != null && skinTextures.model() == type) return;
            this.texture = texture;
            // 其他显示模组可能读取标准玩家皮肤字段，不能只填自定义 texture。
            AssetInfo.TextureAsset asset = new AssetInfo.TextureAsset() {
                @Override public Identifier id() { return texture; }
                @Override public Identifier texturePath() { return texture; }
            };
            skinTextures = new SkinTextures(asset, null, null, type, false);
        }
    }

    @Override public VillagerEntityRenderState createRenderState() { return new State(this); }

    @Override public void updateRenderState(VillagerEntity entity, VillagerEntityRenderState state, float tickProgress) {
        super.updateRenderState(entity, state, tickProgress);
        State result = (State) state;
        SkinAssignments.Choice choice = VillagerSkinsClient.choiceFor(entity);
        SkinLibrary.Skin skin = choice == null ? null : VillagerSkinsClient.library.get(choice.file());
        result.custom = skin != null;
        if (skin != null) {
            result.slim = choice.slim();
            result.human.setSkin(skin.texture(), result.slim);
            (result.slim ? slim : normal).updateRenderState(entity, result.human, tickProgress);
        }
    }

    @Override public void render(VillagerEntityRenderState state, MatrixStack matrices,
                                 OrderedRenderCommandQueue queue, CameraRenderState camera) {
        State result = (State) state;
        if (result.custom) {
            // 背包式预览会在更新后调整外层状态的朝向和光照，同步到玩家模型。
            result.human.bodyYaw = state.bodyYaw;
            result.human.relativeHeadYaw = state.relativeHeadYaw;
            result.human.pitch = state.pitch;
            result.human.baseScale = state.baseScale;
            result.human.light = state.light;
            result.human.outlineColor = state.outlineColor;
            (result.slim ? slim : normal).render(result.human, matrices, queue, camera);
        } else super.render(state, matrices, queue, camera);
    }

    private static final class HumanRenderer extends MobEntityRenderer<VillagerEntity, HumanState, PlayerEntityModel> {
        private final ItemModelManager itemModelManager;

        HumanRenderer(EntityRendererFactory.Context context, boolean slim) {
            super(context, new PlayerEntityModel(TexturedModelData.of(PlayerEntityModel.getTexturedModelData(Dilation.NONE, slim), 64, 64).createModel(), slim), 0.5F);
            itemModelManager = context.getItemModelManager();
        }
        @Override public HumanState createRenderState() { return new HumanState(); }
        @Override public Identifier getTexture(HumanState state) { return state.texture; }
        @Override public void updateRenderState(VillagerEntity entity, HumanState state, float progress) {
            // NeedsOfNature 等模组按玩家状态中的 id 查找实体，默认 0 会指向错误对象。
            state.id = entity.getId();
            super.updateRenderState(entity, state, progress);
            BipedEntityRenderer.updateBipedRenderState(entity, state, progress, itemModelManager);
            state.hatVisible = true;
            state.jacketVisible = true;
            state.leftPantsLegVisible = true;
            state.rightPantsLegVisible = true;
            state.leftSleeveVisible = true;
            state.rightSleeveVisible = true;
            state.spectator = false;
        }
        @Override protected void scale(HumanState state, MatrixStack matrices) {
            // 玩家模型与原版玩家同高，幼年村民再按年龄缩小。
            float scale = 0.9375F * state.ageScale;
            matrices.scale(scale, scale, scale);
        }
    }
}
