package dev.smokefx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;

/**
 * Works like vanilla's "animate tick": every client tick it checks a bunch of random
 * positions around the player, and spawns extra particles where it finds something hot.
 * Everything is client-side; nothing is sent to or needed on a server.
 */
public final class SmokeEngine {

	// Close to the player we sample densely so nearby fires look good,
	// further out we sample sparsely so distant fires still wisp a little.
	private static final int NEAR_RADIUS = 5;
	private static final int NEAR_SAMPLES = 250;
	private static final int FAR_SAMPLES = 900;

	private static final BlockPos.MutableBlockPos CURSOR = new BlockPos.MutableBlockPos();

	private static SmokeFxConfig cfg = SmokeFxConfig.get();
	private static int budget;

	private SmokeEngine() {
	}

	public static void tick(Minecraft client) {
		cfg = SmokeFxConfig.get();
		ClientLevel level = client.level;
		var player = client.player;

		if (level == null || player == null || client.isPaused()) {
			return;
		}
		if (!cfg.enabled || cfg.particleMultiplier <= 0f || cfg.maxParticlesPerTick <= 0) {
			return;
		}

		budget = cfg.maxParticlesPerTick;
		RandomSource random = level.getRandom();
		BlockPos center = player.blockPosition();

		scan(level, center, Math.min(NEAR_RADIUS, cfg.scanRadius), NEAR_SAMPLES, random);
		scan(level, center, cfg.scanRadius, FAR_SAMPLES, random);

		if (on("burning_entities")) {
			burningEntities(level, player, random);
		}
	}

	// ------------------------------------------------------------------ scanning

	private static void scan(ClientLevel level, BlockPos center, int radius, int samples, RandomSource r) {
		int span = radius * 2 + 1;
		for (int i = 0; i < samples; i++) {
			CURSOR.set(
				center.getX() + r.nextInt(span) - radius,
				center.getY() + r.nextInt(span) - radius,
				center.getZ() + r.nextInt(span) - radius
			);
			BlockState state = level.getBlockState(CURSOR);
			if (!state.isAir()) {
				inspect(level, CURSOR, state, r);
			}
		}
	}

	private static void inspect(ClientLevel level, BlockPos pos, BlockState state, RandomSource r) {
		if (state.is(BlockTags.FIRE)) {
			if (on("fire")) {
				fire(level, pos, state, r);
			}
			return;
		}

		if (state.is(Blocks.LAVA)) {
			if (on("lava")) {
				lava(level, pos, r);
			}
			return;
		}

		if (state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH)
			|| state.is(Blocks.SOUL_TORCH) || state.is(Blocks.SOUL_WALL_TORCH)
			|| state.is(Blocks.COPPER_TORCH) || state.is(Blocks.COPPER_WALL_TORCH)) {
			if (on("torches")) {
				torch(level, pos, state, r);
			}
			return;
		}

		// Everything below only matters while lit.
		if (state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT)) {
			if (state.is(BlockTags.CAMPFIRES)) {
				if (on("campfires")) {
					campfire(level, pos, r);
				}
			} else if (state.is(BlockTags.CANDLES)) {
				if (on("candles")) {
					candle(level, pos, r);
				}
			} else if (state.is(Blocks.FURNACE) || state.is(Blocks.BLAST_FURNACE) || state.is(Blocks.SMOKER)) {
				if (on("furnaces")) {
					furnace(level, pos, r);
				}
			}
		}
	}

	// ------------------------------------------------------------------ effects
	// burst(level, random, particle, amount, centerX, bottomY, centerZ, radiusXZ, height, rise, drift)
	// "amount" is the base number of particles per hit; the player's multiplier scales it.

	private static void fire(ClientLevel level, BlockPos pos, BlockState state, RandomSource r) {
		double cx = pos.getX() + 0.5;
		double cy = pos.getY() + 0.2;
		double cz = pos.getZ() + 0.5;
		boolean soul = state.is(Blocks.SOUL_FIRE);

		burst(level, r, ParticleTypes.SMOKE, 1.0, cx, cy, cz, 0.3, 0.7, 0.04, 0.02);
		burst(level, r, ParticleTypes.LARGE_SMOKE, 0.5, cx, cy, cz, 0.3, 0.7, 0.03, 0.02);
		burst(level, r, soul ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME, 0.3, cx, cy, cz, 0.25, 0.6, 0.05, 0.03);
	}

	private static void torch(ClientLevel level, BlockPos pos, BlockState state, RandomSource r) {
		double x = pos.getX() + 0.5;
		double y = pos.getY() + 0.7;
		double z = pos.getZ() + 0.5;
		if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
			// Wall torches: flame sits out from the wall.
			Direction out = state.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite();
			x += 0.27 * out.getStepX();
			y += 0.22;
			z += 0.27 * out.getStepZ();
		}
		burst(level, r, ParticleTypes.SMOKE, 0.5, x, y, z, 0.05, 0.1, 0.02, 0.01);
	}

	private static void campfire(ClientLevel level, BlockPos pos, RandomSource r) {
		double cx = pos.getX() + 0.5;
		double cy = pos.getY() + 0.6;
		double cz = pos.getZ() + 0.5;
		burst(level, r, ParticleTypes.CAMPFIRE_COSY_SMOKE, 0.5, cx, cy, cz, 0.25, 0.3, 0.07, 0.01);
		burst(level, r, ParticleTypes.LARGE_SMOKE, 0.5, cx, cy, cz, 0.25, 0.3, 0.04, 0.02);
		burst(level, r, ParticleTypes.FLAME, 0.4, cx, cy, cz, 0.2, 0.3, 0.06, 0.03);
	}

	private static void candle(ClientLevel level, BlockPos pos, RandomSource r) {
		burst(level, r, ParticleTypes.SMOKE, 0.15, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0.15, 0.2, 0.01, 0.005);
	}

	private static void furnace(ClientLevel level, BlockPos pos, RandomSource r) {
		if (!level.getBlockState(pos.above()).isAir()) {
			return; // chimney is blocked
		}
		double cx = pos.getX() + 0.5;
		double cy = pos.getY() + 1.0;
		double cz = pos.getZ() + 0.5;
		burst(level, r, ParticleTypes.SMOKE, 1.0, cx, cy, cz, 0.25, 0.1, 0.04, 0.01);
		burst(level, r, ParticleTypes.LARGE_SMOKE, 0.3, cx, cy, cz, 0.25, 0.1, 0.03, 0.01);
	}

	private static void lava(ClientLevel level, BlockPos pos, RandomSource r) {
		if (!level.getBlockState(pos.above()).isAir()) {
			return; // only the exposed surface
		}
		double cx = pos.getX() + 0.5;
		double cy = pos.getY() + 1.0;
		double cz = pos.getZ() + 0.5;
		burst(level, r, ParticleTypes.SMOKE, 0.5, cx, cy, cz, 0.5, 0.1, 0.015, 0.01);
		burst(level, r, ParticleTypes.LARGE_SMOKE, 0.1, cx, cy, cz, 0.5, 0.1, 0.02, 0.01);
		burst(level, r, ParticleTypes.FLAME, 0.1, cx, cy, cz, 0.4, 0.1, 0.06, 0.03);
	}

	private static void burningEntities(ClientLevel level, Entity viewer, RandomSource r) {
		AABB area = viewer.getBoundingBox().inflate(cfg.scanRadius * 2.0);
		for (Entity e : level.getEntities((Entity) null, area)) {
			if (e.isRemoved() || !e.isOnFire()) {
				continue;
			}
			if (e == viewer && !cfg.affectLocalPlayer) {
				continue;
			}
			double half = e.getBbWidth() * 0.5;
			double height = e.getBbHeight();
			burst(level, r, ParticleTypes.SMOKE, 0.8, e.getX(), e.getY(), e.getZ(), half, height, 0.04, 0.03);
			burst(level, r, ParticleTypes.LARGE_SMOKE, 0.3, e.getX(), e.getY(), e.getZ(), half, height, 0.03, 0.02);
		}
	}

	// ------------------------------------------------------------------ helpers

	private static boolean on(String effect) {
		return cfg.isEffectEnabled(effect);
	}

	/** Turns 1.5 into 1 or 2 (50/50), 0.25 into 0 or 1 (25% chance of 1), and so on. */
	private static int count(RandomSource r, double baseAmount) {
		double scaled = baseAmount * cfg.particleMultiplier;
		int whole = (int) scaled;
		if (r.nextDouble() < scaled - whole) {
			whole++;
		}
		return whole;
	}

	private static void burst(ClientLevel level, RandomSource r, ParticleOptions type, double amount,
	                          double cx, double bottomY, double cz,
	                          double radiusXZ, double height, double rise, double drift) {
		for (int i = count(r, amount); i > 0; i--) {
			emit(level, type,
				cx + (r.nextDouble() - 0.5) * 2.0 * radiusXZ,
				bottomY + r.nextDouble() * height,
				cz + (r.nextDouble() - 0.5) * 2.0 * radiusXZ,
				(r.nextDouble() - 0.5) * 2.0 * drift,
				rise * (0.7 + r.nextDouble() * 0.6),
				(r.nextDouble() - 0.5) * 2.0 * drift);
		}
	}

	private static void emit(ClientLevel level, ParticleOptions type,
	                         double x, double y, double z, double dx, double dy, double dz) {
		if (budget <= 0) {
			return;
		}
		budget--;
		level.addParticle(type, x, y, z, dx, dy, dz);
	}
}
