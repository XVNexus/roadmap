package xveon.roadmap

import net.minecraft.util.math.BlockPos

data class ScannedBlock(var pos: BlockPos, var clearance: Int, var isRoad: Boolean, var name: String) {
    override fun toString(): String {
        return "${pos.x},${pos.y},${pos.z};$clearance ${(if (isRoad) ScannedBlock.ROAD_TAG else ScannedBlock.TERRAIN_TAG)}.$name"
    }

    companion object {
        fun fromString(value: String): ScannedBlock {
            val substrings = value.split(' ', ',', ';', '.')
            val parsedPos = BlockPos(substrings[0].toInt(), substrings[1].toInt(), substrings[2].toInt())
            val parsedClearance = substrings[3].toInt()
            val parsedIsRoad = substrings[4][0] == ScannedBlock.ROAD_TAG
            val parsedName = substrings[5]
            return ScannedBlock(parsedPos, parsedClearance, parsedIsRoad, parsedName)
        }

        fun fromRoadBlockFilter(pos: BlockPos, clearance: Int, name: String): ScannedBlock {
            return ScannedBlock(pos, clearance, (Config["road_blocks"] as MutableList<String>).contains(name), name)
        }

        fun asRoad(pos: BlockPos, clearance: Int, name: String): ScannedBlock {
            return ScannedBlock(pos, clearance, true, name)
        }

        fun asTerrain(pos: BlockPos, clearance: Int, name: String): ScannedBlock {
            return ScannedBlock(pos, clearance, false, name)
        }

        fun asVoid(pos: BlockPos, clearance: Int = 0): ScannedBlock {
            return ScannedBlock(pos, clearance, false, "_")
        }

        const val ROAD_TAG = 'r'
        const val TERRAIN_TAG = 't'
    }
}
