package phylogeny.proportionaldestructionparticles;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.Config.Comment;
import net.minecraftforge.common.config.Config.LangKey;
import net.minecraftforge.common.config.Config.Name;
import net.minecraftforge.common.config.Config.RangeDouble;
import net.minecraftforge.common.config.Config.Type;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent.OnConfigChangedEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Config(modid = ProportionalDestructionParticles.MOD_ID)
@LangKey("config." + ProportionalDestructionParticles.MOD_ID + ".title")
@EventBusSubscriber(modid = ProportionalDestructionParticles.MOD_ID, value = Side.CLIENT)
public class ConfigMod
{

	private static final String PREFIX = "config." + ProportionalDestructionParticles.MOD_ID + ".client";

	@Name("Client")
	@Comment("Client-only configs.")
	@LangKey(PREFIX)
	public static final Client CLIENT = new Client();

	public static class Client
	{
		@Name("Enabled")
		@Comment("If true, block destruction particles will spawn normally (throughout the block space), otherwise they will only spawn in blocks' collision/bounding boxes.")
		@LangKey(PREFIX + ".enabled")
		public boolean enabled = true;

		@Name("Collision Boxes")
		@Comment("If true, block destruction particles only spawn in the collision boxes of blocks that have them, otherwise they will only spawn in their bounding boxes.")
		@LangKey(PREFIX + ".collision")
		public boolean collisionBoxes = true;

		@Name("Box Growth")
		@Comment("The boxes that block destruction particles are allowed to spawn in is expanded by this many meters.")
		@LangKey(PREFIX + ".growth")
		@RangeDouble(min = 0)
		public double boxGrowth = 0.1;

		@Name("Random")
		@Comment("If true, block destruction particles spawn randomly within blocks' collision/bounding boxes, otherwise they will spawn at evenly spaced, "
				+ "pre-determined positions that fall withing expanded versions of those boxes.")
		@LangKey(PREFIX + ".random")
		public boolean random = false;
	}

	@SubscribeEvent
	public static void onConfigChanged(OnConfigChangedEvent event)
	{
		if (event.getModID().equalsIgnoreCase(ProportionalDestructionParticles.MOD_ID))
		{
			ConfigManager.sync(ProportionalDestructionParticles.MOD_ID, Type.INSTANCE);
		}
	}

}
