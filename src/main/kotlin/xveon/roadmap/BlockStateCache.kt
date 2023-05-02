package xveon.roadmap

import net.minecraft.block.BlockState
import net.minecraft.util.math.BlockPos

object BlockStateCache {
    val blockStateMap = mutableMapOf<BlockPos, BlockState>()

    fun getBlockStateAtPos(pos: BlockPos): BlockState? {
        if (!containsBlockStateAtPos(pos)) return null
        return blockStateMap[pos]
    }

    fun setBlockStateAtPos(pos: BlockPos, block: BlockState) {
        blockStateMap[pos] = block
    }

    fun addBlockStateAtPos(pos: BlockPos, block: BlockState): Boolean {
        if (containsBlockStateAtPos(pos)) return false
        setBlockStateAtPos(pos, block)
        return true
    }

    fun replaceBlockAtPos(pos: BlockPos, block: BlockState): Boolean {
        if (!containsBlockStateAtPos(pos)) return false
        setBlockStateAtPos(pos, block)
        return true
    }

    fun removeBlockAtPos(pos: BlockPos): Boolean {
        if (!containsBlockStateAtPos(pos)) return false
        blockStateMap.remove(pos)
        return true
    }

    fun clearBlockStates(): Boolean {
        if (blockStateMap.isEmpty()) return false
        blockStateMap.clear()
        return true
    }

    fun containsBlockStateAtPos(pos: BlockPos): Boolean {
        return blockStateMap.containsKey(pos)
    }
}
