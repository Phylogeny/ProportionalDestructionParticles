package phylogeny.proportionaldestructionparticles.api;

import java.util.List;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import phylogeny.proportionaldestructionparticles.IProportionalDestructionParticleManager;

/**
 * Provides a means of accessing C&B's API without reference to API classes or imports. Doing so is required if C&B is not a required mod.
 */
public class ChiselsAndBitsAPIProxy
{

	static boolean apiPresent;

	public static boolean isApiPresent()
	{
		return apiPresent;
	}

	public static IBlockState getPrimaryState(World world, BlockPos pos, IBlockState state)
	{
		return apiPresent ? ChiselsAndBitsAPI.getPrimaryState(world, pos, state) : Blocks.STONE.getDefaultState();
	}

	public static boolean isBlockChiseled(World world, BlockPos pos)
	{
		return apiPresent ? ChiselsAndBitsAPI.api.isBlockChiseled(world, pos) : false;
	}

	public static boolean spawnDestructionParticlesPerBit(World world, BlockPos pos, IProportionalDestructionParticleManager particleManager,
			double particlesPerAxis, List<AxisAlignedBB> masks)
	{
		return apiPresent ? ChiselsAndBitsAPI.spawnDestructionParticlesPerBit(world, pos, particleManager, particlesPerAxis, masks) : false;
	}

}