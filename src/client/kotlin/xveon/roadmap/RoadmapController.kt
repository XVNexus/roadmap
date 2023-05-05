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

    fun handleRedoScanPress(button: ButtonWidget?) {
        RoadmapClient.logger.info("GUI: Redo Scan button pressed")
        RoadmapClient.redoLastScan(MinecraftClient.getInstance())
    }

    fun handleClearAreaPress(button: ButtonWidget?) {
        RoadmapClient.logger.info("GUI: Clear Area button pressed")
        RoadmapClient.clearSurroundingChunks(MinecraftClient.getInstance())
    }

    fun handleFindTailsPress(button: ButtonWidget?) {
        RoadmapClient.logger.info("GUI: Find Unscanned button pressed")
        RoadmapClient.findUnscannedRoads(MinecraftClient.getInstance())
    }

    fun handleReloadDataPress(button: ButtonWidget?) {
        RoadmapClient.logger.info("GUI: Reload Data button pressed")
        RoadmapClient.reloadData(MinecraftClient.getInstance())
    }

    fun handleClearDataPress(button: ButtonWidget?) {
        RoadmapClient.logger.info("GUI: Clear Data button pressed")
        RoadmapClient.clearData(MinecraftClient.getInstance())
    }

    fun handleConfigToggle(label: String, configId: String) {
        Config[configId] = !(Config[configId] as Boolean)
        RoadmapClient.logger.info("UI: Changed option '$label' to ${Config[configId]}")
        RoadmapClient.notifyPlayer("Changed option '$label' to ${Config[configId]}.")
        Config.saveFile()
    }

    fun handleConfigAdjust(amount: Int, label: String, configId: String) {
        Config[configId] = (Config[configId] as Int) + amount
        RoadmapClient.logger.info("UI: Changed option '$label' to ${Config[configId]}")
        RoadmapClient.notifyPlayer("Changed option '$label' to ${Config[configId]}.")
        Config.saveFile()
    }

    fun handleConfigAdjust(amount: Double, label: String, configId: String) {
        Config[configId] = (Config[configId] as Double) + amount
        RoadmapClient.logger.info("UI: Changed option '$label' to ${Config[configId]}")
        RoadmapClient.notifyPlayer("Changed option '$label' to ${Config[configId]}.")
        Config.saveFile()
    }

    fun handleConfigChange(value: String?, label: String, configId: String) {
        val successfullyChanged = Config.setOptionString(configId, value ?: return)
        if (successfullyChanged) {
            RoadmapClient.logger.info("UI: Changed option '$label' to ${Config[configId]}")
            RoadmapClient.notifyPlayer("Changed option '$label' to ${Config[configId]}.")
            Config.saveFile()
        }
    }
}
