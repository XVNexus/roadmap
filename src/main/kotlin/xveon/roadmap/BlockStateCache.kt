package xveon.roadmap

import net.minecraft.block.BlockState
import net.minecraft.util.math.BlockPos

object BlockStateCache {
    val blockStateMap = mutableMapOf<BlockPos, BlockState>()

    fun getBlockState(pos: BlockPos): BlockState? {
        if (!containsBlockState(pos)) return null
        return blockStateMap[pos]
    }

    fun setBlockState(pos: BlockPos, block: BlockState) {
        blockStateMap[pos] = block
    }

    fun addBlockState(pos: BlockPos, block: BlockState): Boolean {
        if (containsBlockState(pos)) return false
        setBlockState(pos, block)
        return true
    }

    fun replaceBlock(pos: BlockPos, block: BlockState): Boolean {
        if (!containsBlockState(pos)) return false
        setBlockState(pos, block)
        return true
    }

    fun removeBlock(pos: BlockPos): Boolean {
        if (!containsBlockState(pos)) return false
        blockStateMap.remove(pos)
        return true
    }

    fun clearBlockStates(): Boolean {
        if (blockStateMap.isEmpty()) return false
        blockStateMap.clear()
        return true
    }

    fun containsBlockState(pos: BlockPos): Boolean {
        return blockStateMap.containsKey(pos)
    }
}
