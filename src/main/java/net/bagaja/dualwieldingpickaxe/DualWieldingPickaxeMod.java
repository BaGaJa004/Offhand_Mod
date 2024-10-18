package net.bagaja.dualwieldingpickaxe;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

@Mod("dualwieldingpickaxe")
public class DualWieldingPickaxeMod {
    private final Map<BlockPos, Float> miningProgress = new HashMap<>();
    private final Map<Player, BlockPos> lastMinedBlock = new HashMap<>();

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
        BlockPos pos = event.getPos();
        BlockState state = event.getLevel().getBlockState(pos);
        float destroySpeed = offhandItem.getDestroySpeed(state);

        if (destroySpeed > 1.0F) {
            // Cancel the main hand event
            event.setCanceled(true);

            // Check if player started mining a different block
            BlockPos lastPos = lastMinedBlock.get(player);
            if (lastPos == null || !lastPos.equals(pos)) {
                miningProgress.remove(lastPos);
                lastMinedBlock.put(player, pos);
            }

            // Calculate mining speed more accurately
            float hardness = state.getDestroySpeed(event.getLevel(), pos);
            float speedMultiplier = destroySpeed / hardness;

            // Factor in the proper mining speed calculation
            boolean canHarvest = ((TieredItem)offhandItem.getItem()).getTier().getEnchantmentValue() >= state.getBlock().getExplosionResistance();
            float miningSpeedMod = canHarvest ? speedMultiplier : 1.0f;

            // Get current progress or start from 0
            float progress = miningProgress.getOrDefault(pos, 0f);

            // Add mining progress with adjusted speed
            progress += (miningSpeedMod / 30f) * (canHarvest ? 1.0f : 0.3f);
            miningProgress.put(pos, progress);

            // Break the block if mining is complete
            if (progress >= 1.0f) {
                // Only break if we can actually harvest it
                if (canHarvest) {
                    event.getLevel().destroyBlock(pos, true, player);
                    offhandItem.hurtAndBreak(1, player, EquipmentSlot.OFFHAND);
                }
                miningProgress.remove(pos);
                lastMinedBlock.remove(player);
            }

            // Play the mining animation
            player.swing(InteractionHand.OFF_HAND);

            // Update block breaking animation
            int stage = (int) (progress * 10.0F);
            event.getLevel().destroyBlockProgress(player.getId(), pos, stage);
        }
    }

    private void handleOffhandAttack(Player player, ItemStack offhandItem, AttackEntityEvent event) {
        if (!(offhandItem.getItem() instanceof SwordItem swordItem)) {
            return;
        }

        // Cancel the main hand attack
        event.setCanceled(true);

        // Calculate proper sword damage
        float baseDamage = ((TieredItem)swordItem).getTier().getAttackDamageBonus() + 2.0f; // Base damage of sword + base attack damage
        float attackStrength = player.getAttackStrengthScale(0.5F);

        // Get the proper damage calculation including critical hits
        float damage = baseDamage * (0.2F + attackStrength * attackStrength * 0.8F);

        // Apply the damage
        event.getTarget().hurt(player.damageSources().playerAttack(player), damage);

        // Damage the sword
        offhandItem.hurtAndBreak(1, player, EquipmentSlot.OFFHAND);

        // Play the attack animation
        player.swing(InteractionHand.OFF_HAND);
    }
}