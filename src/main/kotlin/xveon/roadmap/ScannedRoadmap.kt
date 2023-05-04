package xveon.roadmap

import net.minecraft.util.math.BlockPos

class ScannedRoadmap(var name: String = "") {
    var markers = mutableListOf<RoadmapMarker>()
    var chunks = mutableMapOf<ChunkPos, ScannedChunk>()

    private var chunkFilesToUpdate = mutableMapOf<ChunkPos, Boolean>()
    private var chunkFilesToRemove = mutableSetOf<ChunkPos>()

    fun getMarkers(type: RoadmapMarkerType): MutableList<RoadmapMarker> {
        val result = mutableListOf<RoadmapMarker>()
        for (marker in markers)
            if (marker.type == type)
                result.add(marker)
        return result
    }

    fun addMarker(marker: RoadmapMarker) {
        addMarker(marker.pos, marker.type)
    }

    fun addMarker(pos: BlockPos, type: RoadmapMarkerType) {
        markers.add(RoadmapMarker(pos, type))
    }

    fun removeMarker(marker: RoadmapMarker) {
        removeMarker(marker.pos, marker.type)
    }

    fun removeMarker(pos: BlockPos, type: RoadmapMarkerType) {
        val markersToRemove = mutableListOf<RoadmapMarker>()
        for (marker in markers)
            if ((marker.type == type) and marker.testPos(pos))
                markersToRemove.add(marker)
        for (marker in markersToRemove)
            markers.remove(marker)
    }

    fun clearMarkers(): Boolean {
        if (markers.isEmpty()) return false
        markers.clear()
        return true
    }

    fun testMarker(marker: RoadmapMarker): Boolean {
        return testMarker(marker.pos, marker.type)
    }

    fun testMarker(pos: BlockPos, type: RoadmapMarkerType): Boolean {
        for (marker in markers)
            if ((marker.type == type) and marker.testPos(pos))
                return true
        return false
    }

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

    fun clearBlocks(): Boolean {
        return clearChunks()
    }

    fun containsBlock(pos: BlockPos, rangeY: Int, name: String): Boolean {
        for (y in -rangeY..rangeY + 1)
            if (containsBlock(pos.add(0, y, 0), name))
                return true
        return false
    }

    fun containsBlock(pos: BlockPos, rangeY: Int): Boolean {
        for (y in -rangeY..rangeY + 1)
            if (containsBlock(pos.add(0, y, 0)))
                return true
        return false
    }

    fun containsBlock(pos: BlockPos, name: String): Boolean {
        val nameAtPos = getBlock(pos)?.name ?: return false
        return nameAtPos == name
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
        // Save chunk files
        for (removalPos in chunkFilesToRemove) {
            FileSys.removeFile(Constants.OUTPUT_PATH + Util.genChunkFilename(removalPos))
        }
        chunkFilesToRemove.clear()
        for (updatePos in chunkFilesToUpdate.keys) if (chunkFilesToUpdate[updatePos] == true) {
            val chunk = chunks[updatePos]
            FileSys.writeFile(
                Constants.OUTPUT_PATH + Util.genChunkFilename(chunk?.pos ?: ChunkPos(0, 0)),
                chunk.toString()
            )
            chunkFilesToUpdate[updatePos] = false
        }

        // Save marker file
        var markerString = ""
        for (marker in markers)
            markerString += "\n$marker"
        if (markerString.isNotEmpty())
            FileSys.writeFile(
                Constants.OUTPUT_PATH + Util.genMarkersFilename(),
                markerString.substring(1)
            )
    }

    companion object {
        fun readFiles(): ScannedRoadmap {
            val result = ScannedRoadmap()

            // Load chunk files
            for (file in FileSys.listFiles(Constants.OUTPUT_PATH))
                if (file.nameWithoutExtension.matches(Util.chunkFilenameRegex))
                    result.addChunk(ScannedChunk.fromString(FileSys.readFile(file)))

            // Load marker file
            if (FileSys.containsFile(Constants.OUTPUT_PATH + Util.genMarkersFilename())) {
                val markerString = FileSys.readFile(Util.genMarkersFilename())
                for (line in markerString.split('\n'))
                    result.addMarker(RoadmapMarker.fromString(line))
            }

            return result
        }
    }
}
