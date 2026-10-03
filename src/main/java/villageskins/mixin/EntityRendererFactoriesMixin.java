package villageskins.mixin;

import villageskins.VillagerSkinRenderers;
import java.util.Map;
import net.minecraft.entity.EntityType;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.EntityRendererFactories;
import net.minecraft.client.render.entity.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={EntityRendererFactories.class})
public abstract class EntityRendererFactoriesMixin {
    @Inject(method={"reloadEntityRenderers"}, at={@At(value="RETURN")})
    private static void villagerSkins$reload(net.minecraft.client.render.entity.EntityRendererFactory.Context context, CallbackInfoReturnable<Map<EntityType<?>, EntityRenderer<?, ?>>> cir) {
        VillagerSkinRenderers.reload(context);
    }
}

