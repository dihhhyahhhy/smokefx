package dev.smokefx;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

/**
 * In-game settings:
 *   /smokefx                      show current settings
 *   /smokefx on | off             master switch
 *   /smokefx amount <0-5>         particle amount (1 = default, 2 = double, 0 = none)
 *   /smokefx radius <4-32>        how far around you effects are generated
 *   /smokefx limit <0-1000>       max particles this mod spawns per tick
 *   /smokefx effect <name> <true|false>
 *   /smokefx reload               re-read config/smokefx.json
 */
public final class SmokeFxCommand {

	private SmokeFxCommand() {
	}

	public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
		LiteralArgumentBuilder<FabricClientCommandSource> root = ClientCommands.literal("smokefx");

		root.executes(ctx -> status(ctx.getSource()));

		root.then(ClientCommands.literal("on").executes(ctx -> {
			SmokeFxConfig.get().enabled = true;
			return saved(ctx.getSource(), "SmokeFX enabled.");
		}));

		root.then(ClientCommands.literal("off").executes(ctx -> {
			SmokeFxConfig.get().enabled = false;
			return saved(ctx.getSource(), "SmokeFX disabled.");
		}));

		root.then(ClientCommands.literal("amount")
			.then(ClientCommands.argument("multiplier", FloatArgumentType.floatArg(0f, 5f))
				.executes(ctx -> {
					float value = FloatArgumentType.getFloat(ctx, "multiplier");
					SmokeFxConfig.get().particleMultiplier = value;
					return saved(ctx.getSource(), "Particle amount set to " + value + "x.");
				})));

		root.then(ClientCommands.literal("radius")
			.then(ClientCommands.argument("blocks", IntegerArgumentType.integer(4, 32))
				.executes(ctx -> {
					int value = IntegerArgumentType.getInteger(ctx, "blocks");
					SmokeFxConfig.get().scanRadius = value;
					return saved(ctx.getSource(), "Effect radius set to " + value + " blocks.");
				})));

		root.then(ClientCommands.literal("limit")
			.then(ClientCommands.argument("particles", IntegerArgumentType.integer(0, 1000))
				.executes(ctx -> {
					int value = IntegerArgumentType.getInteger(ctx, "particles");
					SmokeFxConfig.get().maxParticlesPerTick = value;
					return saved(ctx.getSource(), "Particle limit set to " + value + " per tick.");
				})));

		LiteralArgumentBuilder<FabricClientCommandSource> effect = ClientCommands.literal("effect");
		for (String name : SmokeFxConfig.EFFECTS) {
			effect.then(ClientCommands.literal(name)
				.then(ClientCommands.argument("enabled", BoolArgumentType.bool())
					.executes(ctx -> {
						boolean value = BoolArgumentType.getBool(ctx, "enabled");
						SmokeFxConfig.get().setEffect(name, value);
						return saved(ctx.getSource(), "Effect '" + name + "' " + (value ? "enabled." : "disabled."));
					})));
		}
		root.then(effect);

		root.then(ClientCommands.literal("reload").executes(ctx -> {
			SmokeFxConfig.load();
			ctx.getSource().sendFeedback(Component.literal("[SmokeFX] Config reloaded."));
			return Command.SINGLE_SUCCESS;
		}));

		dispatcher.register(root);
	}

	private static int saved(FabricClientCommandSource source, String message) {
		SmokeFxConfig.save();
		source.sendFeedback(Component.literal("[SmokeFX] " + message));
		return Command.SINGLE_SUCCESS;
	}

	private static int status(FabricClientCommandSource source) {
		SmokeFxConfig cfg = SmokeFxConfig.get();
		StringBuilder off = new StringBuilder();
		for (String name : SmokeFxConfig.EFFECTS) {
			if (!cfg.isEffectEnabled(name)) {
				if (off.length() > 0) {
					off.append(", ");
				}
				off.append(name);
			}
		}
		source.sendFeedback(Component.literal(
			"[SmokeFX] " + (cfg.enabled ? "ON" : "OFF")
				+ " | amount " + cfg.particleMultiplier + "x"
				+ " | radius " + cfg.scanRadius
				+ " | limit " + cfg.maxParticlesPerTick + "/tick"
				+ " | disabled effects: " + (off.length() == 0 ? "none" : off)));
		return Command.SINGLE_SUCCESS;
	}
}
