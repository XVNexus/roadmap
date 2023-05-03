package xveon.roadmap

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.widget.ButtonWidget

object RoadmapController {
    fun handleScanPress(button: ButtonWidget?) {
        RoadmapClient.logger.info("UI: Scan button pressed")
        RoadmapClient.scanSurroundingRoads(MinecraftClient.getInstance())
    }

    fun handleClearAreaPress(button: ButtonWidget?) {
        RoadmapClient.logger.info("UI: Clear Area button pressed")
        RoadmapClient.clearSurroundingChunks(MinecraftClient.getInstance())
    }

    fun handleUndoScanPress(button: ButtonWidget?) {
        RoadmapClient.logger.info("UI: Undo Scan button pressed")
        RoadmapClient.undoLastScan(MinecraftClient.getInstance())
    }
    fun handleReloadPress(button: ButtonWidget?) {
        RoadmapClient.logger.info("UI: Reload button pressed")
        RoadmapClient.reloadFiles(MinecraftClient.getInstance())
    }

    fun handleConfigChange(value: String?, configId: String) {
        val successfullyChanged = Config.setOptionString(configId, value ?: return)
        if (successfullyChanged) {
            RoadmapClient.logger.info("UI: Changed config option $configId to ${Config[configId]}")
            RoadmapClient.notifyPlayer("Changed config option $configId to ${Config[configId]}.")
            Config.saveFile()
        }
    }
}
