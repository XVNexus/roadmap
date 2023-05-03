package xveon.roadmap

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.client.gui.widget.*
import net.minecraft.client.gui.widget.ButtonWidget.PressAction
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.text.Text

@Environment(EnvType.CLIENT)
class RoadmapGui(val parent: Screen? = null) : Screen(Text.literal("Roadmap Manager")) {
    private val spacing = 4
    private val shortHeight = 12
    private val mediumHeight = 16
    private val tallHeight = 20
    private var sizeX = 360
    private var sizeY = 320
    private var centerX = 0
    private var centerY = 0
    private var pointerX = 0
    private var pointerY = 0
    private var maxWidth = sizeX - spacing * 2
    private var maxHeight = sizeY - spacing * 2

    override fun init() {
        resetLayoutControlVars()

        putHeader("Roadmap Menu")
        putButton(maxWidth, tallHeight, "Scan", "Scan the surrounding area for road blocks")
        { button: ButtonWidget? -> RoadmapController.handleScanPress(button) }
        putButton(70, tallHeight, "Clear Area", "Clear the surrounding chunks of road data")
        { button: ButtonWidget? -> RoadmapController.handleClearAreaPress(button) }
        putButton(maxWidth - 140 - spacing * 2, tallHeight, "Undo Scan", "Undo the last scan")
        { button: ButtonWidget? -> RoadmapController.handleUndoScanPress(button) }
        putButton(70, tallHeight, "Find New", "Locate road sections which have been partially scanned")
        { button: ButtonWidget? -> RoadmapController.handleFindNewPress(button) }

        putSpacer(shortHeight)

        putHeader("Config Options")
        putConfigField(maxWidth, mediumHeight, "Draw Particles", "Display particles on road surfaces", "draw_particles")
        putConfigField(maxWidth, mediumHeight, "Particle Chunk Radius", "Maximum distance that road surface particles should be drawn", "particle_chunk_radius")
        putConfigField(maxWidth, mediumHeight, "Scan Radius", "How far away from the player the scanner can record blocks", "scan_radius")
        putConfigField(maxWidth, mediumHeight, "Scan Height", "How many blocks high to scan when searching for the ceiling", "scan_height")
        putConfigField(maxWidth, mediumHeight, "Scan Everything", "Scan all blocks in the player radius instead of just scanning near roads", "scan_everything")
        putConfigField(maxWidth, mediumHeight, "Road Blocks", "Which blocks are recorded as roads", "road_blocks")
        putConfigField(maxWidth, mediumHeight, "Terrain Blocks", "Which blocks are recorded as terrain (scanner considers all solid blocks not mentioned here as terrain)", "terrain_blocks")
        putConfigField(maxWidth, mediumHeight, "Ignored Blocks", "Which blocks are ignored by the scanner (scanner ignores all transparent blocks not mentioned here)", "ignored_blocks")

        putSpacer(shortHeight)

        putHeader("Other Controls")
        putButton(maxWidth, mediumHeight, "Reload Data", "Load the config and scan data from saved files and clear the cache")
        { button: ButtonWidget? -> RoadmapController.handleReloadPress(button) }
    }

    override fun render(matrices: MatrixStack?, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(matrices)
        super.render(matrices, mouseX, mouseY, delta)
    }

    fun resetLayoutControlVars() {
        centerX = width / 2
        centerY = height / 2
        pointerX = centerX - sizeX / 2 + spacing
        pointerY = centerY - sizeY / 2 + spacing
        maxWidth = sizeX - spacing * 2
        maxHeight = sizeY - spacing * 2
    }

    fun movePointer(width: Int, height: Int) {
        if (width == 0) {
            pointerY += height + spacing
            return
        }
        pointerX += width + spacing
        if (pointerX >= centerX + maxWidth / 2) {
            pointerX = centerX - sizeX / 2 + spacing
            pointerY += height + spacing
        }
    }

    fun putSpacer(height: Int) {
        movePointer(0, height)
    }

    fun putHeader(text: String) {
        addDrawableChild(TextWidget(
            pointerX, pointerY, maxWidth, shortHeight,
            Text.literal(text),
            MinecraftClient.getInstance().textRenderer
        ))

        movePointer(maxWidth, shortHeight)
    }

    fun putButton(width: Int, height: Int, label: String, tooltip: String, action: PressAction) {
        addDrawableChild(ButtonWidget.builder(
            Text.literal(label), action
        )
            .dimensions(pointerX, pointerY, width, height)
            .tooltip(Tooltip.of(Text.literal(tooltip)))
            .build())

        movePointer(width, height)
    }

    fun putConfigField(width: Int, height: Int, label: String, tooltip: String, configId: String) {
        val labelWidth = 120

        val text = TextWidget(
            pointerX, pointerY, labelWidth, height,
            Text.literal("$label:"),
            MinecraftClient.getInstance().textRenderer
        )
        text.alignLeft()
        addDrawableChild(text)

        when (Config.getOption(configId)?.type) {
            ConfigType.BOOLEAN -> {

                addDrawableChild(ButtonWidget.builder(
                    Text.literal(Config.getOptionString(configId))
                ) { button: ButtonWidget? ->
                    run {
                        RoadmapController.handleConfigToggle(label, configId)
                        button?.message = Text.literal(Config.getOptionString(configId))
                    }
                }
                    .dimensions(pointerX + labelWidth + spacing, pointerY, maxWidth - labelWidth - spacing, height)
                    .tooltip(Tooltip.of(Text.literal(tooltip)))
                    .build())

            }
            else -> {

                val field = TextFieldWidget(
                    MinecraftClient.getInstance().textRenderer,
                    pointerX + labelWidth + spacing + 1, pointerY + 1, maxWidth - labelWidth - spacing - 2, height - 2,
                    Text.literal(label),
                )
                field.text = Config.getOptionString(configId)
                field.setChangedListener { value: String? -> RoadmapController.handleConfigChange(value, label, configId) }
                field.setTooltip(Tooltip.of(Text.literal(tooltip)))
                field.setMaxLength(2048)
                addDrawableChild(field)

            }
        }

        movePointer(maxWidth, height)
    }
}
