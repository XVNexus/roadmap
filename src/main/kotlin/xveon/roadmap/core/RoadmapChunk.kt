package xveon.roadmap.core

import net.minecraft.util.math.BlockPos
import xveon.roadmap.util.ChunkPos

data class RoadmapChunk(var pos: ChunkPos) {
    var blocks = mutableMapOf<BlockPos, RoadmapBlock>()

    fun getBlock(pos: BlockPos): RoadmapBlock? {
        return if(containsBlock(pos))
            blocks[pos]
        else
            null
    }

    fun setBlock(block: RoadmapBlock) {
        blocks[block.pos] = block
    }

    fun addBlock(block: RoadmapBlock): Boolean {
        if (containsBlock(block.pos)) return false
        setBlock(block)
        return true
    }

    fun replaceBlock(block: RoadmapBlock): Boolean {
        if (!containsBlock(block.pos)) return false
        setBlock(block)
        return true
    }

    fun removeBlock(pos: BlockPos): Boolean {
        if (!containsBlock(pos)) return false
        blocks.remove(pos)
        return true
    }

    fun clearBlocks(): Boolean {
        if (blocks.isEmpty()) return false
        blocks.clear()
        return true
    }

    fun containsBlock(pos: BlockPos): Boolean {
        return blocks.containsKey(pos)
    }

    override fun toString(): String {
        var result = ""
        for (block in blocks.values)
            result += "\n$block"
        return result.substring(1)
    }

    companion object {
        fun fromString(value: String): RoadmapChunk {
            val result = RoadmapChunk(ChunkPos(0, 0))
            if (value.isEmpty()) return result
            val lines = value.split('\n')
            result.pos = ChunkPos.fromBlockPos(RoadmapBlock.fromString(lines[0]).pos)
            for (line in lines)
                result.addBlock(RoadmapBlock.fromString(line))
            return result
        }
    }
}
