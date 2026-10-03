package villageskins.mixin;

import villageskins.VillagerSkinRenderers;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRenderManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={EntityRenderManager.class})
public abstract class EntityRenderManagerMixin {
    @Inject(method={"getRenderer(Lnet/minecraft/entity/Entity;)Lnet/minecraft/client/render/entity/EntityRenderer;"}, at={@At(value="HEAD")}, cancellable=true)
    private void villagerSkins$forEntity(Entity entity, CallbackInfoReturnable<EntityRenderer<?, ?>> cir) {
        EntityRenderer<?, ?> renderer = VillagerSkinRenderers.forEntity(entity);
        if (renderer != null) {
            cir.setReturnValue(renderer);
        }
    }

    @Inject(method={"getRenderer(Lnet/minecraft/client/render/entity/state/EntityRenderState;)Lnet/minecraft/client/render/entity/EntityRenderer;"}, at={@At(value="HEAD")}, cancellable=true)
    private void villagerSkins$forState(EntityRenderState state, CallbackInfoReturnable<EntityRenderer<?, ?>> cir) {
        EntityRenderer<?, ?> renderer = VillagerSkinRenderers.forState(state);
        if (renderer != null) {
            cir.setReturnValue(renderer);
        }
    }
}

