package cn.blockforge.generated.mod5c31df19.mixin;

import cn.blockforge.generated.mod5c31df19.VillagerSkinRenderers;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 同时覆盖实体取状态与状态提交两处入口，让世界渲染和界面预览保持一致。 */
@Mixin(EntityRenderManager.class)
public abstract class EntityRenderManagerMixin {
    @Inject(method = "getRenderer(Lnet/minecraft/entity/Entity;)Lnet/minecraft/client/render/entity/EntityRenderer;",
            at = @At("HEAD"), cancellable = true)
    private void villagerSkins$forEntity(Entity entity,
            CallbackInfoReturnable<EntityRenderer<?, ?>> cir) {
        EntityRenderer<?, ?> renderer = VillagerSkinRenderers.forEntity(entity);
        if (renderer != null) cir.setReturnValue(renderer);
    }

    @Inject(method = "getRenderer(Lnet/minecraft/client/render/entity/state/EntityRenderState;)Lnet/minecraft/client/render/entity/EntityRenderer;",
            at = @At("HEAD"), cancellable = true)
    private void villagerSkins$forState(EntityRenderState state,
            CallbackInfoReturnable<EntityRenderer<?, ?>> cir) {
        EntityRenderer<?, ?> renderer = VillagerSkinRenderers.forState(state);
        if (renderer != null) cir.setReturnValue(renderer);
    }
}
