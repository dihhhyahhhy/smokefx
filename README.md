# SmokeFX

Client-side visual effects for Minecraft Java Edition (Fabric). Adds extra smoke, sparks and
heat wisps to:

- fire and soul fire
- torches (wall and standing, including soul and copper torches) and lit candles
- lit campfires
- lit furnaces, blast furnaces and smokers
- exposed lava
- burning mobs and players

Everything is purely visual and client-side, so it works on any server (including vanilla)
and nothing needs to be installed on the server.

**Targets:** Minecraft 26.2, Fabric Loader 0.19.5+, Fabric API 0.154.0+26.2, Java 25.

## Using it in game

Install Fabric Loader and Fabric API, drop the built jar into your `mods` folder.

| Command | What it does |
|---|---|
| `/smokefx` | Show current settings |
| `/smokefx amount <0-5>` | **Particle amount.** 1 = default, 2 = double, 0.5 = half, 0 = none |
| `/smokefx on` / `off` | Master switch |
| `/smokefx radius <4-32>` | How far around you effects are generated |
| `/smokefx limit <0-1000>` | Max particles this mod can spawn per tick (FPS safety cap) |
| `/smokefx effect <name> <true/false>` | Toggle one effect: `fire`, `torches`, `candles`, `campfires`, `furnaces`, `lava`, `burning_entities` |
| `/smokefx reload` | Re-read the config file |

Settings are saved to `.minecraft/config/smokefx.json`, which you can also edit by hand.
If the game gets laggy with a high amount, lower `amount`, `radius` or `limit`.

## Getting the jar without installing anything (GitHub Actions)

1. Create a new GitHub repository and upload everything in this folder (keep the `.github` folder).
2. Open the **Actions** tab, pick the **build** workflow and run it (it also runs on every push).
3. When it finishes, open the run and download the **smokefx-jar** artifact. Inside is `smokefx-1.0.0.jar`.
4. Put that jar in your `mods` folder next to Fabric API.

## Building


You need JDK 25.

**Option A, easiest (recommended):**
1. Go to https://fabricmc.net/develop/template/ and generate a project for Minecraft 26.2.
   Set the **Mod ID to `smokefx`** and leave "Split client and common sources" ticked.
2. Delete the generated `src` folder and copy this project's `src` folder in its place.
3. In that folder run `./gradlew build` (Windows: `gradlew build`).

**Option B, use the build files in this folder:**
1. Install Gradle, then run `gradle wrapper --gradle-version 9.5.1` once in this folder.
2. Run `./gradlew build`.

The jar ends up in `build/libs/smokefx-1.0.0.jar` (not the `-sources` one).
`./gradlew runClient` launches a test game with the mod loaded.

## Other Minecraft versions

Minecraft 26.1 and newer share this build setup, so for 26.1 / 26.3 just change
`minecraft_version`, `loader_version` and `fabric_api_version` in `gradle.properties`
(recommended values are on https://fabricmc.net/develop/). Versions before 26.1 are
obfuscated and need a different build script, so ask and I'll make a port.

## How it works

Every tick `SmokeEngine` checks random positions around you (like vanilla's own ambient
particles), and where it finds fire, lava, a lit furnace etc. it spawns extra particles.
`amount` scales how many particles each hit produces (fractions are handled
probabilistically, so 0.3x really does give about 30% as much).
