package xveon.roadmap

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.widget.ButtonWidget

object RoadmapController {
    fun handleScanPress(button: ButtonWidget?) {
        RoadmapClient.logger.info("GUI: Scan button pressed")
        RoadmapClient.scanSurroundingRoads(MinecraftClient.getInstance())
    }

    fun handleUndoScanPress(button: ButtonWidget?) {
        RoadmapClient.logger.info("GUI: Undo Scan button pressed")
        RoadmapClient.undoLastScan(MinecraftClient.getInstance())
    }

    fun handleClearAreaPress(button: ButtonWidget?) {
        RoadmapClient.logger.info("GUI: Clear Area button pressed")
        RoadmapClient.clearSurroundingChunks(MinecraftClient.getInstance())
    }

    fun handleFindNewPress(button: ButtonWidget?) {
        RoadmapClient.logger.info("GUI: Find New button pressed")
        RoadmapClient.findUnscannedRoads(MinecraftClient.getInstance())
    }

    fun handleReloadPress(button: ButtonWidget?) {
        RoadmapClient.logger.info("GUI: Reload button pressed")
        RoadmapClient.reloadFiles(MinecraftClient.getInstance())
    }

    fun handleConfigChange(value: String?, label: String, configId: String) {
        val successfullyChanged = Config.setOptionString(configId, value ?: return)
        if (successfullyChanged) {
            RoadmapClient.logger.info("UI: Changed option '$label' to ${Config[configId]}")
            RoadmapClient.notifyPlayer("Changed option '$label' to ${Config[configId]}.")
            Config.saveFile()
        }
    }

    fun handleConfigToggle(label: String, configId: String) {
        Config[configId] = !(Config[configId] as Boolean)
        RoadmapClient.logger.info("UI: Changed option '$label' to ${Config[configId]}")
        RoadmapClient.notifyPlayer("Changed option '$label' to ${Config[configId]}.")
        Config.saveFile()
    }
}
