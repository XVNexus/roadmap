package xveon.roadmap

import net.minecraft.util.math.BlockPos

data class ScanPos(var x: Int, var y: Int, var z: Int) {
    fun getAdjPositions(): Set<ScanPos> {
        return setOf(
            ScanPos(x + 1, y, z),
            ScanPos(x, y, z + 1),
            ScanPos(x - 1, y, z),
            ScanPos(x, y, z - 1),
        )
    }

    fun toBlockPos(): BlockPos {
        return BlockPos(x, y, z)
    }

    companion object {
        fun fromBlockPos(pos: BlockPos): ScanPos {
            return ScanPos(pos.x, pos.y, pos.z)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is ScanPos) return false
        val xzEqual = (x == other.x) and (z == other.z)
        val yWithinRange = (y >= other.y - 1) and (y <= other.y + 1)
        return xzEqual and yWithinRange
    }

    override fun hashCode(): Int {
        return x * 65536 + y * 256 + z
    }
}
