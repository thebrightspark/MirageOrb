package brightspark.mirageorb;

import brightspark.mirageorb.message.MessageUseOrb;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.OreDictionary;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
		EntityPlayer player = Minecraft.getMinecraft().player;
		return player.isCreative() || !player.getCooldownTracker().hasCooldown(stack.getItem());
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand)
	{
		//If still on cooldown, then don't do anything
		if(player.getCooldownTracker().hasCooldown(this))
			return super.onItemRightClick(world, player, hand);

		//Make sure player has the cost items to consume
		if(player.isCreative() || ModConfig.cost == null || hasEnoughCostStacks(player))
		{
			//Tell the server that the orb has been activated so it can send back the player ghost details
			if(world.isRemote && player instanceof EntityPlayerSP)
			{
				EntityPlayerSP playerSP = (EntityPlayerSP) player;
				MessageUseOrb message = new MessageUseOrb();
				message.playerName = playerSP.getName();
				message.resourceLocation = playerSP.getLocationSkin();
				message.rotationYaw = playerSP.rotationYaw;
				message.rotationPitch = playerSP.rotationPitch;
				message.rotationYawHead = playerSP.rotationYawHead;
				message.renderYawOffset = playerSP.renderYawOffset;
				message.swingProgress = playerSP.swingProgress;
				message.limbSwing = playerSP.limbSwing;
				message.limbSwingAmount = playerSP.limbSwingAmount;
				message.isSneaking = playerSP.isSneaking();
				message.isSwingInProgress = playerSP.isSwingInProgress;
				message.swingProgressInt = playerSP.swingProgressInt;
				message.swingingHand = playerSP.swingingHand;
				message.handSide = playerSP.getPrimaryHand();
				MirageOrb.NETWORK.sendToServer(message);
			}
			return new ActionResult<>(EnumActionResult.SUCCESS, player.getHeldItem(hand));
		}
		else
		{
			player.sendStatusMessage(new TextComponentTranslation("item.mirageorb.message.cost",
				ModConfig.cost.getCount(), ModConfig.cost.getDisplayName()), true);
			return new ActionResult<>(EnumActionResult.PASS, player.getHeldItem(hand));
		}
	}

	@Override
	public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn)
	{
		tooltip.add(I18n.format(tooltipPrefix + "1"));
		tooltip.add(I18n.format(tooltipPrefix + "2"));
		Minecraft mc = Minecraft.getMinecraft();
		if(mc.player == null)
			return;
		//Cooldown is given as a percentage (0F - 1F) of the time remaining
		float cooldown = mc.player.getCooldownTracker().getCooldown(stack.getItem(), mc.getRenderPartialTicks());
		if(cooldown > 0F)
			tooltip.add(I18n.format(tooltipPrefix + "3", String.format("%.1f", ModConfig.mirageOrbCooldown * cooldown)));
	}

	public static Map<Integer, ItemStack> findCostStacks(EntityPlayer player)
	{
		ItemStack cost = ModConfig.cost;
		int found = cost.getCount();
		InventoryPlayer inv = player.inventory;
		Map<Integer, ItemStack> foundStacks = new HashMap<>();
		for(int i = 0; i < inv.getSizeInventory(); i++)
		{
			ItemStack stack = inv.getStackInSlot(i);
			if(OreDictionary.itemMatches(cost, stack, false))
			{
				found += stack.getCount();
				foundStacks.put(i, stack);
				if(found >= cost.getCount())
					break;
			}
		}
		return found >= cost.getCount() ? foundStacks : null;
	}

	private static boolean hasEnoughCostStacks(EntityPlayer player)
	{
		Map<Integer, ItemStack> stacks = findCostStacks(player);
		if (stacks == null)
			return false;
		int cost = ModConfig.cost.getCount();
		int total = 0;
		for (ItemStack stack : stacks.values())
		{
			if ((total += stack.getCount()) >= cost)
				return true;
		}
		return false;
	}
}
