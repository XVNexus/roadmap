package xveon.roadmap

import net.minecraft.util.math.BlockPos

class ScannedRoadmap {
    var scannedChunks = mutableMapOf<ChunkPos, ScannedChunk>()
    var chunkFilesToUpdate = mutableMapOf<ChunkPos, Boolean>()
    var chunkFilesToRemove = mutableSetOf<ChunkPos>()

    fun getBlockCount(): Int {
        var result = 0
        for (chunk in scannedChunks.values) result += chunk.blocks.count()
        return result
    }

    fun getBlock(pos: BlockPos): ScannedBlock? {
        val chunkPos = ChunkPos.fromBlockPosition(pos)
        if (!containsChunk(chunkPos)) return null
        return scannedChunks[chunkPos]?.getBlock(pos)
    }

    fun setBlock(block: ScannedBlock) {
        val chunkPos = ChunkPos.fromBlockPosition(block.pos)
        if (containsChunk(chunkPos)) {
            scannedChunks[chunkPos]?.addBlock(block)
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
        val chunkPos = ChunkPos.fromBlockPosition(pos)
        if (!containsChunk(chunkPos)) return false
        val chunk = getChunk(chunkPos)
        val result = chunk?.removeBlock(pos) ?: false
        chunkFilesToUpdate[chunkPos] = result
        if (result and (chunk?.blocks.isNullOrEmpty())) removeChunk(chunkPos)
        return result
    }

    fun clearBlocks(): Boolean {
        return clearChunks()
    }

    fun containsBlock(pos: BlockPos): Boolean {
        val chunkPos = ChunkPos.fromBlockPosition(pos)
        if (!containsChunk(chunkPos)) return false
        return scannedChunks[chunkPos]?.containsBlock(pos) ?: false
    }

    fun getChunk(pos: ChunkPos): ScannedChunk? {
        return if(containsChunk(pos))
            scannedChunks[pos]
        else
            null
    }

    fun setChunk(chunk: ScannedChunk) {
        scannedChunks[chunk.pos] = chunk
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
        scannedChunks.remove(pos)
        chunkFilesToUpdate.remove(pos)
        chunkFilesToRemove.add(pos)
        return true
    }

    fun clearChunks(): Boolean {
        if (scannedChunks.isEmpty()) return false
        for (pos in scannedChunks.keys) chunkFilesToRemove.add(pos)
        scannedChunks.clear()
        chunkFilesToUpdate.clear()
        return true
    }

    fun containsChunk(pos: ChunkPos): Boolean {
        return scannedChunks.containsKey(pos)
    }

    fun writeFiles() {
        for (removalPos in chunkFilesToRemove) FileSys.removeFile(removalPos.toFilename())
        for (updatePos in chunkFilesToUpdate.keys) if (chunkFilesToUpdate[updatePos] ?: false) {
            val chunk = scannedChunks[updatePos]
            FileSys.writeFile(
                Constants.SCAN_FOLDER_PATH + (chunk?.pos?.toFilename() ?: "scan_null.${Constants.SCAN_FILE_EXTENSION}"),
                chunk.toString()
            )
        }
    }

    companion object {
        fun readFiles(): ScannedRoadmap {
            val result = ScannedRoadmap()
            for (file in FileSys.listFiles(Constants.SCAN_FOLDER_PATH))
                if (Regex("scan_(-?\\d+)_(-?\\d+)").matches(file.nameWithoutExtension))
                    result.addChunk(ScannedChunk.fromString(FileSys.readFile(file)))
            return result
        }
    }
}
