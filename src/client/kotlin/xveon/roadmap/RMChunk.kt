package xveon.roadmap

import net.minecraft.util.math.BlockPos

data class RMChunk(var pos: ChunkPos) {
    var blocks = mutableMapOf<BlockPos, RMBlock>()

    fun getBlock(pos: BlockPos): RMBlock? {
        return if(containsBlock(pos))
            blocks[pos]
        else
            null
    }

    fun setBlock(block: RMBlock) {
        blocks[block.pos] = block
    }

    fun addBlock(block: RMBlock): Boolean {
        if (containsBlock(block.pos)) return false
        setBlock(block)
        return true
    }

    fun replaceBlock(block: RMBlock): Boolean {
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
            result += '\n' + block.toString()
        return result.substring(1)
    }

    companion object {
        fun fromString(value: String): RMChunk {
            val result = RMChunk(ChunkPos(0, 0))
            if (value.isEmpty()) return result
            val lines = value.split('\n')
            result.pos = ChunkPos.fromBlockPos(RMBlock.fromString(lines[0]).pos)
            for (line in lines)
                result.addBlock(RMBlock.fromString(line))
            return result
        }
    }
}
