package brightspark.mirageorb;

import net.minecraftforge.common.config.Config;

@Config(modid = MirageOrb.MODID)
public class ModConfig
{
	@Config.Comment("The Mirage Orb's usage cooldown in seconds")
	@Config.RangeInt(min = 0)
	public static int mirageOrbCooldown = 60;

	@Config.Comment("The Mirage Orb Ghost's life in seconds")
	@Config.RangeInt(min = 1)
	public static int mirageOrbGhostLife = 10;
}
