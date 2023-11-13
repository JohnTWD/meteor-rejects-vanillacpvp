/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * It is also modified by me, FatMC#1209
 * Copyright (c) Meteor Development.
 */

package anticope.rejects.modules;


import anticope.rejects.MeteorRejectsAddon;
import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.*;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.function.Predicate;

public class TweakedAutoTool extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgWhitelist = settings.createGroup("Whitelist");

    // General

    private final Setting<EnchantPreference> prefer = sgGeneral.add(new EnumSetting.Builder<EnchantPreference>()
            .name("prefer")
            .description("Either to prefer Silk Touch, Fortune, or none.")
            .defaultValue(EnchantPreference.Fortune)
            .build()
    );

    private final Setting<Boolean> silkTouchForEnderChest = sgGeneral.add(new BoolSetting.Builder()
            .name("silk-touch-for-ender-chest")
            .description("Mines Ender Chests only with the Silk Touch enchantment.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> fortuneForOresCrops = sgGeneral.add(new BoolSetting.Builder()
            .name("fortune-for-ores-and-crops")
            .description("Mines Ores and crops only with the Fortune enchantment.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> antiBreak = sgGeneral.add(new BoolSetting.Builder()
            .name("anti-break")
            .description("Stops you from breaking your tool.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Integer> breakDurability = sgGeneral.add(new IntSetting.Builder()
            .name("anti-break-percentage")
            .description("The durability percentage to stop using a tool.")
            .defaultValue(10)
            .range(1, 100)
            .sliderRange(1, 100)
            .visible(antiBreak::get)
            .build()
    );

    private final Setting<Boolean>  swapCrystalOnBedcockAndObby = sgGeneral.add(new BoolSetting.Builder()
            .name("crystalSwap")
            .description("Swap to crystals when trying to place on obby or bedrock")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean>  swapToWeapon = sgGeneral.add(new BoolSetting.Builder()
            .name("weaponSwaps")
            .description("Swap to a weapon when attacking someone")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean>  swapBackFromWeapon = sgGeneral.add(new BoolSetting.Builder()
            .name("weaponSwapsBack")
            .description("Back to orig slot after hitting someone!")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> switchBack = sgGeneral.add(new BoolSetting.Builder()
            .name("switch-back")
            .description("Switches your hand to whatever was selected when releasing your attack key.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Integer> switchDelay = sgGeneral.add((new IntSetting.Builder()
            .name("switch-delay")
            .description("Delay in ticks before switching tools.")
            .defaultValue(0)
            .build()
    ));

    // Whitelist and blacklist

    private final Setting<ListMode> listMode = sgWhitelist.add(new EnumSetting.Builder<ListMode>()
            .name("list-mode")
            .description("Selection mode.")
            .defaultValue(ListMode.Blacklist)
            .build()
    );

    private final Setting<List<Item>> whitelist = sgWhitelist.add(new ItemListSetting.Builder()
            .name("whitelist")
            .description("The tools you want to use.")
            .visible(() -> listMode.get() == ListMode.Whitelist)
            // .filter(AutoTool::isTool)
            .build()
    );

    private final Setting<List<Item>> blacklist = sgWhitelist.add(new ItemListSetting.Builder()
            .name("blacklist")
            .description("The tools you don't want to use.")
            .visible(() -> listMode.get() == ListMode.Blacklist)
            // .filter(AutoTool::isTool)
            .build()
    );

    private boolean wasPressed;
    private boolean shouldSwitch;
    private int ticks;
    private int bestSlot;


    public TweakedAutoTool() {
    super(MeteorRejectsAddon.CATEGORY, "tweakedtoolauto", "Automatically switches to the most effective tool when performing an action. (Now includes crystals)");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        // if (Modules.get().isActive(InfinityMiner.class)) return;

        if (switchBack.get() && !mc.options.attackKey.isPressed() && wasPressed && InvUtils.previousSlot != -1) {
            InvUtils.swapBack();
            wasPressed = false;
            return;
        }

        if (swapCrystalOnBedcockAndObby.get()) {
            ItemStack mainHand = mc.player.getMainHandStack();
            if (mainHand == null) return;

            boolean isECinHand = mainHand.getItem() == Items.END_CRYSTAL;
            boolean isProhibitedInHand = mainHand.getItem() == Items.TOTEM_OF_UNDYING|| mainHand.getItem() == Items.BOW || mainHand.getItem() == Items.CROSSBOW;
            boolean isBlockInHand = mainHand.getItem() instanceof BlockItem;

            if (!isBlockInHand && !isProhibitedInHand && mc.options.useKey.isPressed() && mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.BLOCK) {
                BlockPos pos = ((BlockHitResult) mc.crosshairTarget).getBlockPos();
                BlockState state = mc.world.getBlockState(pos);

                if ((state.getBlock() == Blocks.OBSIDIAN) || (state.getBlock() == Blocks.BEDROCK)) { // switch to crystal
                    FindItemResult result = InvUtils.find(Items.END_CRYSTAL);
                    if (!isECinHand && result.isHotbar()) {
                        boolean wasHeld = result.isMainHand();

                        if (!wasHeld)
                            InvUtils.swap(result.slot(), false);
                    }
                    mc.options.useKey.setPressed(true);
                }
            }
        }

        if (ticks <= 0 && shouldSwitch && bestSlot != -1) {
            InvUtils.swap(bestSlot, switchBack.get());
            shouldSwitch = false;
        } else {
            ticks--;
        }

        wasPressed = mc.options.attackKey.isPressed();
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onAttackEntity(AttackEntityEvent event) {
        if (!swapToWeapon.get() || mc.targetedEntity == null) return;

        if (event.entity instanceof EndCrystalEntity) return;
        // Find the highest damaging object
        int bigDmg = -1;
        int bestWep = -1;

        for (int i = 0; i < 9; i++) {
            ItemStack itemStack = mc.player.getInventory().getStack(i);

            if (isTool(itemStack)) {
                int damage = itemStack.getDamage();

                if (damage < 0) continue;

                if (damage > bigDmg) {
                    bigDmg = damage;
                    bestWep = i;
                }
            }
        }

        if (bestWep != -1) {
            InvUtils.swap(bestWep, swapBackFromWeapon.get());
            mc.player.attack(mc.targetedEntity);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onStartBreakingBlock(StartBreakingBlockEvent event) {
        // if (Modules.get().isActive(InfinityMiner.class)) return;

        // Get blockState
        BlockState blockState = mc.world.getBlockState(event.blockPos);
        if (!BlockUtils.canBreak(event.blockPos, blockState)) return;

        // Check if we should switch to a better tool
        ItemStack currentStack = mc.player.getMainHandStack();

        double bestScore = -1;
        bestSlot = -1;

        for (int i = 0; i < 9; i++) {
            ItemStack itemStack = mc.player.getInventory().getStack(i);

            if (listMode.get() == ListMode.Whitelist && !whitelist.get().contains(itemStack.getItem())) continue;
            if (listMode.get() == ListMode.Blacklist && blacklist.get().contains(itemStack.getItem())) continue;

            double score = getScore(itemStack, blockState, silkTouchForEnderChest.get(), fortuneForOresCrops.get(), prefer.get(), itemStack2 -> !shouldStopUsing(itemStack2));
            if (score < 0) continue;

            if (score > bestScore) {
                bestScore = score;
                bestSlot = i;
            }
        }

        if ((bestSlot != -1 && (bestScore > getScore(currentStack, blockState, silkTouchForEnderChest.get(), fortuneForOresCrops.get(), prefer.get(), itemStack -> !shouldStopUsing(itemStack))) || shouldStopUsing(currentStack) || !isTool(currentStack))) {
            ticks = switchDelay.get();

            if (ticks == 0) InvUtils.swap(bestSlot, true);
            else shouldSwitch = true;
        }

        // Anti break
        currentStack = mc.player.getMainHandStack();

        if (shouldStopUsing(currentStack) && isTool(currentStack)) {
            mc.options.attackKey.setPressed(false);
            event.cancel();
        }
    }

    private boolean shouldStopUsing(ItemStack itemStack) {
        return antiBreak.get() && (itemStack.getMaxDamage() - itemStack.getDamage()) < (itemStack.getMaxDamage() * breakDurability.get() / 100);
    }

    public static double getScore(ItemStack itemStack, BlockState state, boolean silkTouchEnderChest, boolean fortuneOre, EnchantPreference enchantPreference, Predicate<ItemStack> good) {
        if (!good.test(itemStack) || !isTool(itemStack)) return -1;
        if (!itemStack.isSuitableFor(state) && !(itemStack.getItem() instanceof SwordItem && (state.getBlock() instanceof BambooBlock || state.getBlock() instanceof BambooSaplingBlock))) return -1;

        if (silkTouchEnderChest
                && state.getBlock() == Blocks.ENDER_CHEST
                && EnchantmentHelper.getLevel(Enchantments.SILK_TOUCH, itemStack) == 0) {
            return -1;
        }

        if (fortuneOre
                && isFortunable(state.getBlock())
                && EnchantmentHelper.getLevel(Enchantments.FORTUNE, itemStack) == 0) {
            return -1;
        }

        double score = 0;

        score += itemStack.getMiningSpeedMultiplier(state) * 1000;
        score += EnchantmentHelper.getLevel(Enchantments.UNBREAKING, itemStack);
        score += EnchantmentHelper.getLevel(Enchantments.EFFICIENCY, itemStack);
        score += EnchantmentHelper.getLevel(Enchantments.MENDING, itemStack);

        if (enchantPreference == EnchantPreference.Fortune) score += EnchantmentHelper.getLevel(Enchantments.FORTUNE, itemStack);
        if (enchantPreference == EnchantPreference.SilkTouch) score += EnchantmentHelper.getLevel(Enchantments.SILK_TOUCH, itemStack);

        if (itemStack.getItem() instanceof SwordItem item && (state.getBlock() instanceof BambooBlock || state.getBlock() instanceof BambooSaplingBlock))
            score += 9000 + (item.getMaterial().getMiningLevel() * 1000);


        return score;
    }

    public static boolean isTool(Item item) {
        return item instanceof ToolItem || item instanceof ShearsItem || item instanceof SwordItem;
    }
    public static boolean isTool(ItemStack itemStack) {
        return isTool(itemStack.getItem());
    }

    private static boolean isFortunable(Block block) {
        if (block == Blocks.ANCIENT_DEBRIS) return false;
        return /*Xray.ORES.contains(block) ||*/ block instanceof CropBlock;
    }

    public enum EnchantPreference {
        None,
        Fortune,
        SilkTouch
    }

    public enum ListMode {
        Whitelist,
        Blacklist
    }
}