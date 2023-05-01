package xveon.roadmap

import net.minecraft.util.math.BlockPos
import java.util.*

class ScannerBlockTracker {
    private val scannedPositions = mutableListOf<BlockPos>()
    private val pendingPositions: Queue<BlockPos> = LinkedList()

    fun getScannedPositionCount(): Int {
        return scannedPositions.count()
    }

    fun markPositionScanned(pos: BlockPos): Boolean {
        if (isPositionScanned(pos)) return false
        scannedPositions.add(pos)
        return true
    }

    fun isPositionScanned(pos: BlockPos): Boolean {
        for (scannedPos in scannedPositions)
            if (isPositionNearOtherPosition(pos, scannedPos))
                return true
        return false
    }

    fun getPendingPositionCount(): Int {
        return pendingPositions.count()
    }

    fun hasPendingPositions(): Boolean {
        return pendingPositions.isNotEmpty()
    }

    fun dequeuePendingPosition(): BlockPos {
        return pendingPositions.remove()
    }

    fun enqueuePendingPositions(positions: Set<BlockPos>) {
        for (pos in positions)
            if (!isPositionScanned(pos) and !isPositionPending(pos))
                pendingPositions.add(pos)
    }

    fun isPositionPending(pos: BlockPos): Boolean {
        for (pendingPos in pendingPositions)
            if (isPositionNearOtherPosition(pos, pendingPos))
                return true
        return false
    }

    fun isPositionNearOtherPosition(pos: BlockPos, other: BlockPos): Boolean {
        val xzEqual = (pos.x == other.x) and (pos.z == other.z)
        val yWithinRange = (pos.y >= other.y - 1) and (pos.y <= other.y + 1)
        return xzEqual and yWithinRange
    }
}
