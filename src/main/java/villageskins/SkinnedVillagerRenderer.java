package villageskins;

import villageskins.SkinAssignments;
import villageskins.SkinLibrary;
import villageskins.VillagerSkinsClient;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.entity.state.VillagerEntityRenderState;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.util.AssetInfo;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.player.PlayerSkinType;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.VillagerEntityRenderer;

public final class SkinnedVillagerRenderer
extends VillagerEntityRenderer {
    private final HumanRenderer normal;
    private final HumanRenderer slim;

    public SkinnedVillagerRenderer(net.minecraft.client.render.entity.EntityRendererFactory.Context context) {
        super(context);
        this.normal = new HumanRenderer(context, false);
        this.slim = new HumanRenderer(context, true);
    }

    public VillagerEntityRenderState createRenderState() {
        return new State(this);
    }

    public void updateRenderState(VillagerEntity entity, VillagerEntityRenderState state, float tickProgress) {
        super.updateRenderState(entity, state, tickProgress);
        State result = (State)state;
        SkinAssignments.Choice choice = VillagerSkinsClient.choiceFor(entity);
        SkinLibrary.Skin skin = choice == null ? null : VillagerSkinsClient.library.get(choice.file());
        boolean bl = result.custom = skin != null;
        if (skin != null) {
            result.slim = choice.slim();
            result.human.setSkin(skin.texture(), result.slim);
            (result.slim ? this.slim : this.normal).updateRenderState(entity, result.human, tickProgress);
        }
    }

    public void render(VillagerEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState camera) {
        State result = (State)state;
        if (result.custom) {
            result.human.bodyYaw = state.bodyYaw;
            result.human.relativeHeadYaw = state.relativeHeadYaw;
            result.human.pitch = state.pitch;
            result.human.baseScale = state.baseScale;
            result.human.light = state.light;
            result.human.outlineColor = state.outlineColor;
            (result.slim ? this.slim : this.normal).render(result.human, matrices, queue, camera);
        } else {
            super.render(state, matrices, queue, camera);
        }
    }

    private static final class HumanRenderer
    extends MobEntityRenderer<VillagerEntity, HumanState, PlayerEntityModel> {
        private final ItemModelManager itemModelManager;

        HumanRenderer(net.minecraft.client.render.entity.EntityRendererFactory.Context context, boolean slim) {
            super(context, new PlayerEntityModel(TexturedModelData.of((ModelData)PlayerEntityModel.getTexturedModelData((Dilation)Dilation.NONE, (boolean)slim), (int)64, (int)64).createModel(), slim), 0.5f);
            this.itemModelManager = context.getItemModelManager();
        }

        public HumanState createRenderState() {
            return new HumanState();
        }

        public Identifier getTexture(HumanState state) {
            return state.texture;
        }

        public void updateRenderState(VillagerEntity entity, HumanState state, float progress) {
            state.id = entity.getId();
            super.updateRenderState(entity, state, progress);
            BipedEntityRenderer.updateBipedRenderState((LivingEntity)entity, (BipedEntityRenderState)state, (float)progress, (ItemModelManager)this.itemModelManager);
            state.hatVisible = true;
            state.jacketVisible = true;
            state.leftPantsLegVisible = true;
            state.rightPantsLegVisible = true;
            state.leftSleeveVisible = true;
            state.rightSleeveVisible = true;
            state.spectator = false;
        }

        protected void scale(HumanState state, MatrixStack matrices) {
            float scale = 0.9375f * state.ageScale;
            matrices.scale(scale, scale, scale);
        }
    }

    public static final class State
    extends VillagerEntityRenderState {
        private final SkinnedVillagerRenderer renderer;
        final HumanState human = new HumanState();
        boolean custom;
        boolean slim;

        State(SkinnedVillagerRenderer renderer) {
            this.renderer = renderer;
        }

        public SkinnedVillagerRenderer renderer() {
            return this.renderer;
        }
    }

    public static final class HumanState
    extends PlayerEntityRenderState {
        Identifier texture;

        void setSkin(final Identifier texture, boolean slim) {
            PlayerSkinType type;
            PlayerSkinType resolved = type = slim ? PlayerSkinType.SLIM : PlayerSkinType.WIDE;
            if (texture.equals((Object)this.texture) && this.skinTextures != null && this.skinTextures.model() == type) {
                return;
            }
            this.texture = texture;
            net.minecraft.util.AssetInfo.TextureAsset asset = new net.minecraft.util.AssetInfo.TextureAsset(){

                public Identifier id() {
                    return texture;
                }

                public Identifier texturePath() {
                    return texture;
                }
            };
            this.skinTextures = new SkinTextures(asset, null, null, type, false);
        }
    }
}

