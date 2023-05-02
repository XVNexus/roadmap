package xveon.roadmap

import net.minecraft.util.math.BlockPos
import java.util.*

class ScannedBlockTracker {
    private val scannedPositions = mutableListOf<BlockPos>()
    private val pendingPositions: Queue<BlockPos> = LinkedList()

    fun getScannedPosCount(): Int {
        return scannedPositions.count()
    }

    fun markPosScanned(pos: BlockPos): Boolean {
        if (isPosScanned(pos)) return false
        scannedPositions.add(pos)
        return true
    }

    fun isPosScanned(pos: BlockPos): Boolean {
        for (scannedPos in scannedPositions)
            if (isPosNearOtherPos(pos, scannedPos))
                return true
        return false
    }

    fun getPendingPosCount(): Int {
        return pendingPositions.count()
    }

    fun hasPendingPositions(): Boolean {
        return pendingPositions.isNotEmpty()
    }

    fun dequeuePendingPos(): BlockPos {
        return pendingPositions.remove()
    }

    fun enqueuePendingPositions(positions: Set<BlockPos>) {
        for (pos in positions)
            if (!isPosScanned(pos) and !isPosPending(pos))
                pendingPositions.add(pos)
    }

    fun isPosPending(pos: BlockPos): Boolean {
        for (pendingPos in pendingPositions)
            if (isPosNearOtherPos(pos, pendingPos))
                return true
        return false
    }

    fun isPosNearOtherPos(pos: BlockPos, other: BlockPos): Boolean {
        val xzEqual = (pos.x == other.x) and (pos.z == other.z)
        val yWithinRange = (pos.y >= other.y - 1) and (pos.y <= other.y + 1)
        return xzEqual and yWithinRange
    }
}
