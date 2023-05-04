package xveon.roadmap

import net.minecraft.util.math.BlockPos

data class ScannedChunk(var pos: ChunkPos) {
    var blocks = mutableMapOf<BlockPos, ScannedBlock>()

    fun getBlock(pos: BlockPos): ScannedBlock? {
        return if(containsBlock(pos))
            blocks[pos]
        else
            null
    }

    fun setBlock(block: ScannedBlock) {
        blocks[block.pos] = block
    }

    fun addBlock(block: ScannedBlock): Boolean {
        if (containsBlock(block.pos)) return false
        setBlock(block)
        return true
    }

    fun replaceBlock(block: ScannedBlock): Boolean {
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
        fun fromString(value: String): ScannedChunk {
            val result = ScannedChunk(ChunkPos(0, 0))
            if (value.isEmpty()) return result
            val lines = value.split('\n')
            result.pos = ChunkPos.fromBlockPos(ScannedBlock.fromString(lines[0]).pos)
            for (line in lines)
                result.addBlock(ScannedBlock.fromString(line))
            return result
        }
    }
}
