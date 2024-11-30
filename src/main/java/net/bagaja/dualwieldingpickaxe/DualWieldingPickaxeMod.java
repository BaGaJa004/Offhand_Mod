package net.bagaja.dualwieldingpickaxe;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap; import java.util.Map;

import static net.minecraft.world.item.Tiers.*;

@Mod("dualwieldingpickaxe")
public class DualWieldingPickaxeMod {
    private final Map<BlockPos, Float> miningProgress = new HashMap<>();
    private final Map<Player, BlockPos> lastMinedBlock = new HashMap<>();
    public static final Map<Player, Boolean> isOffhandMining = new HashMap<>();

    public DualWieldingPickaxeMod() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            Player player = event.player;
            BlockPos lastPos = lastMinedBlock.get(player);

            // Clear mining animation if player stopped mining
            if (!isOffhandMining.getOrDefault(player, false) && lastPos != null) {
                event.player.level().destroyBlockProgress(player.getId(), lastPos, -1);
                lastMinedBlock.remove(player);
                miningProgress.remove(lastPos);
            }
        }
    }

    @SubscribeEvent
    public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        ItemStack mainhandItem = player.getMainHandItem();
        ItemStack offhandItem = player.getOffhandItem();

        // Only process if offhand has pickaxe and mainhand doesn't
        if (!offhandItem.isEmpty() && offhandItem.getItem() instanceof PickaxeItem &&
                !(mainhandItem.getItem() instanceof PickaxeItem)) {
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

        // Cancel vanilla mining animation and block breaking
        event.setCanceled(true);

        // Stop mainhand animation
        player.stopUsingItem();

        Tier pickaxeTier = ((TieredItem)offhandItem.getItem()).getTier();
        boolean canHarvestBlock = offhandItem.isCorrectToolForDrops(state);  // Changed this line
        float destroySpeed = offhandItem.getDestroySpeed(state);

        if (destroySpeed > 1.0F) {
            // Set mining flag
            isOffhandMining.put(player, true);

            // Reset progress if mining a different block
            BlockPos lastPos = lastMinedBlock.get(player);
            if (lastPos == null || !lastPos.equals(pos)) {
                if (lastPos != null) {
                    event.getLevel().destroyBlockProgress(player.getId(), lastPos, -1);
                }
                miningProgress.remove(lastPos);
                lastMinedBlock.put(player, pos);
            }

            // Calculate mining speed
            float hardness = state.getDestroySpeed(event.getLevel(), pos);
            float speedMultiplier = destroySpeed / hardness;
            float tierSpeedModifier = getTierSpeedModifier(pickaxeTier);
            speedMultiplier *= tierSpeedModifier;

            // Update progress
            float progress = miningProgress.getOrDefault(pos, 0f);
            if (canHarvestBlock) {
                progress += (speedMultiplier / 30f);
            } else {
                progress += (speedMultiplier / 100f);
            }
            miningProgress.put(pos, progress);

            // Break block if complete
            if (progress >= 1.0f) {
                if (canHarvestBlock) {
                    // Use proper block breaking method
                    event.getLevel().destroyBlock(pos, true, player);
                    offhandItem.hurtAndBreak(1, player, EquipmentSlot.OFFHAND);
                }
                miningProgress.remove(pos);
                lastMinedBlock.remove(player);
                isOffhandMining.put(player, false);
                event.getLevel().destroyBlockProgress(player.getId(), pos, -1);
            } else {
                // Update block breaking animation
                int stage = Math.min(9, (int)(progress * 10.0F));
                event.getLevel().destroyBlockProgress(player.getId(), pos, stage);

                // Play mining animation for offhand
                player.swing(InteractionHand.OFF_HAND, true);
            }
        }
    }

    private float getTierSpeedModifier(Tier tier) {
        // Adjust these values to balance mining speeds
        return switch (tier) {
            case WOOD -> 1.0f;    // Slowest
            case STONE -> 1.0f;   // Slow
            case IRON -> 1.0f;    // Normal
            case DIAMOND -> 1.0f; // Fast
            case NETHERITE -> 1.0f; // Fastest
            default -> 1.0f;
        };
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