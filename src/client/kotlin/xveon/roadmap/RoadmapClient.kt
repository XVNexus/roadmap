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
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.RaycastContext
import org.joml.Vector3f
import org.lwjgl.glfw.GLFW
import org.slf4j.LoggerFactory
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

object RoadmapClient : ClientModInitializer {
    val logger = LoggerFactory.getLogger("roadmap")
    private lateinit var kbPrimary: KeyBinding
    private lateinit var kbSecondary: KeyBinding
    private var roadmap = Roadmap()
    private var ui = RoadmapGui()
    private var masterTickCount = 0
    private var lastChunkPos = ChunkPos(0, 0)
    private var surroundingChunks = setOf<RoadmapChunk>()

    override fun onInitializeClient() {
        // This entrypoint is suitable for setting up client-specific logic, such as rendering.
        logger.info("Initializing roadmap client...")

        Config.reloadFile()
        logger.info("Loaded config")

        if (FileSys.containsFiles(Constants.OUTPUT_PATH)) {
            roadmap = Roadmap.readFiles()
            logger.info("Loaded ${roadmap.getBlockCount()} road blocks and ${roadmap.markers.count()} markers")
        }

        kbPrimary = KeyBindingHelper.registerKeyBinding(
            KeyBinding(
                "key.roadmap.primary",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                "category.roadmap.main"
            )
        )

        kbSecondary = KeyBindingHelper.registerKeyBinding(
            KeyBinding(
                "key.roadmap.secondary",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                "category.roadmap.main"
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
            val player = client.player ?: return@EndTick
            if (Config["draw_road_particles"] as Boolean) drawRoadParticles(client)
            if (Config["draw_marker_particles"] as Boolean) drawMarkerParticles(client)
            while (kbPrimary.wasPressed())
                if (isUsingTool(player)) primaryToolOp(client)
                else scanSurroundingRoads(client)
            while (kbSecondary.wasPressed())
                if (isUsingTool(player)) secondaryToolOp(client)
                else openUi(client)
            if (masterTickCount % 20 == 0) updateParticleChunks(client)
        })
    }

    fun drawRoadParticles(client: MinecraftClient) {
        val player = client.player ?: return
        val particleManager = client.particleManager
        val rand = Random(masterTickCount)

        val maxClearance = (Config["scan_height"] as Int).toFloat()

        for (chunk in surroundingChunks) {
            for (block in chunk.blocks.values) if (block.isRoad) {
                val spawnChance = sqrt(block.pos.getSquaredDistance(player.pos)) * 4
                if (rand.nextInt(spawnChance.toInt().coerceIn(4, 40)) != 0) continue

                val posX = block.pos.x.toDouble()
                val posY = block.pos.y.toDouble()
                val posZ = block.pos.z.toDouble()

                // Tunnel road particles will be tinted cyan to indicate that area is not exposed to the sky
                val color =
                    if (block.clearance == 0) Vector3f(1.0F, 1.0F, 1.0F)
                    else Vector3f(block.clearance / maxClearance, 1.0F, 0.0F)
                particleManager.addParticle(
                    DustParticleEffect(color, 1.0F),
                    posX + rand.nextDouble(),
                    posY + 1,
                    posZ + rand.nextDouble(),
                    0.0, 0.0, 0.0
                )
            }
        }
    }

    fun drawMarkerParticles(client: MinecraftClient) {
        val player = client.player ?: return
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

            when (marker.type) {
                RoadmapMarkerType.CUTOFF_POINT -> {

                    for (i in -markerHeight..markerHeight) {
                        if (rand.nextInt(5) != 0) continue
                        particleManager.addParticle(
                            ParticleTypes.SMOKE,
                            posX + rand.nextDouble(),
                            posY + rand.nextDouble(-markerHeight.toDouble(), markerHeight.toDouble() + 1),
                            posZ + rand.nextDouble(),
                            0.0, 0.0, 0.0
                        )
                    }

                } RoadmapMarkerType.SCAN_FENCE -> {

                    for (i in -markerHeight..markerHeight) {
                        if (rand.nextInt(5) != 0) continue
                        particleManager.addParticle(
                            ParticleTypes.FIREWORK,
                            posX + rand.nextDouble(),
                            posY + rand.nextDouble(-markerHeight.toDouble(), markerHeight.toDouble() + 1),
                            posZ + rand.nextDouble(),
                            0.0, 0.0, 0.0
                        )
                    }

                } RoadmapMarkerType.PATHFINDER_GOAL -> {

                    val angle = rand.nextDouble(Math.PI * 2)
                    val radius = 1.5
                    particleManager.addParticle(
                        ParticleTypes.HAPPY_VILLAGER,
                        posX + cos(angle) * radius + 0.5,
                        posY + rand.nextDouble(1.0, 3.0),
                        posZ + sin(angle) * radius + 0.5,
                        0.0, 0.0, 0.0
                    )

                }
            }
        }
    }

    fun drawBlockParticles(pos: BlockPos, color: Vector3f, client: MinecraftClient) {
        val particleManager = client.particleManager
        val rand = Random(masterTickCount)

        val posX = pos.x.toDouble()
        val posY = pos.y.toDouble()
        val posZ = pos.z.toDouble()

        // TODO: Deshiddify this code
        val effect = DustParticleEffect(color, 2.0F)
        for (i in 0..10)
            particleManager.addParticle(
                effect,
                posX + 1.1,
                posY + rand.nextDouble(),
                posZ + rand.nextDouble(),
                0.5, 0.0, 0.0
            )
        for (i in 0..10)
            particleManager.addParticle(
                effect,
                posX - 0.1,
                posY + rand.nextDouble(),
                posZ + rand.nextDouble(),
                -0.5, 0.0, 0.0
            )
        for (i in 0..10)
            particleManager.addParticle(
                effect,
                posX + rand.nextDouble(),
                posY + 1.0,
                posZ + rand.nextDouble(),
                0.0, 0.5, 0.0
            )
        for (i in 0..10)
            particleManager.addParticle(
                effect,
                posX + rand.nextDouble(),
                posY - 0.1,
                posZ + rand.nextDouble(),
                0.0, -0.5, 0.0
            )
        for (i in 0..10)
            particleManager.addParticle(
                effect,
                posX + rand.nextDouble(),
                posY + rand.nextDouble(),
                posZ + 1.1,
                0.0, 0.0, 0.5
            )
        for (i in 0..10)
            particleManager.addParticle(
                effect,
                posX + rand.nextDouble(),
                posY + rand.nextDouble(),
                posZ - 0.1,
                0.0, 0.0, -0.5
            )
    }

    fun isUsingTool(player: ClientPlayerEntity): Boolean {
        return Util.getRegistryName(player.mainHandStack) == Config["tool_item"]
    }

    fun getTargetedPos(player: ClientPlayerEntity): BlockPos? {
        val result = player.world.raycast(RaycastContext(
            player.eyePos,
            player.eyePos.add(player.rotationVector.multiply(16.0)),
            RaycastContext.ShapeType.COLLIDER,
            RaycastContext.FluidHandling.NONE,
            player
        ))
        return if (result.type == HitResult.Type.BLOCK)
            result.blockPos
        else
            null
    }

    fun primaryToolOp(client: MinecraftClient) {
        val player = client.player ?: return
        val pos = getTargetedPos(player) ?: return

        if (roadmap.removeMarker(pos, RoadmapMarkerType.CUTOFF_POINT)) {
            drawBlockParticles(pos, Vector3f(1.0F, 1.0F, 0.0F), client)
            notifyPlayer("Removed cutoff point at (${pos.x} ${pos.y} ${pos.z}).")
            roadmap.writeFiles()
            return
        }

        val marker = RoadmapMarker(pos, RoadmapMarkerType.SCAN_FENCE)
        if (!roadmap.testMarker(marker)) {
            roadmap.addMarker(marker)
            drawBlockParticles(pos, Vector3f(0.0F, 1.0F, 0.0F), client)
            notifyPlayer("Added scan fence at (${marker.pos.x} ${marker.pos.y} ${marker.pos.z}).")
        } else {
            roadmap.removeMarker(marker)
            drawBlockParticles(pos, Vector3f(1.0F, 0.0F, 0.0F), client)
            notifyPlayer("Removed scan fence at (${marker.pos.x} ${marker.pos.y} ${marker.pos.z}).")
        }
        roadmap.writeFiles()
    }

    fun secondaryToolOp(client: MinecraftClient) {
        val player = client.player ?: return
        val pos = getTargetedPos(player) ?: return

        val marker = RoadmapMarker(pos, RoadmapMarkerType.PATHFINDER_GOAL)
        if (!roadmap.testMarker(marker)) {
            roadmap.clearMarkers(RoadmapMarkerType.PATHFINDER_GOAL)
            roadmap.addMarker(marker)
            drawBlockParticles(pos, Vector3f(0.0F, 1.0F, 1.0F), client)
            notifyPlayer("Set pathfinder goal at (${marker.pos.x} ${marker.pos.y} ${marker.pos.z}).")
        } else {
            roadmap.removeMarker(marker)
            drawBlockParticles(pos, Vector3f(1.0F, 1.0F, 0.0F), client)
            notifyPlayer("Removed pathfinder goal at (${marker.pos.x} ${marker.pos.y} ${marker.pos.z}).")
        }
        roadmap.writeFiles()
    }

    fun undoOperation(client: MinecraftClient) {
        val player = client.player ?: return

        if (!roadmap.hasUndoHistory()) {
            notifyPlayer("No undo history found.", player)
            return
        }

        roadmap.revertStateFromUndoHistory()
        roadmap.writeFiles(true)
        updateParticleChunks(client, true)
        notifyPlayer("Last operation has been reverted.", player)
    }

    fun redoOperation(client: MinecraftClient) {
        val player = client.player ?: return

        if (!roadmap.hasRedoHistory()) {
            notifyPlayer("No redo history found.", player)
            return
        }

        roadmap.restoreStateFromRedoHistory()
        roadmap.writeFiles(true)
        updateParticleChunks(client, true)
        notifyPlayer("Last undone operation has been restored.", player)
    }

    fun handleClientStarted(client: MinecraftClient) {
        // TODO: Do something with this
    }

    fun handleJoinWorld(networkHandler: ClientPlayNetworkHandler, packetSender: PacketSender, client: MinecraftClient) {
        // TODO: Do something with this
    }

    fun scanSurroundingRoads(client: MinecraftClient) {
        val player = client.player ?: return

        roadmap.saveStateToUndoHistory()
        val scanner = RoadmapScanner(roadmap)
        if (scanner.scan(player)) {
            notifyPlayer("Scan completed.", player)
            roadmap.writeFiles()
            updateParticleChunks(client, true)
        } else {
            notifyPlayer("Could not find a road block at your position to start the scan.", player)
        }
    }

    fun clearSurroundingChunks(client: MinecraftClient) {
        val player = client.player ?: return

        roadmap.saveStateToUndoHistory()
        val startChunkCount = roadmap.chunks.count()
        for (chunk in roadmap.getChunksInRadius(ChunkPos.fromBlockPos(player.blockPos), 1))
            roadmap.removeChunk(chunk.pos)
        roadmap.writeFiles()
        val endChunkCount = roadmap.chunks.count()

        val removedChunks = startChunkCount - endChunkCount
        if (removedChunks > 0) {
            notifyPlayer("Removed $removedChunks ${if (removedChunks > 1) "chunks" else "chunk"} from scan data.", player)
        } else {
            notifyPlayer("No chunks were removed from scan data.", player)
        }

        updateParticleChunks(client, true)
    }

    fun findUnscannedRoads(client: MinecraftClient) {
        val player = client.player ?: return

        val posGroups = PosGroupCollection(32)
        for (marker in roadmap.getMarkers(RoadmapMarkerType.CUTOFF_POINT))
            posGroups.addPos(marker.pos)

        val sortedPositions = mutableListOf<BlockPos>()
        for (pos in posGroups.getCenterPositions())
            sortedPositions.add(pos)
        sortedPositions.sortBy { pos -> pos.getSquaredDistance(player.pos) }

        var result = ""
        for (pos in sortedPositions)
            result += ", (${pos.x}, ${pos.y}, ${pos.z})"
        if (result.isNotEmpty())
            messagePlayer("Positions of all unscanned road tails sorted by distance: ${result.substring(2)}", player)
        else
            messagePlayer("Could not find any unscanned road tails.", player)
    }

    fun reloadData(client: MinecraftClient) {
        val player = client.player ?: return

        BlockStateCache.clearBlockStates()
        roadmap = Roadmap.readFiles()
        Config.reloadFile()
        notifyPlayer("Reloaded scan data and config.", player)

        updateParticleChunks(client, true)
    }
    
    fun clearData(client: MinecraftClient) {
        val player = client.player ?: return

        roadmap.saveStateToUndoHistory()
        roadmap.clearMarkers()
        roadmap.clearChunks()
        roadmap.writeFiles()
        notifyPlayer("Cleared map data.", player)

        updateParticleChunks(client, true)
    }

    fun openUi(client: MinecraftClient) {
        client.setScreenAndRender(ui)
    }

    fun updateParticleChunks(client: MinecraftClient, force: Boolean = false) {
        val player = client.player ?: return

        val currentChunkPos = ChunkPos.fromBlockPos(player.blockPos)
        if (currentChunkPos != lastChunkPos || force) {
            surroundingChunks = roadmap.getChunksInRadius(currentChunkPos, (Config["particle_radius"] as Int) / Constants.CHUNK_SIZE)
            lastChunkPos = currentChunkPos
        }
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
        val player = client.player ?: return ""
        val server = client.server ?: return ""
        return ServerAddress.parse(client.server!!.serverIp).address
    }
}
