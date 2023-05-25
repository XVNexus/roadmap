package xveon.roadmap.util

import net.minecraft.block.BlockState
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import xveon.roadmap.core.RoadmapBlock
import xveon.roadmap.storage.BlockStateCache

object UtilClient {
    fun getBlockState(pos: BlockPos, player: ClientPlayerEntity): BlockState {
        val result = BlockStateCache.getBlockState(pos) ?: player.world.getBlockState(pos)
        if (!BlockStateCache.containsBlockState(pos))
            BlockStateCache.setBlockState(pos, result)
        return result
    }

    fun getFloorBlockAtPos(pos: BlockPos, heightMinMax: Pair<Int, Int>, player: ClientPlayerEntity): RoadmapBlock? {
        for (y in pos.y + heightMinMax.second downTo pos.y - heightMinMax.first) {
            val testPos = BlockPos(pos.x, y, pos.z)
            val testBlock = getBlockState(testPos, player)
            val blockAbove = getBlockState(testPos.add(BlockPos(0, 1, 0)), player)

            if (UtilMain.isBlockSolid(testBlock) && !UtilMain.isBlockSolid(blockAbove))
                return RoadmapBlock.detect(testPos, 0, UtilMain.getRegistryName(testBlock))
        }
        return null
    }

    fun getCeilingBlockAtPos(pos: BlockPos, heightMinMax: Pair<Int, Int>, player: ClientPlayerEntity): RoadmapBlock? {
        for (y in pos.y - heightMinMax.first..pos.y + heightMinMax.second) {
            val testPos = BlockPos(pos.x, y, pos.z)
            val testBlock = getBlockState(testPos, player)
            val blockBelow = getBlockState(testPos.subtract(BlockPos(0, 1, 0)), player)

            if (UtilMain.isBlockSolid(testBlock) && !UtilMain.isBlockSolid(blockBelow))
                return RoadmapBlock.detect(testPos, 0, UtilMain.getRegistryName(testBlock))
        }
        return null
    }

    fun getClearanceOfBlock(block: RoadmapBlock, heightMax: Int, player: ClientPlayerEntity): Int {
        val ceiling = getCeilingBlockAtPos(block.pos.add(0, 1, 0), Pair(0, heightMax), player)
        return if (ceiling != null)
            ceiling.pos.y - block.pos.y - 1
        else
            0
    }
}
