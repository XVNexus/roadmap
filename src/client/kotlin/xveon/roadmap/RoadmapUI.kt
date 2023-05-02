package xveon.roadmap

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.client.gui.widget.TextWidget
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.text.Text

@Environment(EnvType.CLIENT)
class RoadmapUI(val parent: Screen? = null) : Screen(Text.literal("Roadmap Manager")) {
    override fun init() {
        val centerX = width / 2
        val centerY = height / 2

        addDrawableChild(TextWidget(
            centerX - 156, centerY - 116, 312, 12,
            Text.literal("Roadmap"),
            MinecraftClient.getInstance().textRenderer
        ))

        addDrawableChild(ButtonWidget.builder(
            Text.literal("Scan")
        ) { button: ButtonWidget? -> handleScanPress(button) }
            .dimensions(centerX - 156, centerY - 100, 312, 20)
            .tooltip(Tooltip.of(Text.literal("Scan the surrounding area for road blocks")))
            .build())

        addDrawableChild(ButtonWidget.builder(
            Text.literal("Clear Area")
        ) { button: ButtonWidget? -> handleClearAreaPress(button) }
            .dimensions(centerX - 156, centerY - 76, 60, 20)
            .tooltip(Tooltip.of(Text.literal("Clear the surrounding chunks of road data")))
            .build())

        addDrawableChild(ButtonWidget.builder(
            Text.literal("Reload Files")
        ) { button: ButtonWidget? -> handleReloadFilesPress(button) }
            .dimensions(centerX - 92, centerY - 76, 184, 20)
            .tooltip(Tooltip.of(Text.literal("Load the config and scan data from saved files")))
            .build())

        addDrawableChild(ButtonWidget.builder(
            Text.literal("Undo Scan")
        ) { button: ButtonWidget? -> handleUndoScanPress(button) }
            .dimensions(centerX + 96, centerY - 76, 60, 20)
            .tooltip(Tooltip.of(Text.literal("Undo the last scan")))
            .build())


        addDrawableChild(TextWidget(
            centerX - 156, centerY - 40, 312, 12,
            Text.literal("Config"),
            MinecraftClient.getInstance().textRenderer
        ))

        addDrawableChild(TextWidget(
            centerX - 156, centerY - 24, 80, 20,
            Text.literal("Scan Radius:"),
            MinecraftClient.getInstance().textRenderer
        ))

        val fieldScanRadius = TextFieldWidget(
            MinecraftClient.getInstance().textRenderer,
            centerX - 71, centerY - 23, 226, 18,
            Text.literal("Scan Radius"),
        )
        fieldScanRadius.text = Config.scanRadius.toString()
        fieldScanRadius.setChangedListener { value: String? -> handleScanRadiusChange(value) }
        addDrawableChild(fieldScanRadius)

        addDrawableChild(TextWidget(
            centerX - 156, centerY, 80, 20,
            Text.literal("Scan Height:"),
            MinecraftClient.getInstance().textRenderer
        ))

        val fieldScanHeight = TextFieldWidget(
            MinecraftClient.getInstance().textRenderer,
            centerX - 71, centerY + 1, 226, 18,
            Text.literal("Scan Height"),
        )
        fieldScanHeight.text = Config.scanHeight.toString()
        fieldScanHeight.setChangedListener { value: String? -> handleScanHeightChange(value) }
        addDrawableChild(fieldScanHeight)

        addDrawableChild(TextWidget(
            centerX - 156, centerY + 24, 80, 20,
            Text.literal("Scan Everything:"),
            MinecraftClient.getInstance().textRenderer
        ))

        val fieldScanEverything = TextFieldWidget(
            MinecraftClient.getInstance().textRenderer,
            centerX - 71, centerY + 25, 226, 18,
            Text.literal("Scan Everything"),
        )
        fieldScanEverything.text = Config.scanEverything.toString()
        fieldScanEverything.setChangedListener { value: String? -> handleScanEverythingChange(value) }
        addDrawableChild(fieldScanEverything)

        addDrawableChild(TextWidget(
            centerX - 156, centerY + 48, 80, 20,
            Text.literal("Road Blocks:"),
            MinecraftClient.getInstance().textRenderer
        ))

        val fieldRoadBlocks = TextFieldWidget(
            MinecraftClient.getInstance().textRenderer,
            centerX - 71, centerY + 49, 226, 18,
            Text.literal("Road Blocks"),
        )
        fieldRoadBlocks.setMaxLength(1024)
        fieldRoadBlocks.text = Config.roadBlocks.toString()
        fieldRoadBlocks.setChangedListener { value: String? -> handleRoadBlocksChange(value) }
        addDrawableChild(fieldRoadBlocks)

        addDrawableChild(TextWidget(
            centerX - 156, centerY + 72, 80, 20,
            Text.literal("Terrain Blocks:"),
            MinecraftClient.getInstance().textRenderer
        ))

        val fieldTerrainBlocks = TextFieldWidget(
            MinecraftClient.getInstance().textRenderer,
            centerX - 71, centerY + 73, 226, 18,
            Text.literal("Terrain Blocks"),
        )
        fieldTerrainBlocks.setMaxLength(1024)
        fieldTerrainBlocks.text = Config.terrainBlocks.toString()
        fieldTerrainBlocks.setChangedListener { value: String? -> handleTerrainBlocksChange(value) }
        addDrawableChild(fieldTerrainBlocks)

        addDrawableChild(TextWidget(
            centerX - 156, centerY + 96, 80, 20,
            Text.literal("Ignored Blocks:"),
            MinecraftClient.getInstance().textRenderer
        ))

        val fieldIgnoredBlocks = TextFieldWidget(
            MinecraftClient.getInstance().textRenderer,
            centerX - 71, centerY + 97, 226, 18,
            Text.literal("Ignored Blocks"),
        )
        fieldIgnoredBlocks.setMaxLength(1024)
        fieldIgnoredBlocks.text = Config.ignoredBlocks.toString()
        fieldIgnoredBlocks.setChangedListener { value: String? -> handleIgnoredBlocksChange(value) }
        addDrawableChild(fieldIgnoredBlocks)
    }

    override fun render(matrices: MatrixStack?, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(matrices, mouseX, mouseY, delta)
    }

    private fun handleScanPress(button: ButtonWidget?) {
        RoadmapClient.logger.info("UI: Scan button pressed")
        RoadmapClient.scanSurroundingRoads(MinecraftClient.getInstance())
    }

    private fun handleClearAreaPress(button: ButtonWidget?) {
        RoadmapClient.logger.info("UI: Clear Area button pressed")
        RoadmapClient.clearSurroundingChunks(MinecraftClient.getInstance())
    }

    private fun handleReloadFilesPress(button: ButtonWidget?) {
        RoadmapClient.logger.info("UI: Reload Files button pressed")
        RoadmapClient.reloadFiles(MinecraftClient.getInstance())
    }

    private fun handleUndoScanPress(button: ButtonWidget?) {
        RoadmapClient.logger.info("UI: Undo Scan button pressed")
        RoadmapClient.undoLastScan(MinecraftClient.getInstance())
    }

    private fun handleScanRadiusChange(value: String?) {
        val cfgValue = value?.toDoubleOrNull() ?: return
        Config.scanRadius = cfgValue
        Config.saveFile()
        RoadmapClient.logger.info("UI: Set Scan Radius to $cfgValue")
    }

    private fun handleScanHeightChange(value: String?) {
        val cfgValue = value?.toIntOrNull() ?: return
        Config.scanHeight = cfgValue
        Config.saveFile()
        RoadmapClient.logger.info("UI: Set Scan Height to $cfgValue")
    }

    private fun handleScanEverythingChange(value: String?) {
        val cfgValue = value?.lowercase()?.toBooleanStrictOrNull() ?: return
        Config.scanEverything = cfgValue
        Config.saveFile()
        RoadmapClient.logger.info("UI: Set Scan Everything to $cfgValue")
    }

    private fun handleRoadBlocksChange(value: String?) {
        val cfgValue = value?.trim('[', ']')?.split(',', ' ')?.toMutableList() ?: return
        cfgValue.removeAll { item: String -> item.trim().isEmpty() }
        Config.roadBlocks = cfgValue
        Config.saveFile()
        RoadmapClient.logger.info("UI: Set Road Blocks to $cfgValue")
    }

    private fun handleTerrainBlocksChange(value: String?) {
        val cfgValue = value?.trim('[', ']')?.split(',', ' ')?.toMutableList() ?: return
        cfgValue.removeAll { item: String -> item.trim().isEmpty() }
        Config.terrainBlocks = cfgValue
        Config.saveFile()
        RoadmapClient.logger.info("UI: Set Terrain Blocks to $cfgValue")
    }

    private fun handleIgnoredBlocksChange(value: String?) {
        val cfgValue = value?.trim('[', ']')?.split(',', ' ')?.toMutableList() ?: return
        cfgValue.removeAll { item: String -> item.trim().isEmpty() }
        Config.ignoredBlocks = cfgValue
        Config.saveFile()
        RoadmapClient.logger.info("UI: Set Ignored Blocks to $cfgValue")
    }
}
