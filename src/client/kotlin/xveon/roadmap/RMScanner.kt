package xveon.roadmap

import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.registry.Registries
import net.minecraft.util.math.BlockPos

class RMScanner(val map: RMMap) {
    fun scan(player: ClientPlayerEntity) {
        // Create a scan queue for keeping track of touched and untouched blocks
        val q = ScanQueue()
        // Find the surface where the player is standing to start the scan
        val origin = getFloor(player.blockPos, Pair(0, Config.scanHeight), player) ?: return
        // If the origin is not road, cancel the scan
        if (!Config.roadBlocks.contains(origin.name)) return
        // Find the clearance of the origin
        val originCeiling = getCeiling(origin.pos, Pair(Config.scanHeight, 0), player)
        if (originCeiling != null)
            origin.clearance = originCeiling.pos.y - origin.pos.y - 1
        // Add the origin block to the map and mark it scanned
        map.addBlock(origin)
        q.markScanned(origin.pos)
        // Add blocks adjacent to the origin to the scan queue
        q.enqueuePending(origin.scanPos.getAdjPositions())
        // Loop through all pending blocks, enqueueing new adjacent blocks when a block is considered "road"
        var i = 0
        while (q.hasPending()) {
            i++
            // Get the next pending position
            val nextPos = q.dequeuePending()
            // If the position is out of range, mark it scanned and continue to the next iteration
            if (!nextPos.toBlockPos().isWithinDistance(player.pos, Config.scanRadius.toDouble())) {
                q.markScanned(nextPos)
                continue
            }
            // Get the next pending block
            val next = getFloor(nextPos.toBlockPos(), Pair(1, 1), player)
            // If the block isn't found, mark that position scanned and continue to the next iteration
            if (next == null) {
                q.markScanned(nextPos)
                continue
            }
            // Find the clearance of the block
            val nextCeiling = getCeiling(next.pos, Pair(Config.scanHeight, 0), player)
            if (nextCeiling != null)
                next.clearance = nextCeiling.pos.y - next.pos.y - 1
            // Add it to the scan data and mark it scanned
            // (The reason nextPos isn't used is the surface may not be at the same y level that's recorded in nextPos)
            map.addBlock(next)
            q.markScanned(next.scanPos)
            // If it's a road block, add the adjacent blocks to the queue
            if (Config.roadBlocks.contains(next.name))
                q.enqueuePending(next.scanPos.getAdjPositions())
            // If the loop goes on for too long, cancel the scan (already scanned blocks will be saved if the scan is cancelled)
            if (i >= Constants.SCAN_MAX_ITERATIONS) {
                RoadmapClient.logger.warn("Scan reached ${Constants.SCAN_MAX_ITERATIONS} iterations, stopping early.")
                return
            }
        }
        RoadmapClient.logger.info("Scan ran for ${i + 1} iterations.")
    }

    fun getCeiling(pos: BlockPos, scanRange: Pair<Int, Int>, player: ClientPlayerEntity): RMBlock? {
        for (y in pos.y - scanRange.second..pos.y + scanRange.first) {
            val testPos = BlockPos(pos.x, y, pos.z)
            val block = player.world.getBlockState(testPos)
            val blockBelow = player.world.getBlockState(testPos.subtract(BlockPos(0, 1, 0)))
            if (isSolid(block) and !isSolid(blockBelow))
                return RMBlock(testPos, getName(block), 0)
        }
        return null
    }

    fun getFloor(pos: BlockPos, scanRange: Pair<Int, Int>, player: ClientPlayerEntity): RMBlock? {
        for (y in pos.y + scanRange.first downTo pos.y - scanRange.second) {
            val testPos = BlockPos(pos.x, y, pos.z)
            val block = player.world.getBlockState(testPos)
            val blockAbove = player.world.getBlockState(testPos.add(BlockPos(0, 1, 0)))
            if (isSolid(block) and !isSolid(blockAbove))
                return RMBlock(testPos, getName(block), 0)
        }
        return null
    }

    fun isSolid(block: BlockState): Boolean {
        val name = getName(block)
        return if (Config.solidBlocks.contains(name))
            true
        else if (Config.transparentBlocks.contains(name))
            false
        else
            block.isOpaque
    }

    fun getName(block: BlockState): String {
        return getName(block.block)
    }

    fun getName(block: Block): String {
        return Registries.BLOCK.getId(block).toString()
    }
}
