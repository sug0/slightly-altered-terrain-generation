package party.lemons.satg.mixins;

import net.minecraft.util.math.noise.SimplexNoiseSampler;
import net.minecraft.world.IWorld;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.gen.chunk.*;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(OverworldChunkGenerator.class)
public abstract class OverworldChunkGeneratorMixin extends SurfaceChunkGenerator<OverworldChunkGeneratorConfig>
{
	@Shadow public abstract int getSeaLevel();
	@Shadow public abstract double sampleNoise(int x, int y);

	@Shadow @Final private boolean amplified;
	@Shadow @Final private static float[] BIOME_WEIGHT_TABLE;

	public OverworldChunkGeneratorMixin(IWorld world, BiomeSource biomeSource, int verticalNoiseResolution, int horizontalNoiseResolution, int worldHeight, OverworldChunkGeneratorConfig config, boolean useSimplexNoise)
	{
		super(world, biomeSource, verticalNoiseResolution, horizontalNoiseResolution, worldHeight, config, useSimplexNoise);
	}


	@Overwrite
	public double[] computeNoiseRange(int x, int z)
	{
		double[] noiseRange = new double[2];
		float f = 0.0F;
		float g = 0.0F;
		float h = 0.0F;
		int seaLevel = this.getSeaLevel();

		Biome baseBiome = this.biomeSource.getBiomeForNoiseGen(x, seaLevel, z);
		float baseDepth = baseBiome.getDepth();
		if(!keepBiomeProportions(baseBiome))
			baseDepth /= 2;

		for(int xOffset = -2; xOffset <= 2; ++xOffset) {
			for(int zOffset = -2; zOffset <= 2; ++zOffset) {
				Biome biome = this.biomeSource.getBiomeForNoiseGen(x + xOffset, seaLevel,z + zOffset);

				float originalDepth = keepBiomeProportions(biome) ? biome.getDepth() : getAdjustedDepth(biome, x, z, xOffset, zOffset);
				float depth = originalDepth;
				float scale = keepBiomeProportions(biome) ? biome.getScale() : getAdjustedScale(biome, x, z, xOffset, zOffset);

				if (this.amplified && depth > 0.0F) {
					depth = 1.0F + depth * 2.0F;
					scale = 1.0F + scale * 4.0F;
				}

				float p = BIOME_WEIGHT_TABLE[xOffset + 2 + (zOffset + 2) * 5] / (depth + 2.0F);
				if (originalDepth > baseDepth)
				{
					p /= 2.0F;
				}

				f += scale * p;
				g += depth * p;
				h += p;
			}
		}

		f /= h;
		g /= h;
		f = f * 0.9F + 0.1F;
		g = (g * 4.0F - 1.0F) / 8.0F;
		noiseRange[0] = (double)g + this.sampleNoise(x, z);
		noiseRange[1] = f;
		return noiseRange;
	}

	public float getAdjustedDepth(Biome biome, int x, int z, int xOffset, int zOffset)
	{
		if(keepBiomeProportions(biome))
			return biome.getDepth();

		Biome stealBiome = this.biomeSource.getBiomeForNoiseGen(x + xOffset + 99999, getSeaLevel(), z + zOffset + 99999);
		return keepBiomeProportions(stealBiome) ? biome.getDepth() : stealBiome.getDepth();
	}

	public float getAdjustedScale(Biome biome, int x, int z, int xOffset, int zOffset)
	{
		if(keepBiomeProportions(biome))
			return biome.getDepth();

		Biome stealBiome = this.biomeSource.getBiomeForNoiseGen(x + xOffset + 99999, getSeaLevel(),z + zOffset + 99999);
		if(keepBiomeProportions(stealBiome))
			return biome.getScale();
		else
		{
			float adjustedScale = (float) Math.abs(sampleNoise(x + xOffset, z + zOffset) * 15F);
			return stealBiome.getScale() + adjustedScale;
		}
	}

	public boolean keepBiomeProportions(Biome biome)
	{
		Biome.Category category = biome.getCategory();
		return category == Biome.Category.OCEAN || category == Biome.Category.RIVER || category == Biome.Category.BEACH;
	}
}

