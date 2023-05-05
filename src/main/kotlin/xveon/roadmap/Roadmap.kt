package xveon.roadmap

import net.minecraft.util.math.BlockPos
import java.util.Stack

class Roadmap(var name: String = "") {
    var markers = mutableListOf<RoadmapMarker>()
    var chunks = mutableMapOf<ChunkPos, RoadmapChunk>()

    private var opStack = Stack<RoadmapOp>()
    private var chunkFilesToUpdate = mutableMapOf<ChunkPos, Boolean>()
    private var chunkFilesToRemove = mutableSetOf<ChunkPos>()
    // Used internally to prevent the undo function itself from creating undo history while undoing stuff
    private var saveUndoHistory = true

    fun saveState() {
        opStack.clear()
    }

    fun restoreState() {
        saveUndoHistory = false
        while (opStack.isNotEmpty()) {
            val op = opStack.pop()
            when (op.type) {
                RoadmapOpType.ADD_CHUNK -> removeChunk(op.newChunk?.pos ?: continue)
                RoadmapOpType.REMOVE_CHUNK -> addChunk(op.oldChunk ?: continue)
                RoadmapOpType.EDIT_CHUNK -> setChunk(op.oldChunk ?: continue)
                RoadmapOpType.ADD_MARKER -> removeMarker(op.newMarker ?: continue)
                RoadmapOpType.REMOVE_MARKER -> addMarker(op.oldMarker ?: continue)
            }
        }
        saveUndoHistory = true
    }

    fun hasSavedState(): Boolean {
        return opStack.isNotEmpty()
    }

    fun recordOp(op: RoadmapOp) {
        if (saveUndoHistory)
            opStack.push(op)
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
        recordOp(RoadmapOp(RoadmapOpType.ADD_MARKER, newMarker = marker))
        markers.add(marker)
    }

    fun removeMarker(pos: BlockPos, type: RoadmapMarkerType): Boolean {
        return removeMarker(RoadmapMarker(pos, type))
    }

    fun removeMarker(marker: RoadmapMarker): Boolean {
        for (i in 0 until markers.count()) {
            val otherMarker = markers[i]
            if (otherMarker.type == marker.type && otherMarker.testPos(marker.pos)) {
                markers.removeAt(i)
                recordOp(RoadmapOp(RoadmapOpType.REMOVE_MARKER, oldMarker = otherMarker))
                return true
            }
        }
        return false
    }

    fun clearMarkers(): Boolean {
        if (markers.isEmpty()) return false
        for (marker in markers)
            recordOp(RoadmapOp(RoadmapOpType.REMOVE_MARKER, oldMarker = marker))
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
            recordOp(RoadmapOp(RoadmapOpType.EDIT_CHUNK, oldChunk = chunk))
            chunk.setBlock(block)
            chunkFilesToUpdate[chunkPos] = true
        } else {
            val chunk = RoadmapChunk(chunkPos)
            chunk.addBlock(block)
            recordOp(RoadmapOp(RoadmapOpType.ADD_CHUNK, newChunk = chunk))
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
        recordOp(RoadmapOp(RoadmapOpType.EDIT_CHUNK, oldChunk = chunk))
        val result = chunk.removeBlock(pos) ?: false
        chunkFilesToUpdate[chunkPos] = result
        if (result && chunk.blocks.isEmpty())
            removeChunk(chunkPos)
        return result
    }

    fun clearBlocks(): Boolean {
        for (chunk in chunks.values)
            recordOp(RoadmapOp(RoadmapOpType.REMOVE_CHUNK, oldChunk = chunk))
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
        recordOp(RoadmapOp(RoadmapOpType.EDIT_CHUNK, oldChunk = chunk))
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
        val removedChunk = chunks.remove(pos)
        recordOp(RoadmapOp(RoadmapOpType.REMOVE_CHUNK, oldChunk = removedChunk))
        chunkFilesToUpdate.remove(pos)
        chunkFilesToRemove.add(pos)
        return true
    }

    fun clearChunks(): Boolean {
        if (chunks.isEmpty()) return false
        for (chunk in chunks.values) {
            recordOp(RoadmapOp(RoadmapOpType.REMOVE_CHUNK, oldChunk = chunk))
            chunkFilesToRemove.add(chunk.pos)
        }
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
        else if (FileSys.containsFile(Constants.OUTPUT_PATH + Util.genMarkersFilename()))
            FileSys.removeFile(Constants.OUTPUT_PATH + Util.genMarkersFilename())
    }

    companion object {
        fun readFiles(): Roadmap {
            val result = Roadmap()

            // Load chunk files
            for (file in FileSys.listFiles(Constants.OUTPUT_PATH))
                if (file.nameWithoutExtension.matches(Util.chunkFilenameRegex))
                    result.addChunk(RoadmapChunk.fromString(FileSys.readFile(file)))

            // Load marker file
            if (FileSys.containsFile(Constants.OUTPUT_PATH + Util.genMarkersFilename())) {
                val markerString = FileSys.readFile(Constants.OUTPUT_PATH + Util.genMarkersFilename())
                for (line in markerString.split('\n'))
                    result.addMarker(RoadmapMarker.fromString(line))
            }

            return result
        }
    }
}
