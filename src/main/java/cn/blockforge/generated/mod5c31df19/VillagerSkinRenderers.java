package cn.blockforge.generated.mod5c31df19;

import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;

/** 只接管已选有效皮肤的村民；其余情况保留游戏/其他模组的原渲染器。 */
public final class VillagerSkinRenderers {
    private static SkinnedVillagerRenderer renderer;

    private VillagerSkinRenderers() { }

    public static void reload(EntityRendererFactory.Context context) {
        // 和游戏渲染器使用同一份已加载资源，F3+T 后重新创建模型。
        renderer = new SkinnedVillagerRenderer(context);
    }

    public static EntityRenderer<?, ?> forEntity(Entity entity) {
        // MCA 男女村民也继承 VillagerEntity，但并不使用 EntityType.VILLAGER。
        // 只做客户端渲染选择，不替换实体，不修改其职业、家庭或互动数据。
        if (renderer == null || !(entity instanceof VillagerEntity villager)) return null;
        SkinAssignments.Choice choice = VillagerSkinsClient.choiceFor(villager);
        if (choice == null || VillagerSkinsClient.library.get(choice.file()) == null) return null;
        return renderer;
    }

    public static EntityRenderer<?, ?> forState(EntityRenderState state) {
        // 1.21.11 会按状态再次选渲染器。必须跟随产生状态的实例，不能按
        // state.entityType 跳回 MCA 渲染器，也不能把 HumanState 当真人玩家状态。
        // 不在这里再次读取当前皮肤选择，以免预览切换后错配已提取的状态。
        return state instanceof SkinnedVillagerRenderer.State custom ? custom.renderer() : null;
    }
}
