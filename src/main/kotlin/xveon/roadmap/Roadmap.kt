package xveon.roadmap

import net.minecraft.util.math.BlockPos
import java.util.Stack

class Roadmap(var name: String = "") {
    var markers = mutableListOf<RoadmapMarker>()
    var chunks = mutableMapOf<ChunkPos, RoadmapChunk>()

    private var chunkFilesToUpdate = mutableMapOf<ChunkPos, Boolean>()
    private var chunkFilesToRemove = mutableSetOf<ChunkPos>()

    private var undoHistory = Stack<Roadmap>()
    private var redoHistory = Stack<Roadmap>()

    fun saveStateToUndoHistory() {
        undoHistory.push(createClone())
        if (undoHistory.count() > Config["undo_history_limit"] as Int)
            undoHistory.removeAt(0)
        redoHistory.clear()
    }

    fun revertStatesFromUndoHistory(steps: Int) {
        for (i in 0 until steps)
            if (!revertStateFromUndoHistory())
                return
    }

    fun revertStateFromUndoHistory(): Boolean {
        if (undoHistory.isEmpty()) return false
        redoHistory.push(createClone())
        restoreClone(undoHistory.pop())
        return true
    }

    fun reloadStatesFromRedoHistory(steps: Int) {
        for (i in 0 until steps)
            if (!restoreStateFromRedoHistory())
                return
    }

    fun restoreStateFromRedoHistory(): Boolean {
        if (redoHistory.isEmpty()) return false
        undoHistory.push(createClone())
        restoreClone(redoHistory.pop())
        return true
    }

    fun hasUndoHistory(): Boolean {
        return undoHistory.isNotEmpty()
    }

    fun hasRedoHistory(): Boolean {
        return redoHistory.isNotEmpty()
    }

    fun clearUndoRedoHistory() {
        undoHistory.clear()
        redoHistory.clear()
    }

    fun getMarkers(type: RoadmapMarkerType): MutableList<RoadmapMarker> {
        val result = mutableListOf<RoadmapMarker>()
        for (marker in markers)
            if (marker.type == type)
                result.add(marker)
        return result
    }

    fun addMarker(pos: BlockPos, type: RoadmapMarkerType) {
        addMarker(RoadmapMarker(pos, type))
    }

    fun addMarker(marker: RoadmapMarker) {
        markers.add(marker)
    }

    fun removeMarker(pos: BlockPos, type: RoadmapMarkerType): Boolean {
        return removeMarker(RoadmapMarker(pos, type))
    }

    fun removeMarker(marker: RoadmapMarker): Boolean {
        var result = false
        for (i in markers.count() - 1 downTo 0) {
            val otherMarker = markers[i]
            if (otherMarker.type == marker.type && otherMarker.testPos(marker.pos)) {
                markers.removeAt(i)
                result = true
            }
        }
        return result
    }

    fun clearMarkers(type: RoadmapMarkerType): Boolean {
        var result = false
        for (i in markers.count() - 1 downTo 0) {
            val otherMarker = markers[i]
            if (otherMarker.type == type) {
                markers.removeAt(i)
                result = true
            }
        }
        return result
    }

    fun clearMarkers(): Boolean {
        if (markers.isEmpty()) return false
        markers.clear()
        return true
    }

    fun testMarker(pos: BlockPos, type: RoadmapMarkerType): Boolean {
        return testMarker(RoadmapMarker(pos, type))
    }

    fun testMarker(marker: RoadmapMarker): Boolean {
        for (otherMarker in markers)
            if (otherMarker.type == marker.type && otherMarker.testPos(marker.pos))
                return true
        return false
    }

    fun containsMarkers(type: RoadmapMarkerType): Boolean {
        for (marker in markers)
            if (marker.type == type)
                return true
        return false
    }

    fun getBlockCount(): Int {
        var result = 0
        for (chunk in chunks.values) result += chunk.blocks.count()
        return result
    }

    fun getAllBlocks(): Map<BlockPos, RoadmapBlock> {
        val result = mutableMapOf<BlockPos, RoadmapBlock>()
        for (chunk in chunks.values)
            result.putAll(chunk.blocks)
        return result
    }

    fun getBlock(pos: BlockPos): RoadmapBlock? {
        val chunkPos = ChunkPos.fromBlockPos(pos)
        if (!containsChunk(chunkPos)) return null
        return chunks[chunkPos]?.getBlock(pos)
    }

    fun setBlock(block: RoadmapBlock) {
        val chunkPos = ChunkPos.fromBlockPos(block.pos)
        if (containsChunk(chunkPos)) {
            val chunk = getChunk(chunkPos) ?: return
            chunk.setBlock(block)
            chunkFilesToUpdate[chunkPos] = true
        } else {
            val chunk = RoadmapChunk(chunkPos)
            chunk.addBlock(block)
            addChunk(chunk)
        }
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
        val chunkPos = ChunkPos.fromBlockPos(pos)
        val chunk = getChunk(chunkPos) ?: return false
        val result = chunk.removeBlock(pos) ?: false
        chunkFilesToUpdate[chunkPos] = result
        if (result && chunk.blocks.isEmpty())
            removeChunk(chunkPos)
        return result
    }

    fun clearBlocks(): Boolean {
        return clearChunks()
    }

    fun containsBlock(pos: BlockPos, rangeY: Int, name: String): Boolean {
        for (y in -rangeY..rangeY)
            if (containsBlock(pos.add(0, y, 0), name))
                return true
        return false
    }

    fun containsBlock(pos: BlockPos, rangeY: Int): Boolean {
        for (y in -rangeY..rangeY)
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

    fun getChunksInRadius(pos: ChunkPos, radius: Int): Set<RoadmapChunk> {
        val result = mutableSetOf<RoadmapChunk>()
        for (z in -radius..radius) {
            for (x in -radius..radius) {
                val chunk = getChunk(ChunkPos(pos.x + x, pos.z + z))
                if (chunk != null) result.add(chunk)
            }
        }
        return result
    }

    fun getChunk(pos: ChunkPos): RoadmapChunk? {
        return if(containsChunk(pos))
            chunks[pos]
        else
            null
    }

    fun setChunk(chunk: RoadmapChunk) {
        chunks[chunk.pos] = chunk
        chunkFilesToUpdate[chunk.pos] = true
    }

    fun addChunk(chunk: RoadmapChunk): Boolean {
        if (containsChunk(chunk.pos)) return false
        setChunk(chunk)
        return true
    }

    fun replaceChunk(chunk: RoadmapChunk): Boolean {
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
        for (chunk in chunks.values)
            chunkFilesToRemove.add(chunk.pos)
        chunks.clear()
        chunkFilesToUpdate.clear()
        return true
    }

    fun containsChunk(pos: ChunkPos): Boolean {
        return chunks.containsKey(pos)
    }

    fun createClone(): Roadmap {
        val result = Roadmap(name)

        for (marker in markers) {
            val clonedPos = BlockPos(marker.pos.x, marker.pos.y, marker.pos.z)
            result.addMarker(RoadmapMarker(clonedPos, marker.type))
        }

        for (chunk in chunks.values) {
            val clonedChunk = RoadmapChunk(ChunkPos(chunk.pos.x, chunk.pos.z))
            for (block in chunk.blocks.values) {
                val clonedPos = BlockPos(block.pos.x, block.pos.y, block.pos.z)
                clonedChunk.addBlock(RoadmapBlock(clonedPos, block.clearance, block.name, block.isRoad))
            }
            result.addChunk(clonedChunk)
        }

        return result
    }

    fun restoreClone(clone: Roadmap) {
        clearMarkers()
        clearChunks()

        for (clonedMarker in clone.markers) {
            val pos = BlockPos(clonedMarker.pos.x, clonedMarker.pos.y, clonedMarker.pos.z)
            addMarker(RoadmapMarker(pos, clonedMarker.type))
        }

        for (clonedChunk in clone.chunks.values) {
            val chunk = RoadmapChunk(clonedChunk.pos)
            for (clonedBlock in clonedChunk.blocks.values) {
                val pos = BlockPos(clonedBlock.pos.x, clonedBlock.pos.y, clonedBlock.pos.z)
                chunk.addBlock(RoadmapBlock(pos, clonedBlock.clearance, clonedBlock.name, clonedBlock.isRoad))
            }
            addChunk(chunk)
        }
    }

    fun writeFiles(force: Boolean = false) {
        if (!force) writeFilesOptimized()
        else writeFilesForce()
    }

    private fun writeFilesOptimized() {
        // Save marker file
        var markerString = ""
        for (marker in markers)
            markerString += "\n$marker"
        if (markerString.isNotEmpty())
            FileSys.writeFile(
                Constants.OUTPUT_PATH + Util.genMarkersFilename(),
                markerString.substring(1)
            )
        else if (FileSys.containsFile(Constants.OUTPUT_PATH + Util.genMarkersFilename()))
            FileSys.removeFile(Constants.OUTPUT_PATH + Util.genMarkersFilename())

        // Save chunk files
        for (removalPos in chunkFilesToRemove)
            FileSys.removeFile(Constants.OUTPUT_PATH + Util.genChunkFilename(removalPos))
        chunkFilesToRemove.clear()
        for (updatePos in chunkFilesToUpdate.keys) if (chunkFilesToUpdate[updatePos] == true) {
            val chunk = chunks[updatePos]
            FileSys.writeFile(
                Constants.OUTPUT_PATH + Util.genChunkFilename(chunk?.pos ?: ChunkPos(0, 0)),
                chunk.toString()
            )
            chunkFilesToUpdate[updatePos] = false
        }
    }

    private fun writeFilesForce() {
        // Remove all existing files
        for (file in FileSys.listFiles(Constants.OUTPUT_PATH))
            if (file.nameWithoutExtension.matches(Util.roadmapFilenameRegex))
                FileSys.removeFile(file)

        // Reset chunk update and removal trackers
        chunkFilesToUpdate.clear()
        chunkFilesToRemove.clear()

        // Save marker file
        var markerString = ""
        for (marker in markers)
            markerString += "\n$marker"
        if (markerString.isNotEmpty())
            FileSys.writeFile(
                Constants.OUTPUT_PATH + Util.genMarkersFilename(),
                markerString.substring(1)
            )

        // Save all chunk files
        for (chunk in chunks.values) {
            FileSys.writeFile(
                Constants.OUTPUT_PATH + Util.genChunkFilename(chunk.pos),
                chunk.toString()
            )
            chunkFilesToUpdate[chunk.pos] = false
        }
    }

    companion object {
        fun readFiles(): Roadmap {
            val result = Roadmap()

            // Load marker file
            if (FileSys.containsFile(Constants.OUTPUT_PATH + Util.genMarkersFilename())) {
                val markerString = FileSys.readFile(Constants.OUTPUT_PATH + Util.genMarkersFilename())
                for (line in markerString.split('\n'))
                    result.addMarker(RoadmapMarker.fromString(line))
            }

            // Load chunk files
            for (file in FileSys.listFiles(Constants.OUTPUT_PATH))
                if (file.nameWithoutExtension.matches(Util.chunkFilenameRegex))
                    result.addChunk(RoadmapChunk.fromString(FileSys.readFile(file)))

            return result
        }
    }
}
