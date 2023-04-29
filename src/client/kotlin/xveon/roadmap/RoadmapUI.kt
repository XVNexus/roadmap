package xveon.roadmap

import io.wispforest.owo.ui.base.BaseOwoScreen
import io.wispforest.owo.ui.component.ButtonComponent
import io.wispforest.owo.ui.component.Components
import io.wispforest.owo.ui.container.Containers
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.core.*
import net.minecraft.text.Text

class RoadmapUI : BaseOwoScreen<FlowLayout>() {
    override fun createAdapter(): OwoUIAdapter<FlowLayout> {
        return OwoUIAdapter.create(this) { horizontalSizing: Sizing?, verticalSizing: Sizing? ->
            Containers.verticalFlow(horizontalSizing, verticalSizing)
        }
    }

    override fun build(rootComponent: FlowLayout?) {
        if (rootComponent == null) return
        rootComponent
            .surface(Surface.VANILLA_TRANSLUCENT)
            .horizontalAlignment(HorizontalAlignment.CENTER)
            .verticalAlignment(VerticalAlignment.CENTER)
        rootComponent.child(
            Components.button(
                Text.literal("hi lol")
            ) { button: ButtonComponent? -> RoadmapClient.logger.error("button click!1!") }
        )
    }
}
