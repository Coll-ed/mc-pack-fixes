package com.yglcs.stuckarrow;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;

/**
 * 在实体渲染之后，按「真实命中偏移 + 弹道朝向」画假箭。
 *
 * <p>挂在 {@code RenderLivingEvent.Post}：此时 PoseStack 已处于**实体局部坐标系**
 * （原点在实体脚下、已按身体朝向旋转），我们 push/translate/rotate 的变换会如实生效 ——
 * 这正好补上 AITK 那一层无法被外部影响的问题。
 */
public final class StuckArrowRenderHandler {

   /** 复用的模板箭（只取模型与贴图，不加入世界）。 */
   private static Arrow template;

   private StuckArrowRenderHandler() {
   }

   @SubscribeEvent
   public static void onRenderLiving(RenderLivingEvent.Post<LivingEntity, ?> event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null) {
         return;
      }
      LivingEntity entity = event.getEntity();
      List<StuckArrows.Stuck> arrows = StuckArrows.get(entity.getId(), mc.level.getGameTime());
      if (arrows.isEmpty()) {
         return;
      }
      Arrow fake = template(mc);
      if (fake == null) {
         return;
      }
      PoseStack pose = event.getPoseStack();
      MultiBufferSource buffer = event.getMultiBufferSource();
      int light = event.getPackedLight();
      float partialTick = event.getPartialTick();

      for (StuckArrows.Stuck stuck : arrows) {
         Vec3 offset = stuck.offset();
         pose.pushPose();
         pose.translate(offset.x, offset.y, offset.z);
         pose.mulPose(Axis.YP.rotationDegrees(stuck.yaw()));
         pose.mulPose(Axis.XP.rotationDegrees(stuck.pitch()));
         mc.getEntityRenderDispatcher().render(
            fake, 0.0D, 0.0D, 0.0D, 0.0F, partialTick, pose, buffer, light);
         pose.popPose();
      }
   }

   private static Arrow template(Minecraft mc) {
      if (template == null && mc.level != null) {
         template = new Arrow(mc.level, 0.0D, 0.0D, 0.0D,
            new ItemStack(Items.ARROW), ItemStack.EMPTY);
      }
      return template;
   }
}
