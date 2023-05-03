package xveon.roadmap

import net.minecraft.block.BlockState
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.util.math.BlockPos

class RoadmapScanner(val scannedRoadmap: ScannedRoadmap) {
    fun scan(player: ClientPlayerEntity) {
        // Used to keep track of which blocks are scanned and store a queue of blocks waiting to be scanned
        val tracker = ScannedBlockTracker()

        val originBlock = getFloorBlockAtPos(player.blockPos, Pair(Config["scan_height"] as Int, 0), player) ?: return
        if (!originBlock.isRoad) return

        originBlock.clearance = getClearanceOfBlock(originBlock, Config["scan_height"] as Int, player)
        scannedRoadmap.setBlock(originBlock)
        tracker.markPosScanned(originBlock.pos)
        tracker.enqueuePendingPositions(getAdjacentPositions(originBlock.pos))

        var i = 0
        while (tracker.hasPendingPositions()) {
            i++

            val nextPendingPos = tracker.dequeuePendingPos()
            if (!isPosWithinRangeOfPlayer(nextPendingPos, Config["scan_radius"] as Double, player)) {
                if (!scannedRoadmap.containsBlocks(nextPendingPos, 3))
                    scannedRoadmap.setBlock(ScannedBlock.void(nextPendingPos))
                tracker.markPosScanned(nextPendingPos)
                continue
            }
            val nextScannedBlock = getFloorBlockAtPos(nextPendingPos, Pair(1, 1), player)
            if (nextScannedBlock == null) {
                scannedRoadmap.removeVoid(nextPendingPos, 3)
                scannedRoadmap.setBlock(ScannedBlock.unknown(nextPendingPos))
                tracker.markPosScanned(nextPendingPos)
                continue
            }

            nextScannedBlock.clearance = getClearanceOfBlock(nextScannedBlock, Config["scan_height"] as Int, player)
            scannedRoadmap.removeVoid(nextScannedBlock.pos, 3)
            scannedRoadmap.setBlock(nextScannedBlock)
            tracker.markPosScanned(nextScannedBlock.pos)
            if (nextScannedBlock.isRoad)
                tracker.enqueuePendingPositions(getAdjacentPositions(nextScannedBlock.pos))

            if (i >= Constants.MAX_SCAN_ITERATIONS) {
                RoadmapClient.logger.warn("Scan reached ${Constants.MAX_SCAN_ITERATIONS} iterations, stopping early.")
                return
            }
        }

        RoadmapClient.logger.info("Scan ran for ${i + 1} iterations.")
    }

    fun getAdjacentPositions(pos: BlockPos): Set<BlockPos> {
        return setOf(
            pos.add(-1, 0, -1),
            pos.add(0, 0, -1),
            pos.add(1, 0, -1),
            pos.add(-1, 0, 0),
            pos.add(1, 0, 0),
            pos.add(-1, 0, 1),
            pos.add(0, 0, 1),
            pos.add(1, 0, 1),
        )
    }

    fun isPosWithinRangeOfPlayer(pos: BlockPos, range: Double, player: ClientPlayerEntity): Boolean {
        return pos.isWithinDistance(player.pos, range)
    }

    fun getClearanceOfBlock(block: ScannedBlock, heightMax: Int, player: ClientPlayerEntity): Int {
        val ceiling = getCeilingBlockAtPos(block.pos, Pair(0, heightMax), player)
        return if (ceiling != null)
            ceiling.pos.y - block.pos.y - 1
        else
            0
    }

    fun getCeilingBlockAtPos(pos: BlockPos, heightMinMax: Pair<Int, Int>, player: ClientPlayerEntity): ScannedBlock? {
        for (y in pos.y - heightMinMax.first..pos.y + heightMinMax.second + 1) {
            val testPos = BlockPos(pos.x, y, pos.z)
            val testBlock = getBlockStateOrCachedBlockState(testPos, player)
            val blockBelow = getBlockStateOrCachedBlockState(testPos.subtract(BlockPos(0, 1, 0)), player)

            if (Util.isBlockSolid(testBlock) and !Util.isBlockSolid(blockBelow))
                return ScannedBlock.fromRoadBlockFilter(testPos, 0, Util.getBlockName(testBlock))
        }
        return null
    }

    fun getFloorBlockAtPos(pos: BlockPos, heightMinMax: Pair<Int, Int>, player: ClientPlayerEntity): ScannedBlock? {
        for (y in pos.y + heightMinMax.second downTo pos.y - heightMinMax.first - 1) {
            val testPos = BlockPos(pos.x, y, pos.z)
            val testBlock = getBlockStateOrCachedBlockState(testPos, player)
            val blockAbove = getBlockStateOrCachedBlockState(testPos.add(BlockPos(0, 1, 0)), player)

            if (Util.isBlockSolid(testBlock) and !Util.isBlockSolid(blockAbove))
                return ScannedBlock.fromRoadBlockFilter(testPos, 0, Util.getBlockName(testBlock))
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
