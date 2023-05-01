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
import javax.management.remote.rmi.RMIConnection

object RoadmapClient : ClientModInitializer {
    val logger = LoggerFactory.getLogger("roadmap")
    private lateinit var kbScan: KeyBinding
    private lateinit var kbReload: KeyBinding
    private lateinit var kbUi: KeyBinding
    private var map = RMMap()
    private var ui = RoadmapUI()

    override fun onInitializeClient() {
        // This entrypoint is suitable for setting up client-specific logic, such as rendering.
        logger.info("Initializing roadmap client...")

        Config.loadFile()
        Config.saveFile()
        logger.info("Loaded config")

        if (FS.containsFiles(Constants.SCAN_FOLDER_PATH)) {
            map = RMMap.readFiles()
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
                val scanner = RMScanner(map)
                val startBlockCount = map.getBlockCount()
                scanner.scan(player)
                val endBlockCount = map.getBlockCount()
                val newBlocks = endBlockCount - startBlockCount
                if (newBlocks > 0) {
                    map.writeFiles()
                    logger.info("Saved $newBlocks blocks to scan data.")
                    displayPopupText("Saved $newBlocks blocks to scan data", player)
                } else {
                    logger.info("No new blocks were saved to scan data.")
                    displayPopupText("No new blocks were saved to scan data", player)
                }
            }
        })

        ClientTickEvents.END_CLIENT_TICK.register(ClientTickEvents.EndTick { client: MinecraftClient ->
            while (kbReload.wasPressed()) {
                val player: ClientPlayerEntity = client.player ?: break
                map = RMMap.readFiles()
                Config.loadFile()
                displayPopupText("Reloaded scan data and config", player)
            }
        })

        ClientTickEvents.END_CLIENT_TICK.register(ClientTickEvents.EndTick { client: MinecraftClient ->
            while (kbUi.wasPressed()) {
                logger.info("Opening ui...")
                ui.init(client, 100, 100)
            }
        })
    }

    fun displayPopupText(text: String, player: ClientPlayerEntity) {
        player.sendMessage(Text.literal("Roadmap: $text"), true)
    }
}
