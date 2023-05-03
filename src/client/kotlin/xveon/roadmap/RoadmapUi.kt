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
class RoadmapUi(val parent: Screen? = null) : Screen(Text.literal("Roadmap Manager")) {
    private val spacing = 4
    private val shortHeight = 12
    private val tallHeight = 20
    private var sizeX = 360
    private var sizeY = 320
    private var centerX = 0
    private var centerY = 0
    private var pointerX = 0
    private var pointerY = 0
    private var maxWidth = sizeX - spacing * 2
    private var maxHeight = sizeY - spacing * 2

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

    fun putButton(width: Int, label: String, tooltip: String, action: PressAction) {
        addDrawableChild(ButtonWidget.builder(
            Text.literal(label), action
        )
            .dimensions(pointerX, pointerY, width, tallHeight)
            .tooltip(Tooltip.of(Text.literal(tooltip)))
            .build())

        movePointer(width, tallHeight)
    }

    fun putConfigField(label: String, tooltip: String, configId: String) {
        val labelWidth = 120

        addDrawableChild(TextWidget(
            pointerX, pointerY, labelWidth, tallHeight,
            Text.literal("$label:"),
            MinecraftClient.getInstance().textRenderer
        ))

        val field = TextFieldWidget(
            MinecraftClient.getInstance().textRenderer,
            pointerX + labelWidth + spacing, pointerY, maxWidth - labelWidth - spacing * 3, tallHeight - 2,
            Text.literal(label),
        )
        field.setMaxLength(2048)
        field.text = Config.getOptionString(configId)
        field.setChangedListener { value: String? -> RoadmapController.handleConfigChange(value, configId) }
        addDrawableChild(field)

        movePointer(maxWidth, tallHeight)
    }

    override fun init() {
        resetLayoutControlVars()

        putHeader("Roadmap Menu")
        putButton(maxWidth, "Scan", "Scan the surrounding area for road blocks")
        { button: ButtonWidget? -> RoadmapController.handleScanPress(button) }
        putButton(60, "Undo Scan", "Undo the last scan")
        { button: ButtonWidget? -> RoadmapController.handleUndoScanPress(button) }
        putButton(maxWidth - 120 - spacing * 2, "Clear Area", "Clear the surrounding chunks of road data")
        { button: ButtonWidget? -> RoadmapController.handleClearAreaPress(button) }
        putButton(60, "Reload", "Load the config and scan data from saved files and clear the cache")
        { button: ButtonWidget? -> RoadmapController.handleReloadPress(button) }

        putSpacer(shortHeight)

        putHeader("Config Options")
        putConfigField("Draw Particles", "Toggles the in-game particle visualizer that marks road surfaces", "draw_particles")
        putConfigField("Particle Chunk Radius", "How many chunks away from the player the road particles should be drawn", "particle_chunk_radius")
        putConfigField("Scan Radius", "Maximum distance from the player the scanner will record blocks", "scan_radius")
        putConfigField("Scan Height", "How many blocks up to scan when searching for the ceiling", "scan_height")
        putConfigField("Scan Everything", "If this is set to true, the scanner will search all blocks in the player radius instead of just searching on and near road blocks", "scan_everything")
        putConfigField("Road Blocks", "The list of block ids which are considered as road blocks", "road_blocks")
        putConfigField("Terrain Blocks", "The list of block ids which are considered as terrain (not road) blocks (scanner considers all solid blocks not mentioned here as terrain)", "terrain_blocks")
        putConfigField("Ignored Blocks", "The list of block ids which are completely ignored by the scanner (scanner ignores all transparent blocks not mentioned here)", "ignored_blocks")
    }

    override fun render(matrices: MatrixStack?, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(matrices, mouseX, mouseY, delta)
    }
}
