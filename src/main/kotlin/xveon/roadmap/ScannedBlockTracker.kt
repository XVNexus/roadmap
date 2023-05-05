package xveon.roadmap

import net.minecraft.util.math.BlockPos
import java.util.*

class ScannedBlockTracker(private val roadmap: Roadmap) {
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
            if (Util.isPosNearOtherPos(pos, scannedPos))
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
            if (!isPosScanned(pos) && !isPosPending(pos) && !roadmap.testMarker(pos, RoadmapMarkerType.SCAN_FENCE))
                pendingPositions.add(pos)
    }

    fun isPosPending(pos: BlockPos): Boolean {
        for (pendingPos in pendingPositions)
            if (Util.isPosNearOtherPos(pos, pendingPos))
                return true
        return false
    }
}
