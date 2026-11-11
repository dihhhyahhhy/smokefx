package dev.smokefx;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class SmokeFxClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		SmokeFxConfig.load();

		ClientTickEvents.END_CLIENT_TICK.register(SmokeEngine::tick);

		ClientCommandRegistrationCallback.EVENT.register(
			(dispatcher, registryAccess) -> SmokeFxCommand.register(dispatcher));
	}
}
