package xveon.roadmap

import net.minecraft.util.math.BlockPos
import java.util.*

class ScanQueue {
    private val scanned = mutableListOf<ScanPos>()
    private val pending: Queue<ScanPos> = LinkedList()

    fun getScannedCount(): Int {
        return scanned.count()
    }

    fun getPendingCount(): Int {
        return pending.count()
    }

    fun hasPending(): Boolean {
        return pending.isNotEmpty()
    }

    fun markScanned(pos: BlockPos): Boolean {
        return markScanned(ScanPos.fromBlockPos(pos))
    }

    fun markScanned(pos: ScanPos): Boolean {
        if (isScanned(pos)) return false
        scanned.add(pos)
        return true
    }

    fun dequeuePending(): ScanPos {
        return pending.remove()
    }

    fun enqueuePending(positions: Set<ScanPos>) {
        for (pos in positions)
            if (!isScanned(pos) and !isPending(pos))
                pending.add(pos)
    }

    fun isPending(pos: BlockPos): Boolean {
        return isPending(ScanPos.fromBlockPos(pos))
    }

    fun isPending(pos: ScanPos): Boolean {
        return pending.contains(pos)
    }

    fun isScanned(pos: BlockPos): Boolean {
        return isScanned(ScanPos.fromBlockPos(pos))
    }

    fun isScanned(pos: ScanPos): Boolean {
        return scanned.contains(pos)
    }
}
