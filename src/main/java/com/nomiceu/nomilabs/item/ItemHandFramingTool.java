package com.nomiceu.nomilabs.item;

import com.jaquadro.minecraft.storagedrawers.api.storage.INetworked;
import com.jaquadro.minecraft.storagedrawers.block.*;
import com.jaquadro.minecraft.storagedrawers.block.tile.TileEntityDrawers;
import com.jaquadro.minecraft.storagedrawers.block.tile.TileEntityTrim;
import com.jaquadro.minecraft.storagedrawers.block.tile.tiledata.MaterialData;
import com.nomiceu.nomilabs.NomiLabs;
import eutros.framedcompactdrawers.registry.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Objects;

public class ItemHandFramingTool extends Item { // TODO have this implement IFrameable when SD releases

    public ItemHandFramingTool(ResourceLocation rl, CreativeTabs tab) {
        setMaxStackSize(1);
        setCreativeTab(tab);
        setRegistryName(rl);
    }

    @Override
    public @NotNull EnumActionResult onItemUse(@NotNull EntityPlayer player, World world, @NotNull BlockPos pos,
                                               @NotNull EnumHand hand, @NotNull EnumFacing facing,
                                               float hitX, float hitY, float hitZ) {
        // This is to return success if we framed it, but not decorated it
        EnumActionResult actionResult = EnumActionResult.PASS;

        if (world.isAirBlock(pos))
            return actionResult;

        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();

        if (!(block instanceof INetworked))
            return actionResult;

        // At this point, further returns should be fail
        actionResult = EnumActionResult.FAIL;

        ItemStack tool = player.getHeldItem(hand);

        // Check if we should make this block a framed one
        if (!isDecorating(Objects.requireNonNull(block.getRegistryName()))){
            // Make it framed
            makeFramedState(world, pos);

            // This should be success, if we framed but not decorated
            actionResult = EnumActionResult.SUCCESS;
        }
        
        if (!tool.hasTagCompound())
            return actionResult;

        NBTTagCompound tagCompound = tool.getTagCompound();

        // hasTagCompound returns false if compound is null
        assert tagCompound != null;

        // Get Decorate Info
        ItemStack matS, matF, matT;

        matS = getItemStackFromKey(tagCompound, "MatS");
        if (matS.isEmpty())
            return actionResult;

        matT= getItemStackFromKey(tagCompound, "MatT");
        matF = getItemStackFromKey(tagCompound, "MatF");

        // Decorate
        MaterialData materialData = getMaterialData(world, pos);
        if (materialData != null) {
            materialData.setSide(matS.copy());
            materialData.setTrim(matT.copy());
            materialData.setFront(matF.copy());
        }

        // Reload Block
        world.markBlockRangeForRenderUpdate(pos, pos);

        return EnumActionResult.SUCCESS;
    }

    private boolean isDecorating(ResourceLocation registryName) {
        String registryString = registryName.toString();

        return registryName.getNamespace().equals("framedcompactdrawers")
               || registryString.equals("storagedrawers:customdrawers")
               || registryString.equals("storagedrawers:customtrim");
    }

    @SuppressWarnings("deprecation")
    private void makeFramedState(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        NBTTagCompound tag = new NBTTagCompound();

        IBlockState newState;

        // Special Case for drawers, to transfer items
        if (block instanceof BlockDrawers){
            TileEntityDrawers tile = Objects.requireNonNull((TileEntityDrawers) world.getTileEntity(pos));

            // Get nbt (items stored, locked, etc.) + direction
            tile.writeToPortableNBT(tag);
            int direction = tile.getDirection();

            // Only block that extends BlockDrawers at this point is drawers and framed drawers
            newState = block instanceof BlockCompDrawers ? ModBlocks.framedCompactDrawer.getDefaultState()
                        : com.jaquadro.minecraft.storagedrawers.core.ModBlocks.customDrawers.getStateFromMeta(block.getMetaFromState(state));

            // Set new BlockState
            world.setBlockState(pos, newState);

            // Reload tile, to the new block
            tile = Objects.requireNonNull((TileEntityDrawers) world.getTileEntity(pos));

            // Load back nbt + direction
            tile.readFromPortableNBT(tag);
            tile.setDirection(direction);
            return;
        }

        // Only block that and extends INetworked at this point is controllers, slaves, and trims
        Block newBlock = block instanceof BlockController ? ModBlocks.framedDrawerController:
                        block instanceof BlockSlave ? ModBlocks.framedSlave :
                        com.jaquadro.minecraft.storagedrawers.core.ModBlocks.customTrim;

        // Meta for controllers are their direction, so read that (Custom Controller's meta is a bit different to normal controller, so -2 to meta is needed)
        newState = block instanceof BlockController ? newBlock.getStateFromMeta(block.getMetaFromState(state) - 2)
                : newBlock.getDefaultState();

        world.setBlockState(pos, newState);
        
    }

    @Nullable
    private MaterialData getMaterialData(World world, BlockPos pos) {
        TileEntity tile = world.getTileEntity(pos);

        // Framed Comp Drawers, Controller & Drawers
        if (tile instanceof TileEntityDrawers drawers) {
            return drawers.material();
        }

        // Framed Trim
        if (tile instanceof TileEntityTrim trim)
            return trim.material();

        // Tile was null, or didn't inherit from these, aka error
        NomiLabs.LOGGER.fatal("[Hand Framing Tool] Failed to get the material data of tile entity at block pos {}.", pos);
        return null;
    }

    private ItemStack getItemStackFromKey(NBTTagCompound tagCompound, String key) {
        if (!tagCompound.hasKey(key))
            return ItemStack.EMPTY;

        else {
            return new ItemStack(tagCompound.getCompoundTag(key));
        }
    }
}