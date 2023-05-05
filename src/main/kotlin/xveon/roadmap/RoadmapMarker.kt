package xveon.roadmap

import net.minecraft.util.math.BlockPos
import java.io.IOException

data class RoadmapMarker(var pos: BlockPos, var type: RoadmapMarkerType) {
    fun testPos(pos: BlockPos): Boolean {
        val xzEqual = pos.x == this.pos.x && pos.y == this.pos.y
        val yInRange = pos.y >= this.pos.y - Constants.MARKER_HEIGHT && pos.y <= this.pos.y + Constants.MARKER_HEIGHT
        return xzEqual && yInRange
    }

    override fun toString(): String {
        return "${pos.x} ${pos.y} ${pos.z} ${type.toString().lowercase()}"
    }

    companion object {
        fun fromString(value: String): RoadmapMarker {
            val substrings = value.split(' ')
            if (substrings.count() != 4) throw IOException("Roadmap marker data is not formatted properly!")
            val parsedPos = BlockPos(substrings[0].toInt(), substrings[1].toInt(), substrings[2].toInt())
            val parsedType = RoadmapMarkerType.valueOf(substrings[3].uppercase())
            return RoadmapMarker(parsedPos, parsedType)
        }
    }
}
