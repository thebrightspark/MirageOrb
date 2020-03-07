package brightspark.mirageorb.message;

import brightspark.mirageorb.MirageOrb;
import brightspark.mirageorb.ghost.EntityPlayerGhost;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MessageSetClientGhostData implements IMessage
{
	private int ghostId;
	private float rotationYaw, rotationPitch, rotationYawHead, renderYawOffset, swingProgress, limbSwing, limbSwingAmount;
	private boolean isSneaking, isSwingInProgress;
	private int swingProgressInt;
	private EnumHand swingingHand;
	private EnumHandSide handSide;

	public MessageSetClientGhostData() {}

	public MessageSetClientGhostData(int ghostId, MessageUseOrb message)
	{
		this.ghostId = ghostId;
		rotationYaw = message.rotationYaw;
		rotationPitch = message.rotationPitch;
		rotationYawHead = message.rotationYawHead;
		renderYawOffset = message.renderYawOffset;
		swingProgress = message.swingProgress;
		limbSwing = message.limbSwing;
		limbSwingAmount = message.limbSwingAmount;
		isSneaking = message.isSneaking;
		isSwingInProgress = message.isSwingInProgress;
		swingProgressInt = message.swingProgressInt;
		swingingHand = message.swingingHand;
		handSide = message.handSide;
	}

	@Override
	public void fromBytes(ByteBuf buf)
	{
		ghostId = buf.readInt();
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
		buf.writeInt(ghostId);
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

	public static class Handler implements IMessageHandler<MessageSetClientGhostData, IMessage>
	{
		@Override
		public IMessage onMessage(final MessageSetClientGhostData message, MessageContext ctx)
		{
			Minecraft mc = Minecraft.getMinecraft();
			//noinspection Convert2Lambda
			mc.addScheduledTask(new Runnable()
			{
				@Override
				public void run()
				{
					World world = mc.world;
					Entity entity = world.getEntityByID(message.ghostId);
					if (!(entity instanceof EntityPlayerGhost))
					{
						MirageOrb.logger.info("{} isn't an EntityPlayerGhost!", entity);
						return;
					}
					EntityPlayerGhost ghost = (EntityPlayerGhost) entity;
					ghost.rotationYaw = message.rotationYaw;
					ghost.rotationPitch = message.rotationPitch;
					ghost.rotationYawHead = message.rotationYawHead;
					ghost.renderYawOffset = message.renderYawOffset;
					ghost.swingProgress = message.swingProgress;
					ghost.limbSwing = message.limbSwing;
					ghost.limbSwingAmount = message.limbSwingAmount;
					ghost.setSneaking(message.isSneaking);
					ghost.isSwingInProgress = message.isSwingInProgress;
					ghost.swingProgressInt = message.swingProgressInt;
					ghost.swingingHand = message.swingingHand;
					ghost.handSide = message.handSide;
				}
			});
			return null;
		}
	}
}
