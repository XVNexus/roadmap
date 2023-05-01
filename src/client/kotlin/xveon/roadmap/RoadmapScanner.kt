package xveon.roadmap

import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.registry.Registries
import net.minecraft.util.math.BlockPos

class RoadmapScanner(val scannedRoadmap: ScannedRoadmap) {
    fun scan(player: ClientPlayerEntity) {
        // Used to keep track of which blocks are scanned and store a queue of blocks waiting to be scanned
        val tracker = ScannerBlockTracker()

        val originBlock = getFloorBlockAtPosition(player.blockPos, Pair(Config.scanHeight, 0), player)
            ?: return
        if (!isBlockRoad(originBlock))
            return

        originBlock.clearance = getClearanceOfBlock(originBlock, Config.scanHeight, player)
        scannedRoadmap.addBlock(originBlock)
        tracker.markPositionScanned(originBlock.pos)
        tracker.enqueuePendingPositions(getAdjacentPositions(originBlock.pos))

        var i = 0
        while (tracker.hasPendingPositions()) {
            i++

            val nextPendingPos = tracker.dequeuePendingPosition()
            if (!isPositionWithinRangeOfPlayer(nextPendingPos, Config.scanRadius, player)) {
                tracker.markPositionScanned(nextPendingPos)
                continue
            }
            val nextScannedBlock = getFloorBlockAtPosition(nextPendingPos, Pair(1, 1), player)
            if (nextScannedBlock == null) {
                tracker.markPositionScanned(nextPendingPos)
                continue
            }

            nextScannedBlock.clearance = getClearanceOfBlock(nextScannedBlock, Config.scanHeight, player)
            scannedRoadmap.addBlock(nextScannedBlock)
            tracker.markPositionScanned(nextScannedBlock.pos)
            if (isBlockRoad(nextScannedBlock))
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
            pos.add(1, 0, 0),
            pos.add(0, 0, 1),
            pos.add(-1, 0, 0),
            pos.add(0, 0, -1)
        )
    }

    fun isPositionWithinRangeOfPlayer(pos: BlockPos, range: Double, player: ClientPlayerEntity): Boolean {
        return pos.isWithinDistance(player.pos, range)
    }

    fun getClearanceOfBlock(block: ScannedBlock, heightMax: Int, player: ClientPlayerEntity): Int {
        val ceiling = getCeilingBlockAtPosition(block.pos, Pair(0, heightMax), player)
        return if (ceiling != null)
            ceiling.pos.y - block.pos.y - 1
        else
            0
    }

    fun getCeilingBlockAtPosition(pos: BlockPos, heightMinMax: Pair<Int, Int>, player: ClientPlayerEntity): ScannedBlock? {
        for (y in pos.y - heightMinMax.first..pos.y + heightMinMax.second) {
            val testPos = BlockPos(pos.x, y, pos.z)
            val block = player.world.getBlockState(testPos)
            val blockBelow = player.world.getBlockState(testPos.subtract(BlockPos(0, 1, 0)))
            if (isBlockSolid(block) and !isBlockSolid(blockBelow))
                return ScannedBlock(testPos, getBlockId(block), 0)
        }
        return null
    }

    fun getFloorBlockAtPosition(pos: BlockPos, heightMinMax: Pair<Int, Int>, player: ClientPlayerEntity): ScannedBlock? {
        for (y in pos.y + heightMinMax.second downTo pos.y - heightMinMax.first) {
            val testPos = BlockPos(pos.x, y, pos.z)
            val block = player.world.getBlockState(testPos)
            val blockAbove = player.world.getBlockState(testPos.add(BlockPos(0, 1, 0)))
            if (isBlockSolid(block) and !isBlockSolid(blockAbove))
                return ScannedBlock(testPos, getBlockId(block), 0)
        }
        return null
    }

    fun isBlockRoad(block: ScannedBlock): Boolean {
        return Config.roadBlocks.contains(block.name)
    }

    fun isBlockSolid(block: BlockState): Boolean {
        val name = getBlockId(block)
        return if (Config.solidBlocks.contains(name))
            true
        else if (Config.transparentBlocks.contains(name))
            false
        else
            block.isOpaque
    }

    fun getBlockId(block: BlockState): String {
        return getBlockId(block.block)
    }

    fun getBlockId(block: Block): String {
        return Registries.BLOCK.getId(block).toString()
    }
}
