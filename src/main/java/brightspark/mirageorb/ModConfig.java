package brightspark.mirageorb;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.oredict.OreDictionary;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Config(modid = MirageOrb.MODID)
public class ModConfig
{
	@Config.Comment("The Mirage Orb's usage cooldown in seconds")
	@Config.RangeInt(min = 0)
	public static int mirageOrbCooldown = 60;

	@Config.Comment("The Mirage Orb Ghost's life in seconds")
	@Config.RangeInt(min = 1)
	public static int mirageOrbGhostLife = 10;

	@Config.Comment({
		"The item to consume whenever the Mirage Orb is used",
		"The format here is '<modId>:<registryName>@<metaData>'",
		"If modId isn't given, then 'minecraft' is used by default",
		"If metaData isn't given, then 0 is used by default",
		"Use '*' as a wildcard for metaData (will match any meta data)"
	})
	private static String costItem = "minecraft:ender_pearl";

	@Config.Comment({
		"The amount of the 'costItem' that is consumed whenever the Mirage Orb is used",
		"Set this to 0 to not require any item to be consumed"
	})
	@Config.RangeInt(min = 0)
	private static int costAmount = 1;

	@Config.Ignore
	private static Pattern costPattern = Pattern.compile("(?:(?<modid>\\w+):)?(?<regName>\\w+)(?:@(?<meta>.+))?");

	@Config.Ignore
	public static ItemStack cost = null;

	public static void initCostStack() {
		cost = null;
		if(costAmount <= 0)
			return;

		Matcher matcher = costPattern.matcher(costItem);
		if(matcher.find())
		{
			String regName = matcher.group("regName");
			if(regName == null)
			{
				MirageOrb.logger.error("costItem config doesn't contain an item registry name! ({})", costItem);
				return;
			}

			String modId = matcher.group("modid");
			if(modId == null)
				modId = "minecraft";

			Item item = Item.REGISTRY.getObject(new ResourceLocation(modId, regName));
			if(item == null)
			{
				MirageOrb.logger.error("The costItem '{}:{}' does not exist! ({})", modId, regName, costItem);
				return;
			}

			String meta = matcher.group("meta");
			if(meta == null)
				meta = "0";
			int metaNum;
			try
			{
				metaNum = Integer.parseInt(meta);
			}
			catch(NumberFormatException e)
			{
				if(meta.equals("*"))
					metaNum = OreDictionary.WILDCARD_VALUE;
				else
				{
					MirageOrb.logger.error("The metaData of the costItem config is invalid! Defaulting to 0 ({})", costItem);
					metaNum = 0;
				}
			}

			cost = new ItemStack(item, costAmount, metaNum);
		}
	}
}
