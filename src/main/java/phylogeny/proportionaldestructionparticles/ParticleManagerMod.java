package phylogeny.proportionaldestructionparticles;

import java.util.List;

import com.google.common.collect.Lists;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.particle.ParticleDigging;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import phylogeny.proportionaldestructionparticles.api.ChiselsAndBitsAPIProxy;

public class ParticleManagerMod extends ParticleManager implements IProportionalDestructionParticleManager
{

	public ParticleManagerMod(World world, TextureManager renderer)
	{
		super(world, renderer);
	}

	@Override
	public void addBlockDestroyEffects(BlockPos pos, IBlockState state)
	{
		if (addBlockDestroyEffects(pos, state, world, this, 4))
			super.addBlockDestroyEffects(pos, state);
	}

	public static boolean addBlockDestroyEffects(BlockPos pos, IBlockState state, World world, IProportionalDestructionParticleManager particleManager, double particlesPerAxis)
	{
		// Spawn block destruction particles normally if mod functionality is disabled
		if (!ConfigMod.CLIENT.enabled)
		{
			return true;
		}

		state = state.getActualState(world, pos);
		IBlockState stateParticle = null;
		if (ChiselsAndBitsAPIProxy.isBlockChiseled(world, pos))
		{
			if (!ConfigMod.CLIENT.particlesPerBit)
				stateParticle = ChiselsAndBitsAPIProxy.getPrimaryState(world, pos, state);
		}
		else
		{
			if (state.getBlock().addDestroyEffects(world, pos, (ParticleManager) particleManager))
			{
				return false;
			}
			stateParticle = state;
		}

		// Attempt to spawn particles if block is not air and if not canceled by the block
		if (!state.getBlock().isAir(state, world, pos))
		{
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
				if ((!ConfigMod.CLIENT.collisionBoxes && Block.FULL_BLOCK_AABB.equals(bounds)) || bounds == null)
				{
					return true;
				}
				masks.add(bounds.offset(pos));
			}

			if (stateParticle == null)
			{
				return !ChiselsAndBitsAPIProxy.spawnDestructionParticlesPerBit(world, pos, particleManager, particlesPerAxis, masks);
			}
			// Attempt to randomly spawn a number of particles in each mask proportional to its size
			if (ConfigMod.CLIENT.random)
			{
				double volumeTotal = 1;
				double d0, d1, d2, dx, dy, dz, count;
				double particlesCountTotal = particlesPerAxis * particlesPerAxis * particlesPerAxis;
				for (AxisAlignedBB mask : masks)
				{
					// Grow box to allow increased number of particles, and revert offset in preparation for spawn area limiting
					mask = mask.grow(ConfigMod.CLIENT.boxGrowth).offset(-pos.getX(), -pos.getY(), -pos.getZ());

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
						particleManager.addDestructionParticle(pos, stateParticle, world, d0 + pos.getX(), d1 + pos.getY(), d2 + pos.getZ(), d0 - 0.5, d1 - 0.5, d2 - 0.5);
					}
				}
				return false;
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

						for (AxisAlignedBB mask : masks)
						{
							// Only spawn particle if it is inside an expanded (to increase the number of particles) version of one of the masks
							if (maskContainsVector(x, y, z, mask.grow(ConfigMod.CLIENT.boxGrowth)))
							{
								particleManager.addDestructionParticle(pos, stateParticle, world, x, y, z, d0 - 0.5, d1 - 0.5, d2 - 0.5);
								break;
							}
						}
					}
				}
			}
		}
		return false;
	}

	/**
	 * Modified version of {@link net.minecraft.util.math.AxisAlignedBB#contains contains} in AxisAlignedBB
	 */
	public static boolean maskContainsVector(double x, double y, double z, AxisAlignedBB mask)
	{
		if (x > mask.minX && x < mask.maxX)
		{
			if (y > mask.minY && y < mask.maxY)
				return z > mask.minZ && z < mask.maxZ;
			
			return false;
		}
		return false;
	}

	@Override
	public void addDestructionParticle(BlockPos pos, IBlockState state, World world,
			double x, double y, double z, double xMotion, double yMotion, double zMotion)
	{
		addEffect((new ParticleDiggingMod(world, x, y, z, xMotion, yMotion, zMotion, state)).setBlockPos(pos));
	}

	/**
	 * Extended version of {@link net.minecraft.client.particle.ParticleDigging ParticleDigging} that gives access to the needed protected constructor
	 */
	private static class ParticleDiggingMod extends ParticleDigging
	{

		protected ParticleDiggingMod(World world, double xCoord, double yCoord, double zCoord, double xSpeed, double ySpeed, double zSpeed, IBlockState state)
		{
			super(world, xCoord, yCoord, zCoord, xSpeed, ySpeed, zSpeed, state);
		}

	}
	
}