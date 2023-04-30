package xveon.roadmap

import net.minecraft.util.math.BlockPos

class RMMap {
    var chunks = mutableMapOf<ChunkPos, RMChunk>()
    var chunkUpdates = mutableMapOf<ChunkPos, Boolean>()
    var chunkRemovals = mutableSetOf<ChunkPos>()

    fun getBlockCount(): Int {
        var result = 0
        for (chunk in chunks.values) result += chunk.blocks.count()
        return result
    }

    fun getBlock(pos: BlockPos): RMBlock? {
        val chunkPos = ChunkPos.fromBlockPos(pos)
        if (!containsChunk(chunkPos)) return null
        return chunks[chunkPos]?.getBlock(pos)
    }

    fun setBlock(block: RMBlock) {
        val chunkPos = ChunkPos.fromBlockPos(block.pos)
        if (containsChunk(chunkPos)) {
            chunks[chunkPos]?.addBlock(block)
            chunkUpdates[chunkPos] = true
        } else {
            val chunk = RMChunk(chunkPos)
            chunk.addBlock(block)
            addChunk(chunk)
        }
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
        val chunkPos = ChunkPos.fromBlockPos(pos)
        if (!containsChunk(chunkPos)) return false
        val chunk = getChunk(chunkPos)
        val result = chunk?.removeBlock(pos) ?: false
        chunkUpdates[chunkPos] = result
        if (result and (chunk?.blocks.isNullOrEmpty())) removeChunk(chunkPos)
        return result
    }

    fun clearBlocks(): Boolean {
        return clearChunks()
    }

    fun containsBlock(pos: BlockPos): Boolean {
        val chunkPos = ChunkPos.fromBlockPos(pos)
        if (!containsChunk(chunkPos)) return false
        return chunks[chunkPos]?.containsBlock(pos) ?: false
    }

    fun getChunk(pos: ChunkPos): RMChunk? {
        return if(containsChunk(pos))
            chunks[pos]
        else
            null
    }

    fun setChunk(chunk: RMChunk) {
        chunks[chunk.pos] = chunk
        chunkUpdates[chunk.pos] = true
    }

    fun addChunk(chunk: RMChunk): Boolean {
        if (containsChunk(chunk.pos)) return false
        setChunk(chunk)
        return true
    }

    fun replaceChunk(chunk: RMChunk): Boolean {
        if (!containsChunk(chunk.pos)) return false
        setChunk(chunk)
        return true
    }

    fun removeChunk(pos: ChunkPos): Boolean {
        if (!containsChunk(pos)) return false
        chunks.remove(pos)
        chunkUpdates.remove(pos)
        chunkRemovals.add(pos)
        return true
    }

    fun clearChunks(): Boolean {
        if (chunks.isEmpty()) return false
        for (pos in chunks.keys) chunkRemovals.add(pos)
        chunks.clear()
        chunkUpdates.clear()
        return true
    }

    fun containsChunk(pos: ChunkPos): Boolean {
        return chunks.containsKey(pos)
    }

    fun writeFiles() {
        for (removalPos in chunkRemovals) FS.removeFile(removalPos.toFilename())
        for (updatePos in chunkUpdates.keys) if (chunkUpdates[updatePos] ?: false) {
            val chunk = chunks[updatePos]
            FS.writeFile(Constants.SCAN_FOLDER_PATH + (chunk?.pos?.toFilename() ?: "scan_null.${Constants.SCAN_FILE_EXTENSION}"), chunk.toString())
        }
    }

    companion object {
        fun readFiles(): RMMap {
            val result = RMMap()
            for (file in FS.listFiles(Constants.SCAN_FOLDER_PATH))
                if (Regex("scan_(-?\\d+)_(-?\\d+)").matches(file.nameWithoutExtension))
                    result.addChunk(RMChunk.fromString(FS.readFile(file)))
            return result
        }
    }
}
