package xveon.roadmap

import net.minecraft.util.math.BlockPos

data class ScannedBlock(var pos: BlockPos, var clearance: Int, var name: String, var isRoad: Boolean = false) {
    var isTerrain: Boolean
        get() { return isRoad }
        set(value) { isRoad = !value }

    override fun toString(): String {
        return "${pos.x} ${pos.y} ${pos.z} $clearance ${Util.compressBlockName(name)}"
    }

    companion object {
        fun fromString(value: String): ScannedBlock {
            val substrings = value.split(' ')
            val parsedPos = BlockPos(substrings[0].toInt(), substrings[1].toInt(), substrings[2].toInt())
            val parsedClearance = substrings[3].toInt()
            val parsedName = Util.expandBlockName(substrings[4])
            return ScannedBlock(parsedPos, parsedClearance, parsedName)
        }

        fun detect(pos: BlockPos, clearance: Int, name: String): ScannedBlock {
            return ScannedBlock(pos, clearance, name, (Config["road_blocks"] as MutableList<String>).contains(name))
        }

        fun road(pos: BlockPos, clearance: Int, name: String): ScannedBlock {
            return ScannedBlock(pos, clearance, name, true)
        }

        fun terrain(pos: BlockPos, clearance: Int, name: String): ScannedBlock {
            return ScannedBlock(pos, clearance, name, false)
        }
    }
}
