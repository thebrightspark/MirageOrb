package brightspark.mirageorb.ghost;

import brightspark.mirageorb.ModConfig;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;

public class EntityPlayerGhost extends EntityLivingBase implements IEntityAdditionalSpawnData
{
	private static final String KEY_SKIN = "ghostSkin";
	private final int MAX_GHOST_AGE = ModConfig.mirageOrbGhostLife * 20;
	public ResourceLocation playerSkin;
	public EnumHandSide handSide;

	public EntityPlayerGhost(World worldIn)
	{
		super(worldIn);
		noClip = true;
	}

	public EntityPlayerGhost(World worldIn, EntityPlayer player)
	{
		this(worldIn);
		//Copy position from player
		copyLocationAndAnglesFrom(player);
	}

	@Override
	public void onUpdate()
	{
		if(ForgeHooks.onLivingUpdate(this))
			return;
		onLivingUpdate();
		firstUpdate = false;
	}

	@Override
	public void onLivingUpdate()
	{
		if(!world.isRemote && ticksExisted > MAX_GHOST_AGE)
			setDead();
	}

	@Override
	public void onKillCommand()
	{
		setDead();
	}

	@Override
	protected void outOfWorld()
	{
		setDead();
	}

	@Override
	public void knockBack(Entity entityIn, float strength, double xRatio, double zRatio) {}

	@Override
	public boolean attackEntityFrom(DamageSource source, float amount)
	{
		if(source.equals(DamageSource.OUT_OF_WORLD))
		{
			damageEntity(source, amount);
			return true;
		}
		return false;
	}

	@Override
	public boolean canBePushed()
	{
		return false;
	}

	@Override
	public boolean isPushedByWater()
	{
		return false;
	}

	@Override
	public Iterable<ItemStack> getArmorInventoryList()
	{
		return NonNullList.withSize(4, ItemStack.EMPTY);
	}

	@Override
	public ItemStack getItemStackFromSlot(EntityEquipmentSlot slotIn)
	{
		return ItemStack.EMPTY;
	}

	@Override
	public void setItemStackToSlot(EntityEquipmentSlot slotIn, ItemStack stack) {}

	@Override
	public EnumHandSide getPrimaryHand()
	{
		return handSide;
	}

	@Override
	public void writeSpawnData(ByteBuf buffer)
	{
		ByteBufUtils.writeUTF8String(buffer, playerSkin == null ? "" : playerSkin.toString());
	}

	@Override
	public void readSpawnData(ByteBuf additionalData)
	{
		playerSkin = new ResourceLocation(ByteBufUtils.readUTF8String(additionalData));
	}

	@Override
	public float getSwingProgress(float partialTickTime)
	{
		return swingProgress;
	}

	@Override
	public void readEntityFromNBT(NBTTagCompound nbt)
	{
		super.readEntityFromNBT(nbt);
		playerSkin = new ResourceLocation(nbt.getString(KEY_SKIN));
	}

	@Override
	public void writeEntityToNBT(NBTTagCompound nbt)
	{
		super.writeEntityToNBT(nbt);
		nbt.setString(KEY_SKIN, playerSkin.toString());
	}

	public int getAgeRemaining()
	{
		return MAX_GHOST_AGE - ticksExisted;
	}
}
