package phylogeny.proportionaldestructionparticles;

import com.TominoCZ.FBP.FBP;
import com.TominoCZ.FBP.particle.FBPParticleDigging;
import com.TominoCZ.FBP.particle.FBPParticleManager;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.IParticleFactory;
import net.minecraft.client.particle.ParticleDigging;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class FBPParticleManagerMod extends FBPParticleManager implements IProportionalDestructionParticleManager
{

	public FBPParticleManagerMod()
	{
		this(null, null, null);
	}

	public FBPParticleManagerMod(World world, TextureManager renderer, IParticleFactory particleFactory)
	{
		super(world, renderer, particleFactory);
	}

	public static void init()
	{
		if (FBP.enabled)
			MinecraftForge.EVENT_BUS.register(new FBPParticleManagerMod());
	}

	@SideOnly(Side.CLIENT)
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onEntityJoinWorldEvent(EntityJoinWorldEvent event)
	{
		if (event.getEntity() == Minecraft.getMinecraft().player)
		{
			FBP.fancyEffectRenderer = new FBPParticleManagerMod(event.getWorld(), Minecraft.getMinecraft().renderEngine, new ParticleDigging.Factory());
			if (FBP.enabled)
			{
				Minecraft.getMinecraft().effectRenderer = FBP.fancyEffectRenderer;
			}
		}
	}

	@Override
	public void addBlockDestroyEffects(BlockPos pos, IBlockState state)
	{
		Block block = state.getBlock();
		if ((!(block instanceof BlockLiquid) && !(FBP.frozen && !FBP.spawnWhileFrozen))
				&& (FBP.spawnRedstoneBlockParticles || block != Blocks.REDSTONE_BLOCK)
				&& !FBP.INSTANCE.isBlacklisted(block, true) && block != FBP.FBPBlock)
		{
			if (!FBP.enabled || ParticleManagerMod.addBlockDestroyEffects(pos, state, world, this, FBP.particlesPerAxis))
				super.addBlockDestroyEffects(pos, state);
		}
	}

	@Override
	public void addDestructionParticle(BlockPos pos, IBlockState state, World world,
			double x, double y, double z, double xMotion, double yMotion, double zMotion)
	{
		try
		{
			addEffect(new FBPParticleDiggingMod(world, x, y, z, xMotion, -0.001, zMotion, state));
		}
		catch (Throwable e) {}
	}

	/**
	 * Extended version of {@link net.minecraft.client.particle.ParticleDigging ParticleDigging} that gives access to the needed protected constructor
	 */
	private class FBPParticleDiggingMod extends FBPParticleDigging
	{

		protected FBPParticleDiggingMod(World world, double xCoord, double yCoord, double zCoord, double xSpeed, double ySpeed, double zSpeed,
				IBlockState state)
		{
			super(world, xCoord, yCoord, zCoord, xSpeed, ySpeed, zSpeed, 1, 1, 1, state, null, (float) FBP.random.nextDouble(0.75, 1),
					Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes().getTexture(state));
		}

	}

}