package phylogeny.proportionaldestructionparticles;

import org.apache.logging.log4j.Logger;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod(modid = ProportionalDestructionParticles.MOD_ID,
	 version = ProportionalDestructionParticles.VERSION,
	 acceptedMinecraftVersions = ProportionalDestructionParticles.MC_VERSIONS_ACCEPTED,
	 updateJSON = ProportionalDestructionParticles.UPDATE_JSON,
	 dependencies = ProportionalDestructionParticles.DEPENDENCIES,
	 clientSideOnly = ProportionalDestructionParticles.CLIENT_ONLY)
public class ProportionalDestructionParticles
{

	public static final String MOD_ID = "pdp";
	public static final String VERSION = "@VERSION@";
	public static final String UPDATE_JSON = "@UPDATE@";
	public static final String MC_VERSIONS_ACCEPTED = "[1.12.2,)";
	public static final String DEPENDENCIES = "before:cofhcore;after:fbp@[2.4.1,);after:chiselsandbits";
	public static final boolean CLIENT_ONLY = true;
	private static final String INFO_LANG_KEY = "logger." + MOD_ID + ".info.replacement";
	private static Logger logger;

	@EventHandler
	public void preInit(FMLPreInitializationEvent event)
	{
		if (event.getSide() == Side.CLIENT)
			logger = event.getModLog();
	}

	@EventHandler
	public void init(FMLInitializationEvent event)
	{
		if (event.getSide() != Side.CLIENT)
			return;

		if (Loader.isModLoaded("fbp"))
			FBPParticleManagerMod.init();

		String info = I18n.format(INFO_LANG_KEY);
		logger.info(info.equals(INFO_LANG_KEY) ? "Replacing Minecraft#effectRenderer - Block destruction particles now only spawn in blocks' collision/bounding boxes." : info);
		Minecraft.getMinecraft().effectRenderer = new ParticleManagerMod(Minecraft.getMinecraft().world, Minecraft.getMinecraft().renderEngine);
	}

}