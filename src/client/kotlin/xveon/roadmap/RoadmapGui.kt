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
    private var sizeX = 400
    private var sizeY = 400
    private var centerX = 0
    private var centerY = 0
    private val spacing = 4

    private val shortWidth = 60
    private val mediumWidth = 80
    private val longWidth = 100
    private var maxWidth = sizeX - spacing * 2

    private val shortHeight = 12
    private val mediumHeight = 16
    private val tallHeight = 20
    private var maxHeight = sizeY - spacing * 2

    private var pointerX = 0
    private var pointerY = 0

    override fun init() {
        resetLayoutControlVars()

        putHeader("Roadmap Menu")
        val remainingWidth = (maxWidth - shortWidth * 2 - spacing * 3) / 2
        putButton(maxWidth, tallHeight, "Scan", "Scan the surrounding area for road blocks")
        { button: ButtonWidget? -> RoadmapController.handleScanPress(button) }
        putButton(shortWidth, tallHeight, "Clear Area", "Clear the surrounding chunks of road data")
        { button: ButtonWidget? -> RoadmapController.handleClearAreaPress(button) }
        putButton(remainingWidth, tallHeight, "Undo", "Undo the last operation")
        { button: ButtonWidget? -> RoadmapController.handleUndoPress(button) }
        putButton(remainingWidth, tallHeight, "Redo", "Redo the last operation")
        { button: ButtonWidget? -> RoadmapController.handleRedoPress(button) }
        putButton(shortWidth, tallHeight, "Find Tails", "Locate road sections which have been partially scanned")
        { button: ButtonWidget? -> RoadmapController.handleFindTailsPress(button) }

        putSpacer(mediumHeight)

        putHeader("Config Options")
        putConfigField(maxWidth, mediumHeight, "Draw Road Particles", "Display particles on road surfaces", "draw_road_particles")
        putConfigField(maxWidth, mediumHeight, "Draw Marker Particles", "Display particles at marker positions", "draw_marker_particles")
        putConfigField(maxWidth, mediumHeight, "Particle Radius", "Maximum distance from the player that particles should be drawn", "particle_radius")
        putConfigField(maxWidth, mediumHeight, "Scan Radius", "How far away from the player the scanner can record blocks", "scan_radius")
        putConfigField(maxWidth, mediumHeight, "Scan Height", "How many blocks high to scan when searching for the ceiling", "scan_height")
        putConfigField(maxWidth, mediumHeight, "Scan Everything", "Scan all blocks in the player radius instead of just scanning near roads", "scan_everything")
        putConfigField(maxWidth, mediumHeight, "Road Blocks", "Which blocks are recorded as roads", "road_blocks")
        putConfigField(maxWidth, mediumHeight, "Terrain Blocks", "Which blocks are recorded as terrain (scanner considers all solid blocks not mentioned here as terrain)", "terrain_blocks")
        putConfigField(maxWidth, mediumHeight, "Ignored Blocks", "Which blocks are ignored by the scanner (scanner ignores all transparent blocks not mentioned here)", "ignored_blocks")
        putConfigField(maxWidth, mediumHeight, "Undo History Limit", "Maximum number of operations stored in undo history", "undo_history_limit")
        putConfigField(maxWidth, mediumHeight, "Enable Clear Button", "Enable the clear all button (deletes all data associated with the current world, use with caution!)", "enable_clear_button")

        putSpacer(mediumHeight)

        putHeader("Other Controls")
        if (!(Config["enable_clear_button"] as Boolean)) {
            putButton(maxWidth, mediumHeight, "Reload Data", "Load the config and scan data from saved files and clear the cache")
            { button: ButtonWidget? -> RoadmapController.handleReloadDataPress(button) }
        } else {
            putButton(maxWidth - shortWidth - spacing, mediumHeight, "Reload Data", "Load the config and scan data from saved files and clear the cache")
            { button: ButtonWidget? -> RoadmapController.handleReloadDataPress(button) }
            putButton(shortWidth, mediumHeight, "Clear Data", "Delete all roadmap data for this world (USE WITH CAUTION!)")
            { button: ButtonWidget? -> RoadmapController.handleClearDataPress(button) }
        }
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
        val type = Config.getOption(configId)?.type
        val labelWidth = 120
        val buttonWidth = height
        val fieldWidth =
            if ((type == ConfigType.INT) or (type == ConfigType.DOUBLE)) width - labelWidth - buttonWidth * 2 - spacing * 3
            else width - labelWidth - spacing

        val text = TextWidget(
            pointerX, pointerY, labelWidth, height,
            Text.literal("$label:"),
            MinecraftClient.getInstance().textRenderer
        )
        text.alignLeft()
        addDrawableChild(text)

        // TODO: Add proper list fields
        when (Config.getOption(configId)?.type) {
        ConfigType.BOOLEAN -> {

            // Toggle button
            addDrawableChild(ButtonWidget.builder(
                Text.literal(Config.getOptionString(configId))
            ) { button: ButtonWidget? ->
                run {
                    RoadmapController.handleConfigToggle(label, configId)
                    button?.message = Text.literal(Config.getOptionString(configId))
                }
            }
                .dimensions(pointerX + labelWidth + spacing, pointerY, fieldWidth, height)
                .tooltip(Tooltip.of(Text.literal(tooltip)))
                .build())

        } ConfigType.INT -> {

            // Number field
            val field = TextFieldWidget(
                MinecraftClient.getInstance().textRenderer,
                pointerX + labelWidth + spacing + 1, pointerY + 1, fieldWidth - 2, height - 2,
                Text.literal(label),
            )
            field.setMaxLength(2048)
            field.text = Config.getOptionString(configId)
            field.setChangedListener { value: String? -> RoadmapController.handleConfigChange(value, label, configId) }
            field.setTooltip(Tooltip.of(Text.literal(tooltip)))
            addDrawableChild(field)

            // Increment button
            addDrawableChild(ButtonWidget.builder(
                Text.literal("+")
            ) {
                run {
                    RoadmapController.handleConfigAdjust(1, label, configId)
                    field.text = Config.getOptionString(configId)
                }
            }
                .dimensions(pointerX + labelWidth + fieldWidth + spacing * 2, pointerY, buttonWidth, height)
                .build())

            // Decrement button
            addDrawableChild(ButtonWidget.builder(
                Text.literal("-")
            ) {
                run {
                    RoadmapController.handleConfigAdjust(-1, label, configId)
                    field.text = Config.getOptionString(configId)
                }
            }
                .dimensions(pointerX + labelWidth + fieldWidth + buttonWidth + spacing * 3, pointerY, buttonWidth, height)
                .build())

        } ConfigType.DOUBLE -> {

            // Number field
            val field = TextFieldWidget(
                MinecraftClient.getInstance().textRenderer,
                pointerX + labelWidth + spacing + 1, pointerY + 1, fieldWidth - 2, height - 2,
                Text.literal(label),
            )
            field.setMaxLength(2048)
            field.text = Config.getOptionString(configId)
            field.setChangedListener { value: String? -> RoadmapController.handleConfigChange(value, label, configId) }
            field.setTooltip(Tooltip.of(Text.literal(tooltip)))
            addDrawableChild(field)

            // Increment button
            addDrawableChild(ButtonWidget.builder(
                Text.literal("+")
            ) {
                run {
                    RoadmapController.handleConfigAdjust(1.0, label, configId)
                    field.text = Config.getOptionString(configId)
                }
            }
                .dimensions(pointerX + labelWidth + fieldWidth + spacing * 2, pointerY, buttonWidth, height)
                .build())

            // Decrement button
            addDrawableChild(ButtonWidget.builder(
                Text.literal("-")
            ) {
                run {
                    RoadmapController.handleConfigAdjust(-1.0, label, configId)
                    field.text = Config.getOptionString(configId)
                }
            }
                .dimensions(pointerX + labelWidth + fieldWidth + buttonWidth + spacing * 3, pointerY, buttonWidth, height)
                .build())

        } else -> {

            // Text field
            val field = TextFieldWidget(
                MinecraftClient.getInstance().textRenderer,
                pointerX + labelWidth + spacing + 1, pointerY + 1, fieldWidth - 2, height - 2,
                Text.literal(label),
            )
            field.setMaxLength(2048)
            field.text = Config.getOptionString(configId)
            field.setChangedListener { value: String? -> RoadmapController.handleConfigChange(value, label, configId) }
            field.setTooltip(Tooltip.of(Text.literal(tooltip)))
            addDrawableChild(field)

        }
        }

        movePointer(maxWidth, height)
    }
}
