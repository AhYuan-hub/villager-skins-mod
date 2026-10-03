package villageskins;

import villageskins.SkinAssignments;
import villageskins.SkinnedVillagerRenderer;
import villageskins.VillagerSkinsClient;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.EntityRenderer;

public final class VillagerSkinRenderers {
    private static SkinnedVillagerRenderer renderer;

    private VillagerSkinRenderers() {
    }

    public static void reload(net.minecraft.client.render.entity.EntityRendererFactory.Context context) {
        renderer = new SkinnedVillagerRenderer(context);
    }

    public static EntityRenderer<?, ?> forEntity(Entity entity) {
        if (renderer == null || !(entity instanceof VillagerEntity)) {
            return null;
        }
        VillagerEntity villager = (VillagerEntity)entity;
        SkinAssignments.Choice choice = VillagerSkinsClient.choiceFor(villager);
        if (choice == null || VillagerSkinsClient.library.get(choice.file()) == null) {
            return null;
        }
        return renderer;
    }

    public static EntityRenderer<?, ?> forState(EntityRenderState state) {
        SkinnedVillagerRenderer skinnedVillagerRenderer;
        if (state instanceof SkinnedVillagerRenderer.State) {
            SkinnedVillagerRenderer.State custom = (SkinnedVillagerRenderer.State)state;
            skinnedVillagerRenderer = custom.renderer();
        } else {
            skinnedVillagerRenderer = null;
        }
        return skinnedVillagerRenderer;
    }
}

