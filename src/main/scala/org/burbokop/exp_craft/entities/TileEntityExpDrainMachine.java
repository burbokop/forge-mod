package org.burbokop.exp_craft.entities;


import java.util.List;

import buildcraft.api.transport.IWireManager;
import buildcraft.api.transport.pipe.IPipe;
import buildcraft.api.transport.pipe.IPipeHolder;
import buildcraft.api.transport.pipe.PipeApi;
import buildcraft.api.transport.pipe.PipeEvent;
import buildcraft.api.transport.pluggable.PipePluggable;
import com.google.common.base.Predicate;
import com.mojang.authlib.GameProfile;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;
import org.burbokop.exp_craft.blocks.BlockExpDrainMachine;
import org.burbokop.exp_craft.fluids.ModFluids;
import org.burbokop.exp_craft.utils.*;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;

public class TileEntityExpDrainMachine extends TileEntityInventoryTyped<TileEntityExpDrainMachine.EnumSlot> implements ITickable {
	private int expConvertAmount;
	private int expConvertIntervalTicks;
	private FluidTank fluidTank;
	private int burnTime;
	private int totalBurnTime;
	private boolean draining = false;

	public enum EnumSlot {
		FUEL_SLOT,
		BUCKET_INPUT_SLOT,
		BUCKET_OUTPUT_SLOT
	}


	public TileEntityExpDrainMachine() {
		super(EnumSlot.values().length);


		Predicate<Fluid> expPredicate = ModFluids.fluidPredicate(scala.collection.JavaConversions.asScalaBuffer(
				Arrays.asList(new Fluid[]{ModFluids.EXP()})
		).seq());

		this.fluidTank = new FluidTankConsumable(ModFluids.EXP(), 0, 1000);
		this.fluidTank.setTileEntity(this);

		this.expConvertAmount = 1;
		this.expConvertIntervalTicks = 20; // 1 sec
	}

	public TileEntityExpDrainMachine(float expConvertIntervalSec, int expConvertAmount) {
		this();

		// TODO - sending to client or something else
		//this.expConvertAmount = expConvertAmount;
		//this.expConvertIntervalTicks = (int) (expConvertIntervalSec * 20.0f);

	}

	@Override
	public void readFromNBT(NBTTagCompound compound) {
		super.readFromNBT(compound);
		this.burnTime = compound.getInteger("BurnTime");
		this.totalBurnTime = compound.getInteger("TotalBurnTime");
		this.iterator = compound.getInteger("It");
		this.expConvertAmount = compound.getInteger("ExpConvertAmount");
		this.expConvertIntervalTicks = compound.getInteger("ExpConvertIntervalTicks");
		this.fluidTank.readFromNBT(compound.getCompoundTag("FluidTank"));
		this.draining = compound.getBoolean("Draining");
		//this.lastChangeTime = compound.getLong("LastChangeTime");
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		compound = super.writeToNBT(compound);
		compound.setInteger("BurnTime", (short)this.burnTime);
		compound.setInteger("TotalBurnTime", (short)this.totalBurnTime);
		compound.setInteger("It", (short)this.iterator);
		compound.setInteger("ExpConvertAmount", this.expConvertAmount);
		compound.setInteger("ExpConvertIntervalTicks", this.expConvertIntervalTicks);

		NBTTagCompound fluidTankCompound = new NBTTagCompound();
		this.fluidTank.writeToNBT(fluidTankCompound);
		compound.setTag("FluidTank", fluidTankCompound);

		compound.setBoolean("Draining", this.draining);
		//compound.setLong("LastChangeTime", lastChangeTime);
		return compound;
	}

	@Override
	public void networkRead(ByteBuf buf) {
		super.networkRead(buf);
		PacketBufferMod data = new PacketBufferMod(buf.readBytes(buf));
		this.fluidTank.setFluid(data.readFluidStack());
		this.burnTime = buf.readInt();
		this.totalBurnTime = buf.readInt();
		this.expConvertAmount = buf.readInt();
		this.expConvertIntervalTicks = buf.readInt();
		this.draining = buf.readBoolean();
	}

	@Override
	public void networkWrite(ByteBuf buf) {
		super.networkWrite(buf);
		PacketBufferMod data = new PacketBufferMod(Unpooled.buffer());
		data.writeFluidStack(this.fluidTank.getFluid());
		buf.writeBytes(data);

		buf.writeInt(this.burnTime);
		buf.writeInt(this.totalBurnTime);
		buf.writeInt(this.expConvertAmount);
		buf.writeInt(this.expConvertIntervalTicks);
		buf.writeBoolean(this.draining);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
			return (T) this.fluidTank;
		}
		return super.getCapability(capability, facing);
	}

	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
			return true;
		}
		return super.hasCapability(capability, facing);
	}

	@Override
	public boolean isItemValidForSlot(int index, ItemStack stack) {
		if(index == EnumSlot.FUEL_SLOT.ordinal()) {
			return TileEntityFurnace.isItemFuel(stack);
		} else if(index == EnumSlot.BUCKET_INPUT_SLOT.ordinal()) {
			return stack.getItem() == Items.BUCKET;
		} else if(index == EnumSlot.BUCKET_OUTPUT_SLOT.ordinal()) {
			return false;
		}
		return false;
	}

	public enum EnumFields {
		BURN_TIME,
		TOTAL_BURN_TIME,
		ITERATOR,
		DRAINING
	}

	int iterator = 0;
	@Override
	public int getField(int id) {
		switch(EnumFields.values()[id]) {
			case BURN_TIME:
				return this.burnTime;
			case TOTAL_BURN_TIME:
				return this.totalBurnTime;
			case ITERATOR:
				return this.iterator;
			case DRAINING:
				return this.draining ? 1 : 0;
			default:
				return 0;
		}
	}

	@Override
	public void setField(int id, int value) {
		switch(EnumFields.values()[id]) {
			case TOTAL_BURN_TIME:
				this.totalBurnTime = value;
				break;
			case BURN_TIME:
				this.burnTime = value;
				break;
			case ITERATOR:
				this.iterator = value;
				break;
			case DRAINING:
				this.draining = value != 0;
				break;
		}
	}

	@Override
	public int getFieldCount() {
		return EnumFields.values().length;
	}

	public boolean isBurning() {
		return this.burnTime > 0;
	}

	public boolean isDraining() { return this.draining; }

	public boolean isConvertTick() {
		if(expConvertIntervalTicks != 0) {
			return iterator % expConvertIntervalTicks == 0;
		}
		return false;
	}

	@Override
	public void update() {
		if (!world.isRemote) {
			BlockExpDrainMachine.setState(isBurning(), world, pos);
			iterator++;

			List<EntityPlayer> playersInTop = world.getEntitiesWithinAABB(EntityPlayer.class, new AxisAlignedBB(pos.add(0, 1, 0)));
			EntityPlayer playerWithExp = null;
			for(EntityPlayer player : playersInTop) {
				if(ExpUtils.canDecreaseExp(player, expConvertAmount)) {
					playerWithExp = player;
					break;
				}
			}

			if(this.isBurning()) {
				--this.burnTime;
			} else if(fluidTank.getFluidAmount() < fluidTank.getCapacity() && playerWithExp != null) {
				ItemStack fuelStack = getStackInSlot(EnumSlot.FUEL_SLOT.ordinal());
				if(!fuelStack.isEmpty()) {
					int itemBurnTime = BurningUtils.getItemBurnTime(fuelStack);
					if(itemBurnTime >= 0) {
						fuelStack.setCount(fuelStack.getCount() - 1);
						this.burnTime = itemBurnTime;
						this.totalBurnTime = itemBurnTime;
					}
				}
			}

			if(isConvertTick()) {
				draining = false;
				if (isBurning()) {
					if (playerWithExp != null && fluidTank.getFluidAmount() + expConvertAmount <= fluidTank.getCapacity()) {
						if (fluidTank.fillInternal(new FluidStack(ModFluids.EXP(), expConvertAmount), true) > 0) {
							ExpUtils.decreaseExp(playerWithExp, expConvertAmount);
							draining = true;
						}
					}
				}
			}

			ItemStack bucketInputSlot = getStackInSlot(EnumSlot.BUCKET_INPUT_SLOT);
			if(
					fluidTank.getFluidAmount() >= 1000 &&
							bucketInputSlot.getCount() > 0 &&
							getStackInSlot(EnumSlot.BUCKET_OUTPUT_SLOT).getCount() == 0
			) {
				setInventorySlotContents(EnumSlot.BUCKET_OUTPUT_SLOT, FluidUtil.getFilledBucket(fluidTank.drain(1000, true)));
				bucketInputSlot.setCount(bucketInputSlot.getCount() - 1);
			}

			if(iterator % 10 == 0) {
				sendMessageToClient();
			}
		}
	}
}
