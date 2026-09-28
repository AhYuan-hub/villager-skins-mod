package cn.blockforge.generated.mod5c31df19.mixin;

import cn.blockforge.generated.mod5c31df19.VillagerSkinRenderers;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactories;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

/** 在原生资源重载阶段建立换肤渲染器，不争抢任何实体类型的注册。 */
@Mixin(EntityRendererFactories.class)
public abstract class EntityRendererFactoriesMixin {
    @Inject(method = "reloadEntityRenderers", at = @At("RETURN"))
    private static void villagerSkins$reload(EntityRendererFactory.Context context,
            CallbackInfoReturnable<Map<EntityType<?>, EntityRenderer<?, ?>>> cir) {
        VillagerSkinRenderers.reload(context);
    }
}
