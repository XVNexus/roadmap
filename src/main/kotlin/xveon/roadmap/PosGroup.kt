package xveon.roadmap

import net.minecraft.util.math.BlockPos
import org.joml.Vector3d
import kotlin.math.roundToInt

class PosGroup {
    private val positions = mutableSetOf<BlockPos>()
    var center = BlockPos(0, 0, 0)

    fun getPositions(): MutableSet<BlockPos> {
        return positions
    }

    fun getPosCount(): Int {
        return positions.count()
    }

    fun addPos(pos: BlockPos): Boolean {
        val result = positions.add(pos)
        if (result) calculateCenter()
        return result
    }

    fun removePos(pos: BlockPos): Boolean {
        val result = positions.remove(pos)
        if (result) calculateCenter()
        return result
    }

    fun clearPositions(): Boolean {
        if (positions.isEmpty()) return false
        positions.clear()
        center = BlockPos(0, 0, 0)
        return true
    }

    fun containsPos(pos: BlockPos): Boolean {
        return positions.contains(pos)
    }

    fun isPosNear(pos: BlockPos, range: Int): Boolean {
        val centerVec = Vector3d(center.x.toDouble(), center.y.toDouble(), center.z.toDouble())
        val posVec = Vector3d(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())
        return centerVec.distance(posVec) <= range
    }

    fun calculateCenter() {
        var result = BlockPos(0, 0, 0)
        for (pos in positions)
            result = result.add(pos)
        val count = positions.count().toDouble()
        center = BlockPos((result.x / count).roundToInt(), (result.y / count).roundToInt(), (result.z / count).roundToInt())
    }
}
