package com.example.examplemod.gui;

import javax.vecmath.Point2i;

import org.lwjgl.util.Rectangle;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.containers.MashineContainer;
import com.example.examplemod.entities.MashineTileEntity;
import com.example.examplemod.utils.PlayerSlotsTemplate.InvalidSlotsSequence;
import com.example.examplemod.utils.TankWidget;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import com.example.examplemod.utils.PointInRectDetector;

@SideOnly(Side.CLIENT)
public class MashineGuiScreen extends GuiContainer {

	private static final ResourceLocation BACKGROUND_TEXTURE = new ResourceLocation(ExampleMod.MODID + ":textures/gui/guimashine.png");
	private static final Point2i BACKGROUND_SIZE = new Point2i(176, 166);

	private static final Rectangle TANK_RECT = new Rectangle(100, 26, 12, 47);

	private MashineTileEntity tileEntity;
	private InventoryPlayer player;

	public MashineGuiScreen(InventoryPlayer player, MashineTileEntity tileEntity) throws InvalidSlotsSequence {
		super(new MashineContainer(player, tileEntity));
		this.player = player;
		this.tileEntity = tileEntity;		
	}



	@Override
	public boolean doesGuiPauseGame() { return false; }


	public String getTileEntityDisplayName() {
		if(tileEntity != null) {
			ITextComponent name = tileEntity.getDisplayName();			
			if(name != null) {
				return name.getUnformattedText();
			} else {
				return "null display name";
			}
		} else {
			return "null tile entity";
		}
	}

	public String getPlayerDisplayName() {
		if(player != null) {
			ITextComponent name = player.getDisplayName();
			if(name != null) {
				return name.getUnformattedText();
			} else {
				return "null display name";					
			}
		} else {
			return "null player";
		}
	}

	public String getFluidTankInfo() {
		FluidTank fluidTank = (FluidTank) this.tileEntity
				.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null);
		if(fluidTank != null) {
			FluidStack fluidStack = fluidTank.getFluid();
			if(fluidStack != null) {
				return fluidStack.getLocalizedName() + ":" + fluidStack.amount;
			} else {
				return "null fluid stack";
			}
		} else {
			return "null fluid tank";
		}
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
		String tileEntityName = getTileEntityDisplayName();

		int iterator = this.tileEntity.getField(MashineTileEntity.EnumFields.ITERATOR.ordinal());

		fontRenderer.drawString(tileEntityName + ":" + iterator, xSize / 2 - fontRenderer.getStringWidth(tileEntityName) / 2, 6, 4210752);
		fontRenderer.drawString(getPlayerDisplayName(), 8, ySize - 96 + 2, 4210752);

		if(PointInRectDetector.detect(
				new Point2i(mouseX, mouseY), 
				new Point2i(guiLeft, guiTop),
				TANK_RECT
				)) {
			String info = getFluidTankInfo();
			
			
			drawRect(mouseX - guiLeft, mouseY - guiTop - 9, mouseX - guiLeft + fontRenderer.getStringWidth(info), mouseY - guiTop, 0x88000000);
			fontRenderer.drawString(
					info, 
					mouseX - guiLeft, 
					mouseY - guiTop - 8, 
					0xff8800
					);	
		}        
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		GlStateManager.color(1, 1, 1, 1);
		if(mc != null) {
			mc.getTextureManager().bindTexture(BACKGROUND_TEXTURE);
		}
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, BACKGROUND_SIZE.x, BACKGROUND_SIZE.y);

		if(tileEntity.isBurning()) {
			int k = this.getBurnLeftScaled(13);
			this.drawTexturedModalRect(this.guiLeft + 72, this.guiTop + 40 + 12 - k, 196, 12 - k, 14, k + 1);
		}

		FluidTank fluidTank = (FluidTank) this.tileEntity
				.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null);

		drawFluid(fluidTank, this.guiLeft + TANK_RECT.getX(), this.guiTop + TANK_RECT.getY(), TANK_RECT.getWidth(), TANK_RECT.getHeight(), true);
	}


	private void drawFluid(FluidTank fluidTank, int left, int top, int width, int height, boolean bindTexture) {
		if (fluidTank == null) {
			throw new NullPointerException("Do not try to draw null FluidTank.");
		} else if (fluidTank.getFluid() == null) {
			drawRect(left, top, left + width, top + height, /* 0x8B8B8B */0x000000);
			System.out.println("fluidTank.getFluid() == null");
			return;
		} else if (fluidTank.getFluid().getFluid() == null) {
			System.out.println("Fluid of given FluidTank is null.");
			return;
		} else if (fluidTank.getFluid().getFluid().getStill() == null) {
			System.out.println("The texture of given Fluid is null. Maybe you forgot to register textures?");
			return;
		}

		TankWidget.draw(fluidTank, left, top, width, height, 0, 0, zLevel);

		this.mc.getTextureManager().bindTexture(BACKGROUND_TEXTURE);
		this.drawTexturedModalRect(left, top, 176, 55, width, height);		
	}

	private int getBurnLeftScaled(int pixels) {
		int i = this.tileEntity.getField(MashineTileEntity.EnumFields.TOTAL_BURN_TIME.ordinal());
		if(i == 0) i = Integer.MAX_VALUE;
		return this.tileEntity.getField(MashineTileEntity.EnumFields.BURN_TIME.ordinal()) * pixels / i;
	}
}