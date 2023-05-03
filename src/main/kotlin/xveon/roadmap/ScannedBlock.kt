package xveon.roadmap

import net.minecraft.util.math.BlockPos

data class ScannedBlock(var pos: BlockPos, var clearance: Int, var isRoad: Boolean, var name: String) {
    var isTerrain: Boolean
        get() { return isRoad }
        set(value) { isRoad = !value }

    fun isUnknown(): Boolean {
        return name == Constants.UNKNOWN_NAME
    }

    fun isVoid(): Boolean {
        return name == Constants.VOID_NAME
    }

    override fun toString(): String {
        return "${pos.x},${pos.y},${pos.z};$clearance ${(if (isRoad) Constants.ROAD_TAG else Constants.TERRAIN_TAG)}.${Util.compressBlockName(name)}"
    }

    companion object {
        fun fromString(value: String): ScannedBlock {
            val substrings = value.split(' ', ',', ';', '.')
            val parsedPos = BlockPos(substrings[0].toInt(), substrings[1].toInt(), substrings[2].toInt())
            val parsedClearance = substrings[3].toInt()
            val parsedIsRoad = substrings[4][0] == Constants.ROAD_TAG
            val parsedName = Util.expandBlockName(substrings[5])
            return ScannedBlock(parsedPos, parsedClearance, parsedIsRoad, parsedName)
        }

        fun fromRoadBlockFilter(pos: BlockPos, clearance: Int, name: String): ScannedBlock {
            return ScannedBlock(pos, clearance, (Config["road_blocks"] as MutableList<String>).contains(name), name)
        }

        fun road(pos: BlockPos, clearance: Int, name: String): ScannedBlock {
            return ScannedBlock(pos, clearance, true, name)
        }

        fun terrain(pos: BlockPos, clearance: Int, name: String): ScannedBlock {
            return ScannedBlock(pos, clearance, false, name)
        }

        fun unknown(pos: BlockPos, clearance: Int = 0): ScannedBlock {
            return ScannedBlock(pos, clearance, false, Constants.UNKNOWN_NAME)
        }

        fun void(pos: BlockPos): ScannedBlock {
            return ScannedBlock(pos, 0, false, Constants.VOID_NAME)
        }
    }
}
