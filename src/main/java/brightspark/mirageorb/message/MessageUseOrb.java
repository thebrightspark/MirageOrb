package brightspark.mirageorb.message;

import brightspark.mirageorb.ItemMirageOrb;
import brightspark.mirageorb.MirageOrb;
import brightspark.mirageorb.ModConfig;
import brightspark.mirageorb.ghost.EntityPlayerGhost;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.IThreadListener;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.List;
import java.util.Map;

public class MessageUseOrb implements IMessage
{
	public String playerName;
	public ResourceLocation resourceLocation;
	public float rotationYaw, rotationPitch, rotationYawHead, renderYawOffset, swingProgress, limbSwing, limbSwingAmount;
	public boolean isSneaking, isSwingInProgress;
	public int swingProgressInt;
	public EnumHand swingingHand;
	public EnumHandSide handSide;

	public MessageUseOrb() {}

	@Override
	public void fromBytes(ByteBuf buf)
	{
		playerName = ByteBufUtils.readUTF8String(buf);
		resourceLocation = new ResourceLocation(ByteBufUtils.readUTF8String(buf));
		rotationYaw = buf.readFloat();
		rotationPitch = buf.readFloat();
		rotationYawHead = buf.readFloat();
		renderYawOffset = buf.readFloat();
		swingProgress = buf.readFloat();
		limbSwing = buf.readFloat();
		limbSwingAmount = buf.readFloat();
		isSneaking = buf.readBoolean();
		isSwingInProgress = buf.readBoolean();
		swingProgressInt = buf.readInt();
		swingingHand = buf.readBoolean() ? EnumHand.MAIN_HAND : EnumHand.OFF_HAND;
		handSide = buf.readBoolean() ? EnumHandSide.RIGHT : EnumHandSide.LEFT;
	}

	@Override
	public void toBytes(ByteBuf buf)
	{
		ByteBufUtils.writeUTF8String(buf, playerName);
		ByteBufUtils.writeUTF8String(buf, resourceLocation.toString());
		buf.writeFloat(rotationYaw);
		buf.writeFloat(rotationPitch);
		buf.writeFloat(rotationYawHead);
		buf.writeFloat(renderYawOffset);
		buf.writeFloat(swingProgress);
		buf.writeFloat(limbSwing);
		buf.writeFloat(limbSwingAmount);
		buf.writeBoolean(isSneaking);
		buf.writeBoolean(isSwingInProgress);
		buf.writeInt(swingProgressInt);
		buf.writeBoolean(swingingHand == EnumHand.MAIN_HAND);
		buf.writeBoolean(handSide == EnumHandSide.RIGHT);
	}

	public static class Handler implements IMessageHandler<MessageUseOrb, IMessage>
	{
		@Override
		public IMessage onMessage(final MessageUseOrb message, final MessageContext ctx)
		{
			IThreadListener mainThread = (WorldServer) ctx.getServerHandler().player.world;
			mainThread.addScheduledTask(() -> {
				WorldServer server = (WorldServer) ctx.getServerHandler().player.world;
				EntityPlayer player = server.getPlayerEntityByName(message.playerName);
				if(player == null)
				{
					MirageOrb.logger.warn("Player {} not found when trying to spawn ghost!", message.playerName);
					return;
				}

				//Validate item and cooldown
				boolean holdingOrb = false;
				for(ItemStack held : player.getHeldEquipment())
				{
					if(held.getItem() instanceof ItemMirageOrb)
					{
						holdingOrb = true;
						break;
					}
				}
				if(!holdingOrb)
				{
					MirageOrb.logger.warn("Player {} isn't actually holding a Mirage Orb!", message.playerName);
					return;
				}

				if(!player.isCreative())
				{
					if(player.getCooldownTracker().hasCooldown(MirageOrb.MIRAGE_ORB))
					{
						MirageOrb.logger.warn("Player {} Mirage Orb is still on cooldown!", message.playerName);
						return;
					}

					if (ModConfig.cost != null)
					{
						//Check has cost items and consume them
						Map<Integer, ItemStack> costStacksInSlots = ItemMirageOrb.findCostStacks(player);
						if (costStacksInSlots == null)
						{
							player.sendStatusMessage(new TextComponentTranslation("item.mirageorb.message.cost",
								ModConfig.cost.getCount(), ModConfig.cost.getDisplayName()), true);
							return;
						}
						int toConsumeRemaining = ModConfig.cost.getCount();
						for (Map.Entry<Integer, ItemStack> costStackInSlot : costStacksInSlots.entrySet()) {
							ItemStack costStack = costStackInSlot.getValue();
							int toConsume = Math.min(toConsumeRemaining, costStack.getCount());
							costStack.shrink(toConsume);
							if (costStack.isEmpty())
								player.inventory.setInventorySlotContents(costStackInSlot.getKey(), ItemStack.EMPTY);
							toConsumeRemaining -= toConsume;
							if (toConsumeRemaining <= 0)
								break;
						}

						//Start cooldown
						player.getCooldownTracker().setCooldown(MirageOrb.MIRAGE_ORB, ModConfig.mirageOrbCooldown * 20);
					}
				}

				//Create ghost
				EntityPlayerGhost ghost = new EntityPlayerGhost(server, player);
				ghost.playerSkin = message.resourceLocation;
				server.spawnEntity(ghost);

				//Update the client entities with the correct data
				MirageOrb.NETWORK.sendToAll(new MessageSetClientGhostData(ghost.getEntityId(), message));

				//Get nearby mobs
				BlockPos playerPos = player.getPosition();
				List<Entity> entities = server.getEntitiesWithinAABBExcludingEntity(player, new AxisAlignedBB(playerPos).grow(20));

				//Set nearby attacking mobs to attack the ghost
				for(Entity e : entities)
				{
					if(e instanceof EntityLiving && (player.equals(((EntityLiving) e).getAttackTarget())))
						//Set attacking target to the ghost
						((EntityLiving) e).setAttackTarget(ghost);
				}
			});
			return null; //No response
		}
	}
}