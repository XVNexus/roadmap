package xveon.roadmap

import net.minecraft.block.BlockState
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.util.math.BlockPos

class RoadmapScanner(val roadmap: Roadmap) {
    fun scan(player: ClientPlayerEntity): Boolean {
        // Used to keep track of which blocks are scanned and store a queue of blocks waiting to be scanned
        val tracker = ScannedBlockTracker(roadmap)

        val originBlock = getFloorBlockAtPos(player.blockPos, Pair(Config["scan_height"] as Int, 0), player) ?: return false
        if (!originBlock.isRoad) return false

        originBlock.clearance = getClearanceOfBlock(originBlock, Config["scan_height"] as Int, player)
        roadmap.setBlock(originBlock)
        tracker.markPosScanned(originBlock.pos)
        tracker.enqueuePendingPositions(getAdjacentPositions(originBlock.pos))

        var i = 0
        while (tracker.hasPendingPositions()) {
            i++

            val nextPendingPos = tracker.dequeuePendingPos()
            if (!isPosWithinRangeOfPlayer(nextPendingPos, Config["scan_radius"] as Double, player)) {
                if (!roadmap.containsBlock(nextPendingPos, Constants.MARKER_HEIGHT))
                    roadmap.addMarker(nextPendingPos, RoadmapMarkerType.CUTOFF_POINT)
                tracker.markPosScanned(nextPendingPos)
                continue
            }

            val nextScannedBlock = getFloorBlockAtPos(nextPendingPos, Pair(1, 1), player)
            if (nextScannedBlock == null) {
                roadmap.removeMarker(nextPendingPos, RoadmapMarkerType.CUTOFF_POINT)
                tracker.markPosScanned(nextPendingPos)
                continue
            }

            nextScannedBlock.clearance = getClearanceOfBlock(nextScannedBlock, Config["scan_height"] as Int, player)
            roadmap.setBlock(nextScannedBlock)
            roadmap.removeMarker(nextPendingPos, RoadmapMarkerType.CUTOFF_POINT)
            tracker.markPosScanned(nextScannedBlock.pos)
            if (nextScannedBlock.isRoad)
                tracker.enqueuePendingPositions(getAdjacentPositions(nextScannedBlock.pos))

            if (i >= Constants.MAX_SCAN_ITERATIONS) {
                RoadmapClient.logger.warn("Scan reached ${Constants.MAX_SCAN_ITERATIONS} iterations, stopping early.")
                return true
            }
        }

        RoadmapClient.logger.info("Scan ran for ${i + 1} iterations.")
        return true
    }

    fun getAdjacentPositions(pos: BlockPos): Set<BlockPos> {
        return setOf(
            pos.add(0, 0, -1), // North
            pos.add(1, 0, 0), // East
            pos.add(0, 0, 1), // South
            pos.add(-1, 0, 0), // West
        )
    }

    fun isPosWithinRangeOfPlayer(pos: BlockPos, range: Double, player: ClientPlayerEntity): Boolean {
        return pos.isWithinDistance(player.pos, range)
    }

    fun getClearanceOfBlock(block: RoadmapBlock, heightMax: Int, player: ClientPlayerEntity): Int {
        val ceiling = getCeilingBlockAtPos(block.pos, Pair(0, heightMax), player)
        return if (ceiling != null)
            ceiling.pos.y - block.pos.y - 1
        else
            0
    }

    fun getCeilingBlockAtPos(pos: BlockPos, heightMinMax: Pair<Int, Int>, player: ClientPlayerEntity): RoadmapBlock? {
        for (y in pos.y - heightMinMax.first..pos.y + heightMinMax.second) {
            val testPos = BlockPos(pos.x, y, pos.z)
            val testBlock = getBlockStateOrCachedBlockState(testPos, player)
            val blockBelow = getBlockStateOrCachedBlockState(testPos.subtract(BlockPos(0, 1, 0)), player)

            if (Util.isBlockSolid(testBlock) && !Util.isBlockSolid(blockBelow))
                return RoadmapBlock.detect(testPos, 0, Util.getRegistryName(testBlock))
        }
        return null
    }

    fun getFloorBlockAtPos(pos: BlockPos, heightMinMax: Pair<Int, Int>, player: ClientPlayerEntity): RoadmapBlock? {
        for (y in pos.y + heightMinMax.second downTo pos.y - heightMinMax.first) {
            val testPos = BlockPos(pos.x, y, pos.z)
            val testBlock = getBlockStateOrCachedBlockState(testPos, player)
            val blockAbove = getBlockStateOrCachedBlockState(testPos.add(BlockPos(0, 1, 0)), player)

            if (Util.isBlockSolid(testBlock) && !Util.isBlockSolid(blockAbove))
                return RoadmapBlock.detect(testPos, 0, Util.getRegistryName(testBlock))
        }
        return null
    }

    fun getBlockStateOrCachedBlockState(pos: BlockPos, player: ClientPlayerEntity): BlockState {
        val result = BlockStateCache.getBlockState(pos) ?: player.world.getBlockState(pos)
        if (!BlockStateCache.containsBlockState(pos))
            BlockStateCache.setBlockState(pos, result)
        return result
    }
}
