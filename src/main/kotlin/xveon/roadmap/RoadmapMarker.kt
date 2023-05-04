package xveon.roadmap

import net.minecraft.util.math.BlockPos

data class RoadmapMarker(var pos: BlockPos, var type: RoadmapMarkerType) {
    fun testPos(pos: BlockPos): Boolean {
        return (pos.y >= this.pos.y - Constants.MARKER_HEIGHT) and (pos.y <= this.pos.y + Constants.MARKER_HEIGHT)
    }

    override fun toString(): String {
        return "${pos.x} ${pos.y} ${pos.z} ${type.toString().lowercase()}"
    }

    companion object {
        fun fromString(value: String): RoadmapMarker {
            val substrings = value.split(' ')
            val parsedPos = BlockPos(substrings[0].toInt(), substrings[1].toInt(), substrings[2].toInt())
            val parsedType = RoadmapMarkerType.valueOf(substrings[3].uppercase())
            return RoadmapMarker(parsedPos, parsedType)
        }
    }
}
