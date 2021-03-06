package de.teamlapen.vampirism.tileentity;

import de.teamlapen.lib.VampLib;
import de.teamlapen.lib.lib.inventory.InventorySlot;
import de.teamlapen.lib.lib.tile.InventoryTileEntity;
import de.teamlapen.lib.lib.util.FluidLib;
import de.teamlapen.lib.lib.util.ItemStackUtil;
import de.teamlapen.lib.util.ISoundReference;
import de.teamlapen.vampirism.VampirismMod;
import de.teamlapen.vampirism.api.entity.player.hunter.IHunterPlayer;
import de.teamlapen.vampirism.api.entity.player.skills.ISkillHandler;
import de.teamlapen.vampirism.api.items.IAlchemicalCauldronRecipe;
import de.teamlapen.vampirism.blocks.BlockAlchemicalCauldron;
import de.teamlapen.vampirism.core.ModSounds;
import de.teamlapen.vampirism.inventory.AlchemicalCauldronCraftingManager;
import de.teamlapen.vampirism.player.hunter.HunterPlayer;
import de.teamlapen.vampirism.player.hunter.skills.HunterSkills;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * 1.10
 *
 * @author maxanier
 */
public class TileAlchemicalCauldron extends InventoryTileEntity implements ITickable, ISidedInventory {

    private static final int SLOT_RESULT = 0;
    private static final int SLOT_LIQUID = 1;
    private static final int SLOT_INGREDIENT = 2;
    private static final int SLOT_FUEL = 3;
    private static final int[] SLOTS_TOP = new int[]{SLOT_RESULT};
    private static final int[] SLOTS_WEST = new int[]{SLOT_LIQUID};
    private static final int[] SLOTS_EAST = new int[]{SLOT_INGREDIENT};
    private static final int[] SLOTS_BOTTOM = new int[]{SLOT_FUEL};

    /**
     * @return The liquid color of the given stack. -1 if not a (registered) liquid stack
     */
    private static int getLiquidColor(ItemStack s) {
        if (s != null) {
            if (FluidLib.hasFluidItemCap(s)) {
                IFluidHandler handler = FluidLib.getFluidItemCap(s);
                FluidStack fluid = handler.drain(10000, false);
                if (fluid != null) {

                    return getFluidColor(fluid);
                }
            }
        }
        return AlchemicalCauldronCraftingManager.getInstance().getLiquidColor(s);
    }

    /**
     * Retrieves the color of the given fluid stack.
     * Never returns -1 (0xFFFFFFFF)
     */
    private static int getFluidColor(FluidStack stack) {
        Fluid fluid = stack.getFluid();
        if (fluid.equals(FluidRegistry.WATER)) {
            return 0xC03040FF;
        } else if (fluid.equals(FluidRegistry.LAVA)) {
            return 0xFFFF5010;
        }
        int color = fluid.getColor(stack);
        if (color == 0xFFFFFFFF) {
            color = 0xFFFFFFFE; //0xFFFFFFFF == -1 which makes our isLiquidCheck fail
        }
        return color;
    }

    /**
     * @return f the given stack is a (registed) liquid stack
     */
    private static boolean isLiquidStack(ItemStack stack) {
        return getLiquidColor(stack) != -1;
    }

    private int totalBurnTime = 0, burnTime = 0, cookTime = 0, totalCookTime = 0;
    private boolean cookingClient;
    private boolean burningClient;
    private int liquidColor;
    @SideOnly(Side.CLIENT)
    private ISoundReference boilingSound;
    /**
     * Can contain a recipe which can be cooked by the owner.
     * Is set when a {@link IAlchemicalCauldronRecipe#canBeCooked(int, ISkillHandler)} check succeeds.
     * As long as the current recipe is equals to this one, no new check is executed.
     * Thereby the owner can e.g. disconnect and the cauldron can continue cooking (as long as the recipe does not change).
     */
    @Nullable
    private IAlchemicalCauldronRecipe checkedRecipe;
    /**
     * The username of the owner.
     */
    private String username;
    /**
     * The UUID of the owner. Null on client, can be null on server.
     */
    private UUID ownerID;

    public TileAlchemicalCauldron() {
        super(new InventorySlot[]{new InventorySlot(new InventorySlot.IItemSelector() {
            @Override
            public boolean isItemAllowed(@Nonnull ItemStack item) {
                return false;
            }
        }, 116, 35), new InventorySlot(new InventorySlot.IItemSelector() {

            @Override
            public boolean isItemAllowed(@Nonnull ItemStack item) {
                return isLiquidStack(item);
            }
        }, 44, 17), new InventorySlot(68, 17), new InventorySlot(new InventorySlot.IItemSelector() {
            @Override
            public boolean isItemAllowed(@Nonnull ItemStack item) {
                return TileEntityFurnace.isItemFuel(item);
            }
        }, 56, 53)});
    }

    @Override
    public boolean canExtractItem(int index, ItemStack stack, EnumFacing direction) {
        return true;
    }

    @Override
    public boolean canInsertItem(int index, ItemStack itemStackIn, EnumFacing direction) {
        return this.isItemValidForSlot(index, itemStackIn);
    }

    public boolean canUse(EntityPlayer player) {
        if (HunterPlayer.get(player).getSkillHandler().isSkillEnabled(HunterSkills.basic_alchemy)) {
            if (isOwner(player)) {
                return true;
            } else {
                player.sendMessage(new TextComponentTranslation("tile.vampirism.alchemical_cauldron.other", getOwnerName()));
                return false;
            }
        }
        player.sendMessage(new TextComponentTranslation("tile.vampirism.alchemical_cauldron.cannot_use", getOwnerName()));
        return false;
    }

    @Nonnull
    @Override
    public ITextComponent getDisplayName() {
        return new TextComponentTranslation("tile.vampirism.alchemical_cauldron.display", getOwnerName(), new TextComponentTranslation("tile.vampirism.alchemical_cauldron.name"));
    }

    @Override
    public int getField(int id) {
        switch (id) {
            case 0:
                return this.totalBurnTime;
            case 1:
                return this.burnTime;
            case 2:
                return this.totalCookTime;
            case 3:
                return this.cookTime;
            default:
                return 0;
        }
    }

    @Override
    public int getFieldCount() {
        return 4;
    }

    @SideOnly(Side.CLIENT)
    public int getLiquidColorClient() {
        return liquidColor;
    }

    @Nonnull
    @Override
    public String getName() {
        return "vampirism.container." + BlockAlchemicalCauldron.regName;
    }

    /**
     * @return The name of the owner or "Unknown" if not yet set or synced
     */
    public String getOwnerName() {
        if (username == null) {

            EntityPlayer player = getOwner();
            if (player != null) {
                username = player.getDisplayNameString();
            } else {
                return "Unknown";
            }
        }
        return username;
    }

    @Nonnull
    @Override
    public int[] getSlotsForFace(EnumFacing side) {
        switch (side) {
            case WEST:
                return SLOTS_WEST;
            case EAST:
                return SLOTS_EAST;
            case DOWN:
                return SLOTS_BOTTOM;
            case UP:
                return SLOTS_TOP;
            default:
                return new int[0];
        }
    }

    @Nullable
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(getPos(), 1, getUpdateTag());
    }

    @Nonnull
    @Override
    public NBTTagCompound getUpdateTag() {
        NBTTagCompound nbt = super.writeToNBT(new NBTTagCompound());
        nbt.setBoolean("cooking", cookTime > 0 && isBurning());
        nbt.setBoolean("burning", burnTime > 0);
        nbt.setString("username", getOwnerName());
        ItemStack liquidItem = getStackInSlot(SLOT_LIQUID);
        if (liquidItem != null) {
            nbt.setTag("liquidItem", liquidItem.writeToNBT(new NBTTagCompound()));
        }
        return nbt;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void handleUpdateTag(@Nonnull NBTTagCompound nbt) {
        super.handleUpdateTag(nbt);
        cookingClient = nbt.getBoolean("cooking");
        burningClient = nbt.getBoolean("burning");
        if (nbt.hasKey("liquidItem")) {
            this.setInventorySlotContents(SLOT_LIQUID, ItemStackUtil.loadFromNBT(nbt.getCompoundTag("liquidItem")));
        } else {
            this.setInventorySlotContents(SLOT_LIQUID, ItemStackUtil.getEmptyStack());
        }
        username = nbt.getString("username");
        liquidColor = getLiquidColor(getStackInSlot(SLOT_LIQUID));
        this.world.markBlockRangeForRenderUpdate(pos, pos);
    }

    /**
     * @return burnTime>0 on server and cached boolean on client
     */
    public boolean isBurning() {
        if (world.isRemote) {
            return burningClient;
        }
        return burnTime > 0;
    }

    /**
     * @return cookTime>0 on server and cached boolean on client
     */
    public boolean isCooking() {
        if (world.isRemote) {
            return cookingClient;
        }
        return cookTime > 0;
    }

    /**
     * @return If there is a liquid inside
     */
    public boolean isFilled() {
        if (world.isRemote) {
            if (liquidColor == -1) return false;
        }
        return isStackInSlot(SLOT_LIQUID);
    }

    /**
     * Checks if the given player is the owner.
     * If none has been set yet, the given one becomes the owner.
     */
    public boolean isOwner(EntityPlayer player) {
        if (ownerID != null) {
            return ownerID.equals(player.getPersistentID());
        } else {
            setOwner(player);
            return true;
        }
    }

    public void markDirty(boolean sync) {
        super.markDirty();
        if (sync) {
            IBlockState state = world.getBlockState(pos);
            this.world.notifyBlockUpdate(pos, state, state, 3);
        }
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        NBTTagCompound nbt = pkt.getNbtCompound();
        handleUpdateTag(nbt);

    }

    @Override
    public void readFromNBT(NBTTagCompound tagCompound) {
        super.readFromNBT(tagCompound);
        if (tagCompound.hasKey("burntime")) {
            this.burnTime = tagCompound.getInteger("burntime");
            this.cookTime = tagCompound.getInteger("cooktime");
            this.totalBurnTime = tagCompound.getInteger("cooktime_total");
        }
        if (tagCompound.hasUniqueId("owner")) {
            ownerID = tagCompound.getUniqueId("owner");
        }
        if (tagCompound.hasKey("ownername")) {
            username = tagCompound.getString("ownername");
        }
        if (tagCompound.hasKey("bypass_recipecheck")) {
            ItemStack liquid = getStackInSlot(SLOT_LIQUID);
            ItemStack ingredient = getStackInSlot(SLOT_INGREDIENT);
            if (!ItemStackUtil.isEmpty(liquid)) {
                checkedRecipe = AlchemicalCauldronCraftingManager.getInstance().findRecipe(liquid, ingredient);

            }
        }
    }

    @Override
    public void setField(int id, int value) {
        switch (id) {
            case 0:
                this.totalBurnTime = value;
                break;
            case 1:
                this.burnTime = value;
                break;
            case 2:
                this.totalCookTime = value;
                break;
            case 3:
                this.cookTime = value;
                break;
        }
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        assert ItemStackUtil.isValid(stack);
        super.setInventorySlotContents(slot, stack);
        if (slot == SLOT_LIQUID && world instanceof WorldServer) {
            ((WorldServer) world).getPlayerChunkMap().markBlockForUpdate(pos);
        }

    }

    @Override
    public void update() {
        boolean wasBurning = isBurning();
        boolean wasCooking = cookTime > 0;
        boolean dirty = false;
        if (wasBurning) {
            burnTime--;
        }

        if (!world.isRemote) {
            if (isBurning() || isStackInSlot(SLOT_LIQUID) && isStackInSlot(SLOT_FUEL)) {
                if (!isBurning() && canCook()) {
                    this.burnTime = TileEntityFurnace.getItemBurnTime(getStackInSlot(SLOT_FUEL));
                    if (isBurning()) {
                        decrStackSize(SLOT_FUEL, 1);
                        dirty = true;
                    }
                    totalBurnTime = burnTime;
                }
                if (isBurning() && this.canCook()) {
                    cookTime++;
                    if (cookTime >= totalCookTime) {
                        cookTime = 0;
                        this.totalCookTime = getCookTime();
                        this.finishCooking();
                        dirty = true;
                    }
                } else {
                    cookTime = 0;
                }

            } else if (!isBurning() && this.cookTime > 0) {
                this.cookTime = MathHelper.clamp(this.cookTime - 2, 0, this.totalCookTime);
            }
            if (wasBurning != this.isBurning()) {
                dirty = true;
            } else if (wasCooking != this.cookTime > 0) {
                dirty = true;
            }

        } else {
            //TODO particles
            //Do not check ISoundReference#isSoundPlaying for performance reason here. Also should not make any difference
            if (isCooking() && boilingSound == null && this.world.rand.nextInt(25) == 0) {
                boilingSound = VampLib.proxy.createSoundReference(ModSounds.boiling, SoundCategory.BLOCKS, getPos(), 0.015F, 7);

                boilingSound.startPlaying();
            } else if (!isCooking() && boilingSound != null) {
                boilingSound.stopPlaying();
                boilingSound = null;
            }

        }
        if (dirty) {
            this.markDirty(true);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setInteger("burntime", this.burnTime);
        compound.setInteger("cooktime", this.cookTime);
        compound.setInteger("cooktime_total", this.totalCookTime);
        if (ownerID != null) compound.setUniqueId("owner", ownerID);
        if (username != null) compound.setString("ownername", username);
        if (checkedRecipe != null) {
            compound.setBoolean("bypass_recipecheck", true);
        }
        return compound;
    }

    private boolean canCook() {
        if (!isStackInSlot(SLOT_LIQUID)) return false;
        IAlchemicalCauldronRecipe recipe = AlchemicalCauldronCraftingManager.getInstance().findRecipe(getStackInSlot(SLOT_LIQUID), getStackInSlot(SLOT_INGREDIENT));
        if (recipe == null) return false;
        totalCookTime = recipe.getCookingTime();
        if (!canPlayerCook(recipe)) return false;
        if (!isStackInSlot(SLOT_RESULT)) return true;
        if (!getStackInSlot(SLOT_RESULT).isItemEqual(recipe.getOutput())) return false;
        int size = ItemStackUtil.getCount(getStackInSlot(SLOT_RESULT)) + ItemStackUtil.getCount(recipe.getOutput());
        return size <= getInventoryStackLimit() && size <= getStackInSlot(SLOT_RESULT).getMaxStackSize();
    }

    /**
     * Checks if the owner can cook the recipe.
     * If a successful check was already performed for this recipe , the owner does not have to be online for this (unless the recipe changed in the meantime).
     */
    private boolean canPlayerCook(IAlchemicalCauldronRecipe recipe) {
        if (checkedRecipe != null && checkedRecipe.equals(recipe)) {
            return true;
        }
        EntityPlayer owner = getOwner();
        if (owner == null) return false;
        IHunterPlayer player = HunterPlayer.get(owner);
        ISkillHandler<IHunterPlayer> handler = player.getSkillHandler();
        boolean flag = recipe.canBeCooked(player.getLevel(), handler);
        if (flag) {
            checkedRecipe = recipe;
            return true;
        } else {
            checkedRecipe = null;
            return false;
        }
    }

    private void finishCooking() {
        if (canCook()) {
            IAlchemicalCauldronRecipe recipe = AlchemicalCauldronCraftingManager.getInstance().findRecipe(getStackInSlot(SLOT_LIQUID), getStackInSlot(SLOT_INGREDIENT));
            if (!isStackInSlot(SLOT_RESULT)) {
                setInventorySlotContents(SLOT_RESULT, recipe.getOutput().copy());
            } else if (getStackInSlot(SLOT_RESULT).isItemEqual(recipe.getOutput())) {
                ItemStackUtil.grow(getStackInSlot(SLOT_RESULT), ItemStackUtil.getCount(recipe.getOutput()));
            }
            if (recipe.isValidLiquidItem(getStackInSlot(SLOT_LIQUID))) {
                decrStackSize(SLOT_LIQUID, 1);
            } else {
                ItemStack fluidContainer = getStackInSlot(SLOT_LIQUID);
                FluidStack s = recipe.isValidFluidItem(fluidContainer);
                if (s != null) {
                    IFluidHandlerItem handler = (IFluidHandlerItem) FluidLib.getFluidItemCap(fluidContainer);
                    handler.drain(s, true);
                    setInventorySlotContents(SLOT_LIQUID, handler.getContainer());
                } else {
                    VampirismMod.log.w("AlchemicalCauldron", "Cooked item without valid input liquid (Recipe %s, Input %s)", recipe, fluidContainer);
                }
            }
            decrStackSize(SLOT_INGREDIENT, 1);
        }
    }

    private int getCookTime() {
        return 200;
    }

    /**
     * Null on client side
     *
     * @return The owner of this cauldron if he is online.
     */
    private
    @Nullable
    EntityPlayer getOwner() {
        if (ownerID != null && !world.isRemote) {
            return FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getPlayerByUUID(ownerID);
        }
        return null;
    }

    public void setOwner(EntityPlayer player) {
        ownerID = player.getPersistentID();
        this.markDirty(true);
    }

    private boolean isStackInSlot(int slot) {
        return !ItemStackUtil.isEmpty(getStackInSlot(slot));
    }
}
