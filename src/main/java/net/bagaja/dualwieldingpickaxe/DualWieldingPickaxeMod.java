package net.bagaja.dualwieldingpickaxe;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod("dualwieldingpickaxe")
public class DualWieldingPickaxeMod {
    public DualWieldingPickaxeMod() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        ItemStack offhandItem = player.getOffhandItem();

        if (!offhandItem.isEmpty() && offhandItem.getItem() instanceof PickaxeItem) {
            handleOffhandMining(player, offhandItem, event);
        }
    }

    @SubscribeEvent
    public void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        ItemStack offhandItem = player.getOffhandItem();

        if (!offhandItem.isEmpty() && offhandItem.getItem() instanceof SwordItem) {
            handleOffhandAttack(player, offhandItem, event);
        }
    }

    private void handleOffhandMining(Player player, ItemStack offhandItem, PlayerInteractEvent.LeftClickBlock event) {
        BlockState state = event.getLevel().getBlockState(event.getPos());
        float destroySpeed = offhandItem.getDestroySpeed(state);

        if (destroySpeed > 1.0F) {
            // Cancel the main hand event
            event.setCanceled(true);

            // Break the block with offhand tool
            event.getLevel().destroyBlock(event.getPos(), true, player);

            // Damage the offhand item
            offhandItem.hurtAndBreak(1, player, EquipmentSlot.OFFHAND);

            // Play the breaking animation (only for offhand)
            player.swing(InteractionHand.OFF_HAND);
        }
    }

    private void handleOffhandAttack(Player player, ItemStack offhandItem, AttackEntityEvent event) {
        // Cancel the main hand attack
        event.setCanceled(true);

        // Apply damage using the offhand weapon
        float damage = (float) player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getValue();
        event.getTarget().hurt(player.damageSources().playerAttack(player), damage);

        // Damage the offhand item
        offhandItem.hurtAndBreak(1, player, EquipmentSlot.OFFHAND);

        // Play the attack animation (only for offhand)
        player.swing(InteractionHand.OFF_HAND);
    }
}