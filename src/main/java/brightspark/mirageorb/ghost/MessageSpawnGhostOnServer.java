package brightspark.mirageorb.ghost;

import brightspark.mirageorb.ItemMirageOrb;
import brightspark.mirageorb.MirageOrb;
import brightspark.mirageorb.ModConfig;
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
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.List;

public class MessageSpawnGhostOnServer implements IMessage
{
	public String playerName;
	public ResourceLocation resourceLocation;
	public float rotationYaw, rotationPitch, rotationYawHead, renderYawOffset, swingProgress, limbSwing, limbSwingAmount;
	public boolean isSneaking, isSwingInProgress;
	public int swingProgressInt;
	public EnumHand swingingHand;
	public EnumHandSide handSide;

	public MessageSpawnGhostOnServer() {}

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

	public static class Handler implements IMessageHandler<MessageSpawnGhostOnServer, IMessage>
	{
		@Override
		public IMessage onMessage(final MessageSpawnGhostOnServer message, final MessageContext ctx)
		{
			IThreadListener mainThread = (WorldServer) ctx.getServerHandler().player.world;
			mainThread.addScheduledTask(new Runnable()
			{
				@Override
				public void run()
				{
					WorldServer server = (WorldServer) ctx.getServerHandler().player.world;
					EntityPlayer player = server.getPlayerEntityByName(message.playerName);
					if(player == null)
					{
						MirageOrb.logger.warn("Player " + message.playerName + " not found when trying to spawn ghost!");
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
					if(!holdingOrb || player.getCooldownTracker().hasCooldown(MirageOrb.MIRAGE_ORB))
						return;

					//Start cooldown
					if(!player.isCreative())
						player.getCooldownTracker().setCooldown(MirageOrb.MIRAGE_ORB, ModConfig.mirageOrbCooldown * 20);

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
				}
			});
			return null; //No response
		}
	}
}