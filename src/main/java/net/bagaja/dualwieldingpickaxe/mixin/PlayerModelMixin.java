package net.bagaja.dualwieldingpickaxe.mixin;

import net.bagaja.dualwieldingpickaxe.DualWieldingPickaxeMod;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin<T extends LivingEntity> extends HumanoidModel<T> {

    public PlayerModelMixin(ModelPart root) {
        super(root);
    }

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void onSetupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (entity instanceof AbstractClientPlayer player) {
            if (DualWieldingPickaxeMod.isOffhandMining.getOrDefault(player, false)) {
                // Cancel main hand animation
                this.rightArm.xRot = 0;
                this.rightArm.yRot = 0;
                this.rightArm.zRot = 0;

                // Apply mining animation to left arm
                this.leftArm.xRot = (float) Math.cos(ageInTicks * 0.6662F) * 0.8F;
                this.leftArm.yRot = 0;
                this.leftArm.zRot = 0;
            }
        }
    }
}