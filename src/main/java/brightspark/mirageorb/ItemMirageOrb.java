package brightspark.mirageorb;

import brightspark.mirageorb.ghost.MessageSpawnGhostOnServer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

public class ItemMirageOrb extends Item
{
	private static final String tooltipPrefix = "item.mirageorb.tooltip.";

	public ItemMirageOrb()
	{
		setCreativeTab(MirageOrb.TAB);
		setTranslationKey("mirageorb");
		setRegistryName("mirageorb");
		setMaxStackSize(1);
	}

	@SideOnly(Side.CLIENT)
	@Override
	public boolean hasEffect(ItemStack stack)
	{
		return !Minecraft.getMinecraft().player.getCooldownTracker().hasCooldown(stack.getItem());
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand)
	{
		//If still on cooldown, then don't do anything
		if(player.getCooldownTracker().hasCooldown(this))
			return super.onItemRightClick(world, player, hand);

		//Start the cooldown and spawn the ghost
		player.getCooldownTracker().setCooldown(this, ModConfig.mirageOrbCooldown * 20);
		MirageOrb.logger.info("Cooldown started");
		if(world.isRemote && player instanceof AbstractClientPlayer)
		{
			MirageOrb.logger.info("Sending message to server to spawn ghost");
			MessageSpawnGhostOnServer message = new MessageSpawnGhostOnServer();
			message.playerName = player.getName();
			message.resourceLocation = ((AbstractClientPlayer) player).getLocationSkin();
			message.rotationYaw = player.rotationYaw;
			message.rotationPitch = player.rotationPitch;
			message.rotationYawHead = player.rotationYawHead;
			message.renderYawOffset = player.renderYawOffset;
			message.swingProgress = player.swingProgress;
			message.limbSwing = player.limbSwing;
			message.limbSwingAmount = player.limbSwingAmount;
			message.isSneaking = player.isSneaking();
			message.isSwingInProgress = player.isSwingInProgress;
			message.swingProgressInt = player.swingProgressInt;
			message.swingingHand = player.swingingHand;
			message.handSide = player.getPrimaryHand();
		}
		return new ActionResult<>(EnumActionResult.SUCCESS, player.getHeldItem(hand));
	}

	@Override
	public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn)
	{
		tooltip.add(I18n.format(tooltipPrefix + "1"));
		tooltip.add(I18n.format(tooltipPrefix + "2"));
		//Cooldown is given as a percentage (0F - 1F) of the time remaining
		if(Minecraft.getMinecraft().player == null)
			return;
		float cooldown = Minecraft.getMinecraft().player.getCooldownTracker().getCooldown(stack.getItem(), Minecraft.getMinecraft().getRenderPartialTicks());
		if(cooldown > 0F)
			tooltip.add(I18n.format(tooltipPrefix + "3", String.format("%.1f", (ModConfig.mirageOrbCooldown * 20) * (1F - cooldown))));
	}
}
