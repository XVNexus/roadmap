package xveon.roadmap.core

import net.minecraft.client.network.ClientPlayerEntity
import xveon.roadmap.storage.Config
import xveon.roadmap.storage.Constants
import xveon.roadmap.util.ScannedBlockTracker
import xveon.roadmap.util.UtilClient
import xveon.roadmap.util.UtilMain.getAdjPositions
import xveon.roadmap.util.UtilMain.isInRange

class RoadmapScanner(val roadmap: Roadmap) {
    fun scan(player: ClientPlayerEntity): Boolean {
        // Used to keep track of which blocks are scanned and store a queue of blocks waiting to be scanned
        val tracker = ScannedBlockTracker(roadmap)

        val originBlock = UtilClient.getFloorBlockAtPos(player.blockPos, Pair(Config["scan_height"] as Int, 0), player) ?: return false
        if (!originBlock.isRoad) return false

        originBlock.clearance = UtilClient.getClearanceOfBlock(originBlock, Config["scan_height"] as Int, player)
        roadmap.setBlock(originBlock)
        roadmap.removeMarker(originBlock.pos, RoadmapMarkerType.CUTOFF_POINT)
        tracker.markPosScanned(originBlock.pos)
        tracker.enqueuePendingPositions(originBlock.pos.getAdjPositions())

        var i = 0
        while (tracker.hasPendingPositions()) {
            i++

            val nextPendingPos = tracker.dequeuePendingPos()
            if (!nextPendingPos.isInRange(player.pos, Config["scan_radius"] as Double)) {
                if (!roadmap.containsBlock(nextPendingPos, Constants.MARKER_HEIGHT))
                    roadmap.addMarker(nextPendingPos, RoadmapMarkerType.CUTOFF_POINT)
                tracker.markPosScanned(nextPendingPos)
                continue
            }

            val nextScannedBlock = UtilClient.getFloorBlockAtPos(nextPendingPos, Pair(1, 1), player)
            if (nextScannedBlock == null) {
                roadmap.setBlock(RoadmapBlock(nextPendingPos, 0, "_"))
                roadmap.removeMarker(nextPendingPos, RoadmapMarkerType.CUTOFF_POINT)
                tracker.markPosScanned(nextPendingPos)
                continue
            }

            nextScannedBlock.clearance = UtilClient.getClearanceOfBlock(nextScannedBlock, Config["scan_height"] as Int, player)
            roadmap.setBlock(nextScannedBlock)
            roadmap.removeMarker(nextPendingPos, RoadmapMarkerType.CUTOFF_POINT)
            tracker.markPosScanned(nextScannedBlock.pos)
            if (nextScannedBlock.isRoad)
                tracker.enqueuePendingPositions(nextScannedBlock.pos.getAdjPositions())

            if (i >= Constants.MAX_SCAN_ITERATIONS) {
                RoadmapClient.logger.warn("Scan reached ${Constants.MAX_SCAN_ITERATIONS} iterations, stopping early.")
                return true
            }
        }

        RoadmapClient.logger.info("Scan ran for ${i + 1} iterations.")
        return true
    }
}
