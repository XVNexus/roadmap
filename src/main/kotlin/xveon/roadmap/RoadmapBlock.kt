package xveon.roadmap

import net.minecraft.util.math.BlockPos
import java.io.IOException

data class RoadmapBlock(var pos: BlockPos, var clearance: Int, var name: String, var isRoad: Boolean = false) {
    var isTerrain: Boolean
        get() { return isRoad }
        set(value) { isRoad = !value }

    override fun toString(): String {
        return "${pos.x} ${pos.y} ${pos.z} $clearance ${Util.compressBlockName(name)} ${if (isRoad) Constants.ROAD_TAG else Constants.TERRAIN_TAG}"
    }

    companion object {
        fun fromString(value: String): RoadmapBlock {
            val substrings = value.split(' ')
            if (substrings.count() != 6) throw IOException("Roadmap block data is not formatted properly!")
            val parsedPos = BlockPos(substrings[0].toInt(), substrings[1].toInt(), substrings[2].toInt())
            val parsedClearance = substrings[3].toInt()
            val parsedName = Util.expandBlockName(substrings[4])
            val parsedIsRoad = substrings[5] == Constants.ROAD_TAG
            return RoadmapBlock(parsedPos, parsedClearance, parsedName, parsedIsRoad)
        }

        fun road(pos: BlockPos, clearance: Int, name: String): RoadmapBlock {
            return RoadmapBlock(pos, clearance, name, true)
        }

        fun terrain(pos: BlockPos, clearance: Int, name: String): RoadmapBlock {
            return RoadmapBlock(pos, clearance, name, false)
        }

        fun detect(pos: BlockPos, clearance: Int, name: String): RoadmapBlock {
            return RoadmapBlock(pos, clearance, name, (Config["road_blocks"] as MutableList<String>).contains(name))
        }
    }
}
