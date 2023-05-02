package xveon.roadmap

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.ClientStarted
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginConnectionEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayNetworkHandler
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.client.network.ServerAddress
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import net.minecraft.text.Text
import org.lwjgl.glfw.GLFW
import org.slf4j.LoggerFactory

object RoadmapClient : ClientModInitializer {
    val logger = LoggerFactory.getLogger("roadmap")
    private lateinit var kbScan: KeyBinding
    private lateinit var kbReload: KeyBinding
    private lateinit var kbUi: KeyBinding
    private var map = ScannedRoadmap()
    private var ui = RoadmapUI()

    override fun onInitializeClient() {
        // This entrypoint is suitable for setting up client-specific logic, such as rendering.
        logger.info("Initializing roadmap client...")

        Config.loadFile()
        Config.saveFile()
        logger.info("Loaded config")

        if (FileSys.containsFiles(Constants.SCAN_FOLDER_PATH)) {
            map = ScannedRoadmap.readFiles()
            logger.info("Loaded ${map.getBlockCount()} previously scanned blocks.")
        }

        kbScan = KeyBindingHelper.registerKeyBinding(
            KeyBinding(
                "key.roadmap.scan",  // The translation key of the keybinding's name
                InputUtil.Type.KEYSYM,  // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
                GLFW.GLFW_KEY_R,  // The keycode of the key
                "category.roadmap.main" // The translation key of the keybinding's category.
            )
        )

        kbReload = KeyBindingHelper.registerKeyBinding(
            KeyBinding(
                "key.roadmap.reload",  // The translation key of the keybinding's name
                InputUtil.Type.KEYSYM,  // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
                GLFW.GLFW_KEY_G,  // The keycode of the key
                "category.roadmap.main" // The translation key of the keybinding's category.
            )
        )

        kbUi = KeyBindingHelper.registerKeyBinding(
            KeyBinding(
                "key.roadmap.ui",  // The translation key of the keybinding's name
                InputUtil.Type.KEYSYM,  // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
                GLFW.GLFW_KEY_U,  // The keycode of the key
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
            while (kbScan.wasPressed()) handleScanKeypress(client)
        })

        ClientTickEvents.END_CLIENT_TICK.register(ClientTickEvents.EndTick { client: MinecraftClient ->
            while (kbReload.wasPressed()) handleReloadKeypress(client)
        })

        ClientTickEvents.END_CLIENT_TICK.register(ClientTickEvents.EndTick { client: MinecraftClient ->
            while (kbUi.wasPressed()) handleUiKeypress(client)
        })
    }

    fun handleClientStarted(client: MinecraftClient) {
        // TODO: Do something with this
    }

    fun handleJoinWorld(networkHandler: ClientPlayNetworkHandler, packetSender: PacketSender, client: MinecraftClient) {
        // TODO: Do something with this
    }

    fun handleScanKeypress(client: MinecraftClient) {
        val player: ClientPlayerEntity = client.player ?: return
        logger.info("Scanning surrounding blocks...")
        val scanner = RoadmapScanner(map)
        val startBlockCount = map.getBlockCount()
        scanner.scan(player)
        val endBlockCount = map.getBlockCount()
        val newBlocks = endBlockCount - startBlockCount
        if (newBlocks > 0) {
            map.writeFiles()
            logger.info("Saved $newBlocks blocks to scan data.")
            notifyPlayer("Saved $newBlocks blocks to scan data", player)
        } else {
            logger.info("No new blocks were saved to scan data.")
            notifyPlayer("No new blocks were saved to scan data", player)
        }
    }

    fun handleReloadKeypress(client: MinecraftClient) {
        val player: ClientPlayerEntity = client.player ?: return
        map = ScannedRoadmap.readFiles()
        Config.loadFile()
        Config.saveFile()
        logger.info("Reloaded scan data and config.")
        notifyPlayer("Reloaded scan data and config.", player)
    }

    fun handleUiKeypress(client: MinecraftClient) {
        val player: ClientPlayerEntity = client.player ?: return
        // ui.init(client, 100, 100)
        if (client.server != null) {
            val sa = ServerAddress.parse(client.server!!.serverIp)
            messagePlayer("Address: ${sa.address} | Port: ${sa.port}", player)
        }
        else {
            messagePlayer("Server not found, cannot get address and port.", player)
        }
        logger.warn("UI is not yet implemented.")
        notifyPlayer("UI is not yet implemented.", player)
    }

    fun messagePlayer(text: String, player: ClientPlayerEntity) {
        player.sendMessage(Text.literal("Roadmap: $text"), false)
    }

    fun notifyPlayer(text: String, player: ClientPlayerEntity) {
        player.sendMessage(Text.literal("Roadmap: $text"), true)
    }
}
