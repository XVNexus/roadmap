package xveon.roadmap

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.ClientStarted
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayNetworkHandler
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.client.network.ServerAddress
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import net.minecraft.particle.ParticleTypes
import net.minecraft.text.Text
import org.lwjgl.glfw.GLFW
import org.slf4j.LoggerFactory
import kotlin.random.Random

object RoadmapClient : ClientModInitializer {
    val logger = LoggerFactory.getLogger("roadmap")
    private lateinit var kbScan: KeyBinding
    private lateinit var kbUi: KeyBinding
    private var scannedRoadmap = ScannedRoadmap()
    private var ui = RoadmapUi()
    private var masterTickCount = 0
    private var lastChunkPos = ChunkPos(0, 0)
    private var surroundingChunks = setOf<ScannedChunk>()

    override fun onInitializeClient() {
        // This entrypoint is suitable for setting up client-specific logic, such as rendering.
        logger.info("Initializing roadmap client...")

        Config.reloadFile()
        logger.info("Loaded config")

        if (FileSys.containsFiles(Constants.OUTPUT_PATH)) {
            scannedRoadmap = ScannedRoadmap.readFiles()
            logger.info("Loaded ${scannedRoadmap.getBlockCount()} previously scanned blocks.")
        }

        kbScan = KeyBindingHelper.registerKeyBinding(
            KeyBinding(
                "key.roadmap.scan",  // The translation key of the keybinding's name
                InputUtil.Type.KEYSYM,  // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
                GLFW.GLFW_KEY_R,  // The keycode of the key
                "category.roadmap.main" // The translation key of the keybinding's category.
            )
        )

        kbUi = KeyBindingHelper.registerKeyBinding(
            KeyBinding(
                "key.roadmap.ui",  // The translation key of the keybinding's name
                InputUtil.Type.KEYSYM,  // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
                GLFW.GLFW_KEY_G,  // The keycode of the key
                "category.roadmap.main" // The translation key of the keybinding's category.
            )
        )

        ClientLifecycleEvents.CLIENT_STARTED.register(ClientStarted { client: MinecraftClient ->
            handleClientStarted(client)
        })

        ClientPlayConnectionEvents.JOIN.register { networkHandler: ClientPlayNetworkHandler, packetSender: PacketSender, client: MinecraftClient ->
            handleJoinWorld(networkHandler, packetSender, client)
        }

        ClientTickEvents.END_CLIENT_TICK.register(ClientTickEvents.EndTick { client: MinecraftClient ->
            masterTickCount++
            if (masterTickCount % 20 == 0) updateSurroundingChunks(client)
            if (Config["draw_particles"] as Boolean) drawRoadParticles(client)
            while (kbScan.wasPressed()) scanSurroundingRoads(client)
            while (kbUi.wasPressed()) openUi(client)
        })
    }

    fun drawRoadParticles(client: MinecraftClient) {
        val particleManager = client.particleManager
        val rand = Random(masterTickCount)

        for (chunk in surroundingChunks) {
            for (block in chunk.blocks.values) if (block.isRoad) {
                if (rand.nextInt(40) != 0) continue

                particleManager.addParticle(
                    ParticleTypes.DRIPPING_OBSIDIAN_TEAR,
                    block.pos.x.toDouble() + rand.nextDouble(),
                    block.pos.y.toDouble() + 1,
                    block.pos.z.toDouble() + rand.nextDouble(),
                    0.0, 0.0, 0.0
                )
            }
        }
    }

    fun handleClientStarted(client: MinecraftClient) {
        // TODO: Do something with this
    }

    fun handleJoinWorld(networkHandler: ClientPlayNetworkHandler, packetSender: PacketSender, client: MinecraftClient) {
        // TODO: Do something with this
    }

    fun updateSurroundingChunks(client: MinecraftClient) {
        val player: ClientPlayerEntity = client.player ?: return

        val currentChunkPos = ChunkPos.fromBlockPos(player.blockPos)
        if (currentChunkPos != lastChunkPos) {
            surroundingChunks = scannedRoadmap.getChunksInRadius(currentChunkPos, Config["particle_chunk_radius"] as Int)
            lastChunkPos = currentChunkPos
        }
    }

    fun scanSurroundingRoads(client: MinecraftClient) {
        val player: ClientPlayerEntity = client.player ?: return
        logger.info("Scanning surrounding blocks...")

        val startBlockCount = scannedRoadmap.getBlockCount()
        val scanner = RoadmapScanner(scannedRoadmap)
        scanner.scan(player)
        scannedRoadmap.writeFiles()
        val endBlockCount = scannedRoadmap.getBlockCount()

        val newBlocks = endBlockCount - startBlockCount
        if (newBlocks > 0) {
            logger.info("Saved $newBlocks blocks to scan data")
            notifyPlayer("Saved $newBlocks blocks to scan data.", player)
        } else {
            logger.info("No new blocks were saved to scan data")
            notifyPlayer("No new blocks were saved to scan data.", player)
        }
    }

    fun clearSurroundingChunks(client: MinecraftClient) {
        val player: ClientPlayerEntity = client.player ?: return
        logger.info("Clearing surrounding chunk data...")

        val startChunkCount = scannedRoadmap.chunks.count()
        val playerChunkPos = ChunkPos.fromBlockPos(player.blockPos)
        for (z in playerChunkPos.z - 1..playerChunkPos.z + 1) {
            for (x in playerChunkPos.x - 1..playerChunkPos.x + 1) {
                scannedRoadmap.removeChunk(ChunkPos(x, z))
            }
        }
        scannedRoadmap.writeFiles()
        val endChunkCount = scannedRoadmap.chunks.count()

        val removedChunks = startChunkCount - endChunkCount
        if (removedChunks > 0) {
            logger.info("Removed $removedChunks chunks from scan data")
            notifyPlayer("Removed $removedChunks chunks from scan data.", player)
        } else {
            logger.info("No chunks were removed from scan data")
            notifyPlayer("No chunks were removed from scan data.", player)
        }
    }

    fun reloadFiles(client: MinecraftClient) {
        val player: ClientPlayerEntity = client.player ?: return
        BlockStateCache.clearBlockStates()
        scannedRoadmap = ScannedRoadmap.readFiles()
        Config.reloadFile()

        logger.info("Reloaded scan data and config")
        notifyPlayer("Reloaded scan data and config.", player)
    }

    fun undoLastScan(client: MinecraftClient) {
        val player: ClientPlayerEntity = client.player ?: return
        logger.warn("Undo feature not implemented!")
        notifyPlayer("The undo feature will be added in a future version.", player)
    }

    fun openUi(client: MinecraftClient) {
        val player: ClientPlayerEntity = client.player ?: return

        client.setScreenAndRender(ui)
    }

    fun messagePlayer(text: String, providedPlayer: ClientPlayerEntity? = null) {
        val player = providedPlayer ?: MinecraftClient.getInstance().player
        player?.sendMessage(Text.literal("Roadmap: $text"), false) ?: logger.error("Could not find player to send notification \"${text}\" to!")
    }

    fun notifyPlayer(text: String, providedPlayer: ClientPlayerEntity? = null) {
        val player = providedPlayer ?: MinecraftClient.getInstance().player
        player?.sendMessage(Text.literal("Roadmap: $text"), true) ?: logger.error("Could not find player to send notification \"${text}\" to!")
    }

    fun getServerIp(client: MinecraftClient): String {
        val player: ClientPlayerEntity = client.player ?: return ""
        val server = client.server ?: return ""
        return ServerAddress.parse(client.server!!.serverIp).address
    }
}
