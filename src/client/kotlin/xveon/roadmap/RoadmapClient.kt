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
import net.minecraft.particle.DustParticleEffect
import net.minecraft.particle.ParticleTypes
import net.minecraft.text.Text
import org.joml.Vector3f
import org.lwjgl.glfw.GLFW
import org.slf4j.LoggerFactory
import kotlin.math.sqrt
import kotlin.random.Random

object RoadmapClient : ClientModInitializer {
    val logger = LoggerFactory.getLogger("roadmap")
    private lateinit var kbScan: KeyBinding
    private lateinit var kbUi: KeyBinding
    private var roadmap = ScannedRoadmap()
    private var ui = RoadmapGui()
    private var masterTickCount = 0
    private var lastChunkPos = ChunkPos(0, 0)
    private var surroundingChunks = setOf<ScannedChunk>()

    override fun onInitializeClient() {
        // This entrypoint is suitable for setting up client-specific logic, such as rendering.
        logger.info("Initializing roadmap client...")

        Config.reloadFile()
        logger.info("Loaded config")

        if (FileSys.containsFiles(Constants.OUTPUT_PATH)) {
            roadmap = ScannedRoadmap.readFiles()
            logger.info("Loaded ${roadmap.getBlockCount()} road blocks and ${roadmap.markers.count()} markers")
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
            if (Config["draw_road_particles"] as Boolean) drawRoadParticles(client)
            if (Config["draw_marker_particles"] as Boolean) drawMarkerParticles(client)
            while (kbScan.wasPressed()) scanSurroundingRoads(client)
            while (kbUi.wasPressed()) openUi(client)
        })
    }

    fun drawRoadParticles(client: MinecraftClient) {
        val player: ClientPlayerEntity = client.player ?: return
        val particleManager = client.particleManager
        val rand = Random(masterTickCount)

        for (chunk in surroundingChunks) {
            for (block in chunk.blocks.values) {
                val distFromPlayer = sqrt(block.pos.getSquaredDistance(player.pos))
                if (rand.nextInt(distFromPlayer.toInt().coerceIn(1, 40)) != 0) continue

                particleManager.addParticle(
                    DustParticleEffect(Vector3f(0.0F, 1.0F, 0.0F), 1.0F),
                    block.pos.x.toDouble() + rand.nextDouble(),
                    block.pos.y.toDouble() + 1,
                    block.pos.z.toDouble() + rand.nextDouble(),
                    0.0, 0.0, 0.0
                )
            }
        }
    }

    fun drawMarkerParticles(client: MinecraftClient) {
        val player: ClientPlayerEntity = client.player ?: return
        val particleManager = client.particleManager
        val rand = Random(masterTickCount)

        val particleRadius = Config["particle_radius"] as Int
        val markerHeight = Constants.MARKER_HEIGHT
        for (marker in roadmap.markers) {
            val distFromPlayer = sqrt(marker.pos.getSquaredDistance(player.pos))
            if (distFromPlayer > particleRadius) continue
            val posX = marker.pos.x.toDouble()
            val posY = marker.pos.y.toDouble()
            val posZ = marker.pos.z.toDouble()

            for (i in -markerHeight..markerHeight + 1) when (marker.type) {
                RoadmapMarkerType.CUTOFF_POINT -> {

                    particleManager.addParticle(
                        ParticleTypes.SMOKE,
                        posX + rand.nextDouble(),
                        posY + rand.nextDouble(-markerHeight.toDouble(), markerHeight.toDouble() + 1),
                        posZ + rand.nextDouble(),
                        0.0, 0.0, 0.0
                    )

                } RoadmapMarkerType.PATHFINDER_GOAL -> {

                    particleManager.addParticle(
                        ParticleTypes.SCRAPE,
                        posX + rand.nextDouble(),
                        posY + rand.nextDouble(-markerHeight.toDouble(), markerHeight.toDouble() + 1),
                        posZ + rand.nextDouble(),
                        0.0, 0.0, 0.0
                    )

                } RoadmapMarkerType.SCAN_FENCE -> {

                    particleManager.addParticle(
                        ParticleTypes.DRIPPING_OBSIDIAN_TEAR,
                        posX + rand.nextDouble(),
                        posY + rand.nextDouble(-markerHeight.toDouble(), markerHeight.toDouble() + 1),
                        posZ + rand.nextDouble(),
                        0.0, 0.0, 0.0
                    )

                }
            }
        }
    }

    fun handleClientStarted(client: MinecraftClient) {
        // TODO: Do something with this
    }

    fun handleJoinWorld(networkHandler: ClientPlayNetworkHandler, packetSender: PacketSender, client: MinecraftClient) {
        // TODO: Do something with this
    }

    fun updateSurroundingChunks(client: MinecraftClient, force: Boolean = false) {
        val player: ClientPlayerEntity = client.player ?: return

        val currentChunkPos = ChunkPos.fromBlockPos(player.blockPos)
        if ((currentChunkPos != lastChunkPos) or force) {
            surroundingChunks = roadmap.getChunksInRadius(currentChunkPos, (Config["particle_radius"] as Int) / Constants.CHUNK_SIZE)
            lastChunkPos = currentChunkPos
        }
    }

    fun scanSurroundingRoads(client: MinecraftClient) {
        val player: ClientPlayerEntity = client.player ?: return

        val scanner = RoadmapScanner(roadmap)
        scanner.scan(player)
        // roadmap.saveState()
        roadmap.writeFiles()

        notifyPlayer("Scan completed.", player)

        updateSurroundingChunks(client, true)
    }

    fun undoLastScan(client: MinecraftClient) {
        val player: ClientPlayerEntity = client.player ?: return

        val undoSuccessful = false // roadmap.restoreState()
        if (undoSuccessful) {
            roadmap.writeFiles()
            updateSurroundingChunks(client, true)
            notifyPlayer("Last scan has been reverted.", player)
        } else {
            notifyPlayer("No saved state to revert to!", player)
        }
    }

    fun clearSurroundingChunks(client: MinecraftClient) {
        val player: ClientPlayerEntity = client.player ?: return

        val startChunkCount = roadmap.chunks.count()
        for (chunk in roadmap.getChunksInRadius(ChunkPos.fromBlockPos(player.blockPos), 1))
            roadmap.removeChunk(chunk.pos)
        roadmap.writeFiles()
        val endChunkCount = roadmap.chunks.count()

        val removedChunks = startChunkCount - endChunkCount
        if (removedChunks > 0) {
            notifyPlayer("Removed $removedChunks chunks from scan data.", player)
        } else {
            notifyPlayer("No chunks were removed from scan data.", player)
        }

        updateSurroundingChunks(client, true)
    }

    fun findUnscannedRoads(client: MinecraftClient) {
        val player: ClientPlayerEntity = client.player ?: return

        val posGroups = PosGroupCollection(32)
        for (marker in roadmap.getMarkers(RoadmapMarkerType.CUTOFF_POINT))
            posGroups.addPos(marker.pos)

        var result = ""
        for (pos in posGroups.getCenterPositions())
            result += ", (${pos.x}, ${pos.y}, ${pos.z})"
        if (result.isNotEmpty())
            messagePlayer("Positions of all unscanned road tails: ${result.substring(2)}.", player)
        else
            messagePlayer("Could not find any unscanned road tails.", player)
    }

    fun reloadData(client: MinecraftClient) {
        val player: ClientPlayerEntity = client.player ?: return

        BlockStateCache.clearBlockStates()
        roadmap = ScannedRoadmap.readFiles()
        Config.reloadFile()
        notifyPlayer("Reloaded scan data and config.", player)

        updateSurroundingChunks(client, true)
    }
    
    fun clearRoadmap(client: MinecraftClient) {
        val player: ClientPlayerEntity = client.player ?: return

        BlockStateCache.clearBlockStates()
        roadmap.clearMarkers()
        roadmap.clearChunks()
        notifyPlayer("Cleared map data.", player)

        updateSurroundingChunks(client, true)
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
