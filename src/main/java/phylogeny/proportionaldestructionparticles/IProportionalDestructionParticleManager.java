package phylogeny.proportionaldestructionparticles;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface IProportionalDestructionParticleManager
{

	void addDestructionParticle(BlockPos pos, IBlockState state, World world,
			double x, double y, double z, double xMotion, double yMotion, double zMotion);

}