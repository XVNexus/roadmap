package xveon.roadmap

import net.minecraft.util.math.BlockPos

data class ScannedBlock(var pos: BlockPos, var name: String, var clearance: Int) {
    override fun toString(): String {
        return if (clearance == 0) {
            "${pos.x} ${pos.y} ${pos.z} $name"
        } else {
            "${pos.x} ${pos.y} ${pos.z} $name $clearance"
        }
    }

    companion object {
        fun fromString(value: String): ScannedBlock {
            val parts = value.split(' ')
            val pos = BlockPos(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
            val block = parts[3]
            var clearance = 0
            if (parts.count() == 5)
                clearance = parts[4].toInt()
            return ScannedBlock(pos, block, clearance)
        }
    }
}
