package phylogeny.proportionaldestructionparticles;

import java.util.List;
import java.util.Random;

import org.apache.logging.log4j.Logger;

import com.google.common.collect.Lists;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleDigging;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.Config.Comment;
import net.minecraftforge.common.config.Config.LangKey;
import net.minecraftforge.common.config.Config.Name;
import net.minecraftforge.common.config.Config.RangeDouble;
import net.minecraftforge.common.config.Config.Type;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent.OnConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod(modid = ProportionalDestructionParticles.MOD_ID,
	 version = ProportionalDestructionParticles.VERSION,
	 acceptedMinecraftVersions = ProportionalDestructionParticles.MC_VERSIONS_ACCEPTED,
	 updateJSON = ProportionalDestructionParticles.UPDATE_JSON,
	 dependencies = ProportionalDestructionParticles.DEPENDENCIES,
	 clientSideOnly = ProportionalDestructionParticles.CLIENT_OLNY)
public class ProportionalDestructionParticles
{

	public static final String MOD_ID = "pdp";
	public static final String VERSION = "@VERSION@";
	public static final String UPDATE_JSON = "@UPDATE@";
	public static final String MC_VERSIONS_ACCEPTED = "[1.12.2,)";
	public static final String DEPENDENCIES = "before-client:cofhcore";
	public static final boolean CLIENT_OLNY = true;
	private static final String INFO_LANG_KEY = MOD_ID + ".logger.info.replacement";
	private static Random random = new Random();
	private static Logger logger;

	@EventHandler
	public void preInit(FMLPreInitializationEvent event)
	{
		logger = event.getModLog();
	}

	@EventHandler
	public void init(FMLInitializationEvent event)
	{
		String info = I18n.format(INFO_LANG_KEY);
		logger.info(info.equals(INFO_LANG_KEY) ? "Replacing Minecraft#effectRenderer - Block destruction particles now only spawn in blocks' collision/bounding boxes." : info);
		Minecraft.getMinecraft().effectRenderer = new ParticleManager(Minecraft.getMinecraft().world, Minecraft.getMinecraft().renderEngine)
		{

			@Override
			public void addBlockDestroyEffects(BlockPos pos, IBlockState state)
			{
				// Spawn block destruction particles normally if mod functionality is disabled
				if (!ConfigMod.CLIENT.enabled)
				{
					super.addBlockDestroyEffects(pos, state);
					return;
				}

				// Attempt to spawn particles if block is not air and if not canceled by the block
				if (!state.getBlock().isAir(state, world, pos) && !state.getBlock().addDestroyEffects(world, pos, this))
				{
					state = state.getActualState(world, pos);
					List<AxisAlignedBB> masks = Lists.newArrayList();

					// Attempt to use collision boxes as masks, doing so is if not disabled
					if (ConfigMod.CLIENT.collisionBoxes)
					{
						// Collect all collision boxes using an infinitely large mask
						state.addCollisionBoxToList(world, pos, TileEntity.INFINITE_EXTENT_AABB, masks, null, true);

						// Remove null or zero-volume boxes
						masks.removeIf(box -> box == null || box.maxX - box.minX == 0 || box.maxY - box.minY == 0 || box.maxZ - box.minZ == 0);
					}

					// Attempt to use bounding box as mask if no valid collision boxes were found
					if (masks.isEmpty())
					{
						AxisAlignedBB bounds = state.getBoundingBox(world, pos);

						// Spawn particles normally if bounding box is null, or if collision boxes are being ignored and it is a full block
						if ((!ConfigMod.CLIENT.collisionBoxes && Block.FULL_BLOCK_AABB.offset(pos).equals(bounds)) || bounds == null)
						{
							super.addBlockDestroyEffects(pos, state);
							return;
						}
						masks.add(bounds.offset(pos));
					}

					// Attempt to randomly spawn a number of particles in each mask proportional to its size
					if (ConfigMod.CLIENT.random)
					{
						double volumeTotal = 1;
						double d0, d1, d2, dx, dy, dz, count;
						for (AxisAlignedBB mask : masks)
						{
							// Grow box to allow increased number of particles, and revert offset in preparation for spawn area limiting
							mask = mask.grow(ConfigMod.CLIENT.boxGrowth).offset(-pos.getX(), -pos.getY(), -pos.getZ());

							// Calculate percentage of the block space that the mask is, and multiply by 64 (the total count per space)
							d0 = mask.maxX - mask.minX;
							d1 = mask.maxY - mask.minY;
							d2 = mask.maxZ - mask.minZ;
							count = Math.round(d0 * d1 * d2 / volumeTotal * 64.0);

							// Restore spawn area, now that the particles count is obtained
							mask = mask.shrink(ConfigMod.CLIENT.boxGrowth);

							// Limit spawn area to the area particles normally spawn in, so as to prevent them from clipping through blocks
							mask = new AxisAlignedBB(Math.max(mask.minX, 0.125), Math.max(mask.minY, 0.125), Math.max(mask.minZ, 0.125),
									Math.min(mask.maxX, 0.875), Math.min(mask.maxY, 0.875), Math.min(mask.maxZ, 0.875));

							// Update range
							dx = mask.maxX - mask.minX;
							dy = mask.maxY - mask.minY;
							dz = mask.maxZ - mask.minZ;

							// Spawn particles at random positions in the mask
							for (int i = 0; i < count; i++)
							{
								d0 = mask.minX + dx * random.nextDouble();
								d1 = mask.minY + dy * random.nextDouble();
								d2 = mask.minZ + dz * random.nextDouble();
								addEffect((new ParticleDiggingExtened(world, d0 + pos.getX(), d1 + pos.getY(), d2 + pos.getZ(), d0 - 0.5, d1 - 0.5, d2 - 0.5, state)).setBlockPos(pos));
							}
						}
						return;
					}

					// Attempt to spawn up to 64 particles within the block space
					double d0, d1, d2, x, y, z;
					for (int j = 0; j < 4; ++j)
					{
						for (int k = 0; k < 4; ++k)
						{
							for (int l = 0; l < 4; ++l)
							{
								d0 = (j + 0.5D) / 4.0D;
								d1 = (k + 0.5D) / 4.0D;
								d2 = (l + 0.5D) / 4.0D;
								x = d0 + pos.getX();
								y = d1 + pos.getY();
								z = d2 + pos.getZ();

								for (AxisAlignedBB mask : masks)
								{
									// Only spawn particle if it is inside an expanded (to increase the number of particles) version of one of the masks
									if (maskContainsVector(x, y, z, mask.grow(ConfigMod.CLIENT.boxGrowth)))
									{
										addEffect((new ParticleDiggingExtened(world, x, y, z, d0 - 0.5, d1 - 0.5, d2 - 0.5, state)).setBlockPos(pos));
										break;
									}
								}
							}
						}
					}
				}

			}

			/**
			 * Modified version of {@link net.minecraft.util.math.AxisAlignedBB#contains contains} in AxisAlignedBB
			 */
			private boolean maskContainsVector(double x, double y, double z, AxisAlignedBB mask)
			{
				if (x > mask.minX && x < mask.maxX)
				{
					if (y > mask.minY && y < mask.maxY)
						return z > mask.minZ && z < mask.maxZ;
					
					return false;
				}
				return false;
			}

		};
	}

	/**
	 * Extended version of {@link net.minecraft.client.particle.ParticleDigging ParticleDigging} that gives access to the needed protected constructor
	 */
	private class ParticleDiggingExtened extends ParticleDigging
	{

		protected ParticleDiggingExtened(World world, double xCoord, double yCoord, double zCoord, double xSpeed, double ySpeed, double zSpeed, IBlockState state)
		{
			super(world, xCoord, yCoord, zCoord, xSpeed, ySpeed, zSpeed, state);
		}

	}

	@Config(modid = MOD_ID)
	@LangKey(MOD_ID + ".config.title")
	@EventBusSubscriber(modid = MOD_ID, value = Side.CLIENT)
	public static class ConfigMod
	{

		@Name("Client")
		@Comment("Client-only configs.")
		@LangKey(MOD_ID + ".config.client")
		public static final Client CLIENT = new Client();

		public static class Client
		{
			@Name("Enabled")
			@Comment("If true, block destruction particles will spawn normally (throughout the block space), otherwise they will only spawn in blocks' collision/bounding boxes.")
			@LangKey(MOD_ID + ".config.client.enabled")
			public boolean enabled = true;

			@Name("Collision Boxes")
			@Comment("If true, block destruction particles only spawn in the collision boxes of blocks that have them, otherwise they will only spawn in their bounding boxes.")
			@LangKey(MOD_ID + ".config.client.collision")
			public boolean collisionBoxes = true;

			@Name("Box Growth")
			@Comment("The boxes that block destruction particles are allowed to spawn in is expanded by this many meters.")
			@LangKey(MOD_ID + ".config.client.growth")
			@RangeDouble(min = 0)
			public double boxGrowth = 0.1;

			@Name("Random")
			@Comment("If true, block destruction particles spawn randomly within blocks' collision/bounding boxes, otherwise they will spawn at evenly spaced, "
					+ "pre-determined positions that fall withing expanded versions of those boxes.")
			@LangKey(MOD_ID + ".config.client.random")
			public boolean random = false;
		}

		@SubscribeEvent
		public static void onConfigChanged(OnConfigChangedEvent event)
		{
			if (event.getModID().equalsIgnoreCase(MOD_ID))
			{
				ConfigManager.sync(MOD_ID, Type.INSTANCE);
			}
		}

	}

}