package phylogeny.proportionaldestructionparticles.api;

import java.util.List;

import mod.chiselsandbits.api.APIExceptions.CannotBeChiseled;
import mod.chiselsandbits.api.ChiselsAndBitsAddon;
import mod.chiselsandbits.api.IBitAccess;
import mod.chiselsandbits.api.IChiselAndBitsAPI;
import mod.chiselsandbits.api.IChiselsAndBitsAddon;
import mod.chiselsandbits.api.IMultiStateBlock;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import phylogeny.proportionaldestructionparticles.ConfigMod;
import phylogeny.proportionaldestructionparticles.IProportionalDestructionParticleManager;
import phylogeny.proportionaldestructionparticles.ParticleManagerMod;

/**
 * Provides access to C&B's API
 * <p>
 * Any class with {@link mod.chiselsandbits.api.ChiselsAndBitsAddon @ChiselsAndBitsAddon} that implements
 * {@link mod.chiselsandbits.api.IChiselsAndBitsAddon IChiselsAndBitsAddon} will allow API access.
 * This must separate from the main class (and must not be referenced) if C&B is not a required mod.
 */
@ChiselsAndBitsAddon
public class ChiselsAndBitsAPI implements IChiselsAndBitsAddon
{

	static IChiselAndBitsAPI api;

	@Override
	public void onReadyChiselsAndBits(IChiselAndBitsAPI api)
	{
		this.api = api;
		ChiselsAndBitsAPIProxy.apiPresent = true;
	}

	static IBlockState getPrimaryState(World world, BlockPos pos, IBlockState state)
	{
		return state.getBlock() instanceof IMultiStateBlock ? ((IMultiStateBlock) state.getBlock()).getPrimaryState(world, pos) : Blocks.STONE.getDefaultState();
	}

	static boolean spawnDestructionParticlesPerBit(World world, BlockPos pos, IProportionalDestructionParticleManager particleManager,
			double particlesPerAxis, List<AxisAlignedBB> masks)
	{
		IBitAccess bitAccess;
		try
		{
			bitAccess = api.getBitAccess(world, pos);
		}
		catch (CannotBeChiseled e)
		{
			return false;
		}
		// Attempt to randomly spawn a number of particles in each mask proportional to its size
		if (ConfigMod.CLIENT.random)
		{
			double volumeTotal = 1;
			double d0, d1, d2, dx, dy, dz, count;
			double particlesCountTotal = particlesPerAxis * particlesPerAxis * particlesPerAxis;
			AxisAlignedBB maskOriginal;
			for (AxisAlignedBB mask : masks)
			{
				// Grow box to allow increased number of particles, and revert offset in preparation for spawn area limiting
				maskOriginal = mask.offset(-pos.getX(), -pos.getY(), -pos.getZ());
				mask = maskOriginal.grow(ConfigMod.CLIENT.boxGrowth);

				// Calculate percentage of the block space that the mask is, and multiply by 64 (the total count per space)
				d0 = mask.maxX - mask.minX;
				d1 = mask.maxY - mask.minY;
				d2 = mask.maxZ - mask.minZ;
				count = Math.round(d0 * d1 * d2 / volumeTotal * particlesCountTotal);

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
					d0 = mask.minX + dx * world.rand.nextDouble();
					d1 = mask.minY + dy * world.rand.nextDouble();
					d2 = mask.minZ + dz * world.rand.nextDouble();
					particleManager.addDestructionParticle(pos, bitAccess.getBitAt((int) (MathHelper.clamp(d0, maskOriginal.minX, maskOriginal.maxX) * 16),
							(int) (MathHelper.clamp(d1, maskOriginal.minY, maskOriginal.maxY) * 16),
							(int) (MathHelper.clamp(d2, maskOriginal.minZ, maskOriginal.maxZ) * 16)).getState(),
							world, d0 + pos.getX(), d1 + pos.getY(), d2 + pos.getZ(), d0 - 0.5, d1 - 0.5, d2 - 0.5);
				}
			}
			return true;
		}

		// Attempt to spawn up to 64 particles within the block space
		double d0, d1, d2, x, y, z;
		for (int j = 0; j < particlesPerAxis; ++j)
		{
			for (int k = 0; k < particlesPerAxis; ++k)
			{
				for (int l = 0; l < particlesPerAxis; ++l)
				{
					d0 = (j + 0.5D) / particlesPerAxis;
					d1 = (k + 0.5D) / particlesPerAxis;
					d2 = (l + 0.5D) / particlesPerAxis;
					x = d0 + pos.getX();
					y = d1 + pos.getY();
					z = d2 + pos.getZ();
					AxisAlignedBB maskOriginal;
					for (AxisAlignedBB mask : masks)
					{
						// Only spawn particle if it is inside an expanded (to increase the number of particles) version of one of the masks
						maskOriginal = mask.offset(-pos.getX(), -pos.getY(), -pos.getZ());
						if (ParticleManagerMod.maskContainsVector(x, y, z, mask.grow(ConfigMod.CLIENT.boxGrowth)))
						{
							particleManager.addDestructionParticle(pos, bitAccess.getBitAt((int) (MathHelper.clamp(d0, maskOriginal.minX, maskOriginal.maxX) * 16),
									(int) (MathHelper.clamp(d1, maskOriginal.minY, maskOriginal.maxY) * 16),
									(int) (MathHelper.clamp(d2, maskOriginal.minZ, maskOriginal.maxZ) * 16)).getState(),
									world, x, y, z, d0 - 0.5, d1 - 0.5, d2 - 0.5);
							break;
						}
					}
				}
			}
		}
		return true;
	}

}