package xveon.roadmap

import net.minecraft.util.math.BlockPos

class ScannedRoadmap(var name: String = "") {
    var chunks = mutableMapOf<ChunkPos, ScannedChunk>()
    var chunkFilesToUpdate = mutableMapOf<ChunkPos, Boolean>()
    var chunkFilesToRemove = mutableSetOf<ChunkPos>()

    fun getBlockCount(): Int {
        var result = 0
        for (chunk in chunks.values) result += chunk.blocks.count()
        return result
    }

    fun getAllBlocks(): Map<BlockPos, ScannedBlock> {
        val result = mutableMapOf<BlockPos, ScannedBlock>()
        for (chunk in chunks.values)
            result.putAll(chunk.blocks)
        return result
    }

    fun getBlock(pos: BlockPos): ScannedBlock? {
        val chunkPos = ChunkPos.fromBlockPos(pos)
        if (!containsChunk(chunkPos)) return null
        return chunks[chunkPos]?.getBlock(pos)
    }

    fun setBlock(block: ScannedBlock) {
        val chunkPos = ChunkPos.fromBlockPos(block.pos)
        if (containsChunk(chunkPos)) {
            chunks[chunkPos]?.setBlock(block)
            chunkFilesToUpdate[chunkPos] = true
        } else {
            val chunk = ScannedChunk(chunkPos)
            chunk.addBlock(block)
            addChunk(chunk)
        }
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
        val chunkPos = ChunkPos.fromBlockPos(pos)
        if (!containsChunk(chunkPos)) return false
        val chunk = getChunk(chunkPos)
        val result = chunk?.removeBlock(pos) ?: false
        chunkFilesToUpdate[chunkPos] = result
        if (result and (chunk?.blocks.isNullOrEmpty())) removeChunk(chunkPos)
        return result
    }

    fun removeVoid(pos: BlockPos, rangeY: Int): Boolean {
        var result = false
        for (y in -rangeY..rangeY + 1) {
            val removalPos = pos.add(0, y, 0)
            if (containsBlock(removalPos, Constants.VOID_NAME)) {
                removeBlock(removalPos)
                result = true
            }
        }
        return result
    }

    fun clearBlocks(): Boolean {
        return clearChunks()
    }

    fun containsBlocks(pos: BlockPos, rangeY: Int, name: String): Boolean {
        for (y in -rangeY..rangeY + 1)
            if (containsBlock(pos.add(0, y, 0), name))
                return true
        return false
    }

    fun containsBlocks(pos: BlockPos, rangeY: Int): Boolean {
        for (y in -rangeY..rangeY + 1)
            if (containsBlock(pos.add(0, y, 0)))
                return true
        return false
    }

    fun containsBlock(pos: BlockPos, name: String): Boolean {
        val nameAtPos = getBlock(pos)?.name ?: return false
        return name == nameAtPos
    }

    fun containsBlock(pos: BlockPos): Boolean {
        val chunkPos = ChunkPos.fromBlockPos(pos)
        if (!containsChunk(chunkPos)) return false
        return chunks[chunkPos]?.containsBlock(pos) ?: false
    }

    fun getChunksInRadius(pos: ChunkPos, radius: Int): Set<ScannedChunk> {
        val result = mutableSetOf<ScannedChunk>()
        for (z in -radius..radius + 1) {
            for (x in -radius..radius + 1) {
                val chunk = getChunk(ChunkPos(pos.x + x, pos.z + z))
                if (chunk != null) result.add(chunk)
            }
        }
        return result
    }

    fun getChunk(pos: ChunkPos): ScannedChunk? {
        return if(containsChunk(pos))
            chunks[pos]
        else
            null
    }

    fun setChunk(chunk: ScannedChunk) {
        chunks[chunk.pos] = chunk
        chunkFilesToUpdate[chunk.pos] = true
    }

    fun addChunk(chunk: ScannedChunk): Boolean {
        if (containsChunk(chunk.pos)) return false
        setChunk(chunk)
        return true
    }

    fun replaceChunk(chunk: ScannedChunk): Boolean {
        if (!containsChunk(chunk.pos)) return false
        setChunk(chunk)
        return true
    }

    fun removeChunk(pos: ChunkPos): Boolean {
        if (!containsChunk(pos)) return false
        chunks.remove(pos)
        chunkFilesToUpdate.remove(pos)
        chunkFilesToRemove.add(pos)
        return true
    }

    fun clearChunks(): Boolean {
        if (chunks.isEmpty()) return false
        for (pos in chunks.keys) chunkFilesToRemove.add(pos)
        chunks.clear()
        chunkFilesToUpdate.clear()
        return true
    }

    fun containsChunk(pos: ChunkPos): Boolean {
        return chunks.containsKey(pos)
    }

    fun writeFiles() {
        for (removalPos in chunkFilesToRemove) {
            FileSys.removeFile(Constants.OUTPUT_PATH + removalPos.toFilename())
        }
        chunkFilesToRemove.clear()
        for (updatePos in chunkFilesToUpdate.keys) if (chunkFilesToUpdate[updatePos] == true) {
            val chunk = chunks[updatePos]
            FileSys.writeFile(
                Constants.OUTPUT_PATH + (chunk?.pos?.toFilename() ?: "null.${Constants.SCAN_FILE_EXTENSION}"),
                chunk.toString()
            )
            chunkFilesToUpdate[updatePos] = false
        }
    }

    companion object {
        fun readFiles(): ScannedRoadmap {
            val result = ScannedRoadmap()
            for (file in FileSys.listFiles(Constants.OUTPUT_PATH))
                if (Regex("scan_(-?\\d+)_(-?\\d+)").matches(file.nameWithoutExtension))
                    result.addChunk(ScannedChunk.fromString(FileSys.readFile(file)))
            return result
        }
    }
}
