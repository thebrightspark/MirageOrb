package brightspark.mirageorb;

import brightspark.mirageorb.ghost.EntityPlayerGhost;
import brightspark.mirageorb.ghost.RenderPlayerGhost;
import brightspark.mirageorb.message.MessageSetClientGhostData;
import brightspark.mirageorb.message.MessageUseOrb;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.Logger;

@Mod(modid = MirageOrb.MODID, name = MirageOrb.NAME, version = MirageOrb.VERSION)
@Mod.EventBusSubscriber
public class MirageOrb
{
	public static final String MODID = "mirageorb";
	public static final String NAME = "Mirage Orb";
	public static final String VERSION = "@VERSION@";

	public static Logger logger;

	static final CreativeTabs TAB = new CreativeTabs(MODID)
	{
		@Override
		public ItemStack createIcon()
		{
			return new ItemStack(MIRAGE_ORB);
		}
	};

	public static SimpleNetworkWrapper NETWORK;

	public static final Item MIRAGE_ORB = new ItemMirageOrb();

	@EventHandler
	public void preInit(FMLPreInitializationEvent event)
	{
		logger = event.getModLog();

		NETWORK = NetworkRegistry.INSTANCE.newSimpleChannel(MODID);
		NETWORK.registerMessage(MessageUseOrb.Handler.class, MessageUseOrb.class, 0, Side.SERVER);
		NETWORK.registerMessage(MessageSetClientGhostData.Handler.class, MessageSetClientGhostData.class, 1, Side.CLIENT);
	}

	@EventHandler
	public void postInit(FMLPostInitializationEvent event)
	{
		ModConfig.initCostStack();
	}

	@SubscribeEvent
	public static void regItems(RegistryEvent.Register<Item> event)
	{
		event.getRegistry().register(MIRAGE_ORB);
	}

	@SubscribeEvent
	public static void regEntities(RegistryEvent.Register<EntityEntry> event)
	{
		event.getRegistry().register(EntityEntryBuilder.create()
			.entity(EntityPlayerGhost.class)
			.id("playerghost", 0)
			.name(MODID + ".playerghost")
			.tracker(64, 1, false)
			.build());
	}

	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public static void regModels(ModelRegistryEvent event)
	{
		ModelLoader.setCustomModelResourceLocation(MIRAGE_ORB, 0, new ModelResourceLocation(MIRAGE_ORB.getRegistryName(), "inventory"));
		RenderingRegistry.registerEntityRenderingHandler(EntityPlayerGhost.class, RenderPlayerGhost::new);
	}

	@SubscribeEvent
	public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event)
	{
		if(event.getModID().equals(MODID))
		{
			ConfigManager.sync(MODID, Config.Type.INSTANCE);
			ModConfig.initCostStack();
		}
	}
}
