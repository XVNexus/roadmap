package xveon.roadmap

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayerEntity
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

        ClientTickEvents.END_CLIENT_TICK.register(ClientTickEvents.EndTick { client: MinecraftClient ->
            while (kbScan.wasPressed()) {
                val player: ClientPlayerEntity = client.player ?: break
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
        })

        ClientTickEvents.END_CLIENT_TICK.register(ClientTickEvents.EndTick { client: MinecraftClient ->
            while (kbReload.wasPressed()) {
                val player: ClientPlayerEntity = client.player ?: break
                map = ScannedRoadmap.readFiles()
                Config.loadFile()
                notifyPlayer("Reloaded scan data and config", player)
            }
        })

        ClientTickEvents.END_CLIENT_TICK.register(ClientTickEvents.EndTick { client: MinecraftClient ->
            while (kbUi.wasPressed()) {
                logger.info("Opening ui...")
                ui.init(client, 100, 100)
            }
        })
    }

    fun notifyPlayer(text: String, player: ClientPlayerEntity, chatOrPopup: Boolean = true) {
        player.sendMessage(Text.literal("Roadmap: $text"), chatOrPopup)
    }
}
