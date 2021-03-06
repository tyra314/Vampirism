package de.teamlapen.vampirism.tests;

import de.teamlapen.vampirism.VampirismMod;
import de.teamlapen.vampirism.blocks.BlockCastleBlock;
import de.teamlapen.vampirism.blocks.BlockWeaponTable;
import de.teamlapen.vampirism.core.ModBlocks;
import de.teamlapen.vampirism.core.ModFluids;
import de.teamlapen.vampirism.core.ModItems;
import de.teamlapen.vampirism.fluids.BloodHelper;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.GameType;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;

/**
 * Since I'm not familiar with JUnit or similar and it does not work that well with Minecraft anyway, this is a some kind of ingame test which is executed via command
 *
 * Usage of lambda and stuff is probably unnecessary and stuff, but fun.
 */
public class Tests {

    public static void runTests(World world, EntityPlayer player) {
        sendMsg(player, "Starting tests");
        log("Clearing area");
        clearArea(world);
        boolean wasCreative = player.isCreative();
        player.setGameType(GameType.SURVIVAL);
        player.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, 40, 100));
        player.attemptTeleport(0, 5, 0);
        TestInfo info = new TestInfo(world, player, new BlockPos(-20, 2, -20), "BloodFluidHandler");

        runTest(Tests::bloodFluidHandler, info);
        runTest(Tests::blockWeaponTableFluids, info.next("BlockWeaponTableFluids"));

        log("Finished tests -> teleporting player");
        player.attemptTeleport(0, 5, 0);
        if (wasCreative) player.setGameType(GameType.CREATIVE);
        sendMsg(player, "Finished tests");
    }

    private static void runTest(Tester tester, TestInfo info) {
        boolean result;
        try {
            result = tester.run(info);
        } catch (Throwable t) {
            log(info.name + " failed with exception %s", t);
            result = false;
        }
        sendMsg(info.player, info.name + " test " + (result ? "§2was successful§r" : "§4failed§r"));
    }

    private static boolean bloodFluidHandler(TestInfo info) throws Throwable {
        info.world.setBlockState(info.pos, ModBlocks.bloodContainer.getDefaultState());
        TileEntity t = info.world.getTileEntity(info.pos);
        IFluidHandler handler = t.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, EnumFacing.random(info.world.rand));
        handler.fill(new FluidStack(ModFluids.blood, 10000000), true);
        int blood = BloodHelper.getBlood(handler);
        assert blood > 0 : "Could not fill blood container";

        ItemStack bloodBottle1 = new ItemStack(ModItems.bloodBottle);
        ItemStack bloodBottle2 = new ItemStack(ModItems.bloodBottle);
        FluidActionResult result1 = FluidUtil.tryFillContainer(bloodBottle1, handler, Integer.MAX_VALUE, null, true);
        assert result1.isSuccess() : "Transaction 1 failed";
        bloodBottle1 = result1.getResult();
        FluidActionResult result2 = FluidUtil.tryFillContainer(bloodBottle2, handler, Integer.MAX_VALUE, null, true);
        assert result2.isSuccess() : "Transaction 2 failed";
        bloodBottle2 = result2.getResult();
        assert BloodHelper.getBlood(handler) < blood : "Failed to drain from container into bottles";
        FluidActionResult result3 = FluidUtil.tryEmptyContainer(bloodBottle1, handler, Integer.MAX_VALUE, null, true);
        assert result3.isSuccess() : "Transaction 3 failed";
        bloodBottle1 = result3.getResult();
        FluidActionResult result4 = FluidUtil.tryEmptyContainer(bloodBottle2, handler, Integer.MAX_VALUE, null, true);
        assert result4.isSuccess() : "Transaction 4 failed";
        bloodBottle2 = result4.getResult();
        log("%d %d", BloodHelper.getBlood(handler), blood);
        assert BloodHelper.getBlood(handler) == blood : "Lost blood somewhere";
        return true;

    }

    private static boolean blockWeaponTableFluids(TestInfo info) throws Throwable {
        info.world.setBlockState(info.pos, ModBlocks.weaponTable.getDefaultState());
        info.player.setHeldItem(info.player.getActiveHand(), new ItemStack(Items.LAVA_BUCKET));
        IBlockState block = info.world.getBlockState(info.pos);
        block.getBlock().onBlockActivated(info.world, info.pos, block, info.player, info.player.getActiveHand(), EnumFacing.random(info.world.rand), 0, 0, 0);
        block = info.world.getBlockState(info.pos);
        assert info.player.getHeldItem(info.player.getActiveHand()).getItem().equals(Items.BUCKET) : "Incorrect Fluid Container Handling";
        log("Block lava level: %s", block.getValue(BlockWeaponTable.LAVA));
        assert (block.getValue(BlockWeaponTable.LAVA) * BlockWeaponTable.MB_PER_META) == Fluid.BUCKET_VOLUME : "Incorrect Fluid Transaction";
        return true;
    }

    private static void log(String msg, Object... format) {
        VampirismMod.log.w("TEST", msg, format);
    }

    private static void sendMsg(EntityPlayer player, String msg) {
        player.sendMessage(new TextComponentString("§1[V-TEST]§r " + msg));
    }

    private static void clearArea(World world) {
        for (int x = -21; x < 22; x++) {
            for (int y = 1; y < 22; y++) {
                for (int z = -21; z < 22; z++) {
                    IBlockState s = (y == 1 || x == -21 || x == 21 || z == -21 || z == 21 || y == 21) ? ModBlocks.castleBlock.getDefaultState().withProperty(BlockCastleBlock.VARIANT, BlockCastleBlock.EnumType.DARK_STONE) : Blocks.AIR.getDefaultState();
                    world.setBlockState(new BlockPos(x, y, z), s);
                }
            }
        }
    }

    @FunctionalInterface
    private interface Tester {
        /**
         * Runs the given test
         *
         * @param t the function argument
         * @return the function result
         */
        Boolean run(TestInfo t) throws Throwable;
    }

    private static class TestInfo {
        final World world;
        final EntityPlayer player;
        BlockPos pos;
        String name;

        private TestInfo(World world, EntityPlayer player, BlockPos pos, String name) {
            this.world = world;
            this.player = player;
            this.pos = pos;
            this.name = name;
        }


        private TestInfo next(String name) {
            int x = pos.getX();
            int z = pos.getZ();
            x += 5;
            if (x > 20) {
                x = -20;
                z += 5;
                if (z > 20) {
                    throw new IllegalStateException("Not enough room -> Too many tests");
                }
            }
            this.pos = new BlockPos(x, pos.getY(), z);
            this.name = name;
            return this;
        }

    }
}
