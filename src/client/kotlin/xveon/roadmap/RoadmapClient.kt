package xveon.roadmap

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import net.minecraft.registry.Registries
import net.minecraft.text.Text
import net.minecraft.util.math.BlockPos
import org.lwjgl.glfw.GLFW
import org.slf4j.LoggerFactory
import java.io.File
import java.util.*

object RoadmapClient : ClientModInitializer {
    val logger = LoggerFactory.getLogger("roadmap")
    private lateinit var kbScan: KeyBinding
    private lateinit var kbUi: KeyBinding
    private var map = RMMap()
    private var config = RMConfig()
    private var ui = RoadmapUI()

    override fun onInitializeClient() {
        // This entrypoint is suitable for setting up client-specific logic, such as rendering.
        logger.info("Initializing roadmap client...")

        if (RMFS.containsFile("config.json")) {
            logger.info("Found config file, restoring settings to memory...")
            config.loadFile("config.json")
        } else {
            logger.info("Creating new config file...")
        }
        config.saveFile("config.json")

        if (RMFS.containsFiles("scan/")) {
            logger.info("Found previous scan data, restoring to memory...")
            map = RMMap.readFiles("scan/")
            logger.info("Loaded ${map.getBlockCount()} previously scanned blocks.")
        }

        kbScan = KeyBindingHelper.registerKeyBinding(
            KeyBinding(
                "key.roadmap.scan",  // The translation key of the keybinding's name
                InputUtil.Type.KEYSYM,  // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
                GLFW.GLFW_KEY_R,  // The keycode of the key
                "category.roadmap.main" // The translation key of the keybinding's category.
            )
        )

        kbUi = KeyBindingHelper.registerKeyBinding(
            KeyBinding(
                "key.roadmap.ui",  // The translation key of the keybinding's name
                InputUtil.Type.KEYSYM,  // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
                GLFW.GLFW_KEY_G,  // The keycode of the key
                "category.roadmap.main" // The translation key of the keybinding's category.
            )
        )

        ClientTickEvents.END_CLIENT_TICK.register(ClientTickEvents.EndTick { client: MinecraftClient ->
            while (kbScan.wasPressed()) {
                val player: ClientPlayerEntity = MinecraftClient.getInstance().player ?: break
                logger.info("Scanning surrounding blocks...")
                val scanner = RMScanner(map)
                val startBlockCount = map.getBlockCount()
                scanner.scan(player, config.scanRadius, config.scanHeight, config.roadBlocks)
                val endBlockCount = map.getBlockCount()
                val newBlocks = endBlockCount - startBlockCount
                if (newBlocks > 0) {
                    map.writeFiles("scan/")
                    logger.info("Saved $newBlocks blocks to scan data.")
                    displayPopupText("Saved $newBlocks blocks to scan data", player)
                } else {
                    logger.info("No new blocks were saved to scan data.")
                    displayPopupText("No new blocks were saved to scan data", player)
                }
            }
        })

        ClientTickEvents.END_CLIENT_TICK.register(ClientTickEvents.EndTick { client: MinecraftClient ->
            while (kbUi.wasPressed()) {
                logger.info("Opening ui...")
                ui.init(client, 100, 100)
            }
        })
    }

    fun displayPopupText(text: String, player: ClientPlayerEntity) {
        player.sendMessage(Text.literal("Roadmap: $text"), true)
    }
}

class RMScanner(val map: RMMap) {
    fun scan(player: ClientPlayerEntity, scanRadius: Int, scanHeight: Int, roadBlocks: List<String>, iterLimit: Int = 1000000) {
        // Create a scan queue for keeping track of touched and untouched blocks
        val q = ScanQueue()
        // Find the surface where the player is standing to start the scan
        val origin = getFloor(player.blockPos, Pair(0, scanHeight), player) ?: return
        // Find the clearance of the origin
        val originCeiling = getCeiling(origin.pos, Pair(scanHeight, 0), player)
        if (originCeiling != null)
            origin.clearance = originCeiling.pos.y - origin.pos.y - 1
        // If the origin is not road, cancel the scan
        if (!roadBlocks.contains(origin.name)) return
        // Add the origin block to the map and mark it scanned
        map.addBlock(origin)
        q.markScanned(origin.pos)
        // Add blocks adjacent to the origin to the scan queue
        q.enqueuePending(origin.scanPos.getAdjPositions())
        // Loop through all pending blocks, enqueueing new adjacent blocks when a block is considered "road"
        var i = 0
        while (q.hasPending()) {
            i++
            // Get the next pending position
            val nextPos = q.dequeuePending()
            // Get the next pending block
            val next = getFloor(nextPos.toBlockPos(), Pair(1, 1), player)
            // If the block isn't found, mark that position scanned and continue to the next iteration
            if (next == null) {
                q.markScanned(nextPos)
                continue
            }
            // Find the clearance of the block
            val nextCeiling = getCeiling(next.pos, Pair(scanHeight, 0), player)
            if (nextCeiling != null)
                next.clearance = nextCeiling.pos.y - next.pos.y - 1
            // If the block is out of range, mark that position scanned and continue to the next iteration
            if (!nextPos.toBlockPos().isWithinDistance(player.pos, scanRadius.toDouble())) {
                q.markScanned(nextPos)
                continue
            }
            // Add it to the scan data
            map.addBlock(next)
            // Mark the block's position scanned
            // (The reason nextPos isn't used is the surface may not be at the same y level that's recorded in nextPos)
            q.markScanned(next.scanPos)
            // If it's a road block, add the adjacent blocks to the queue
            if (roadBlocks.contains(next.name)) q.enqueuePending(next.scanPos.getAdjPositions())
            // If the loop goes on for too long, cancel the scan (already scanned blocks will be saved if the scan is cancelled)
            if (i >= iterLimit) {
                RoadmapClient.logger.warn("Scan reached $iterLimit iterations, stopping early.")
                return
            }
        }
        RoadmapClient.logger.info("Scan ran for ${i + 1} iterations.")
    }

    fun getCeiling(pos: BlockPos, scanRange: Pair<Int, Int>, player: ClientPlayerEntity): RMBlock? {
        for (y in pos.y - scanRange.second..pos.y + scanRange.first) {
            val testPos = BlockPos(pos.x, y, pos.z)
            val block = player.world.getBlockState(testPos)
            val blockBelow = player.world.getBlockState(testPos.subtract(BlockPos(0, 1, 0)))
            if (isSolid(block) and !isSolid(blockBelow))
                return RMBlock(testPos, getName(block), 0)
        }
        return null
    }

    fun getFloor(pos: BlockPos, scanRange: Pair<Int, Int>, player: ClientPlayerEntity): RMBlock? {
        for (y in pos.y + scanRange.first downTo pos.y - scanRange.second) {
            val testPos = BlockPos(pos.x, y, pos.z)
            val block = player.world.getBlockState(testPos)
            val blockAbove = player.world.getBlockState(testPos.add(BlockPos(0, 1, 0)))
            if (isSolid(block) and !isSolid(blockAbove))
                return RMBlock(testPos, getName(block), 0)
        }
        return null
    }

    fun isSolid(block: BlockState): Boolean {
        // TODO: Make lists for overridden opaque and overridden transparent blocks
        return if (getName(block) == "snow_layer")
            false
        else
            block.isOpaque
    }

    fun getName(block: BlockState): String {
        return Registries.BLOCK.getId(block.block).toString()
    }

    fun getName(block: Block): String {
        return Registries.BLOCK.getId(block).toString()
    }
}

class ScanQueue {
    private val scanned = mutableListOf<ScanPos>()
    private val pending: Queue<ScanPos> = LinkedList()

    fun getScannedCount(): Int {
        return scanned.count()
    }

    fun getPendingCount(): Int {
        return pending.count()
    }

    fun hasPending(): Boolean {
        return pending.isNotEmpty()
    }

    fun markScanned(pos: BlockPos): Boolean {
        return markScanned(ScanPos.fromBlockPos(pos))
    }

    fun markScanned(pos: ScanPos): Boolean {
        if (isScanned(pos)) return false
        scanned.add(pos)
        return true
    }

    fun dequeuePending(): ScanPos {
        return pending.remove()
    }

    fun enqueuePending(positions: Set<ScanPos>) {
        for (pos in positions)
            if (!isScanned(pos) and !isPending(pos))
                pending.add(pos)
    }

    fun isPending(pos: BlockPos): Boolean {
        return isPending(ScanPos.fromBlockPos(pos))
    }

    fun isPending(pos: ScanPos): Boolean {
        return pending.contains(pos)
    }

    fun isScanned(pos: BlockPos): Boolean {
        return isScanned(ScanPos.fromBlockPos(pos))
    }

    fun isScanned(pos: ScanPos): Boolean {
        return scanned.contains(pos)
    }
}

data class ScanPos(var x: Int, var y: Int, var z: Int) {
    fun getAdjPositions(): Set<ScanPos> {
        return setOf(
            ScanPos(x - 1, y, z - 1),
            ScanPos(x, y, z - 1),
            ScanPos(x + 1, y, z - 1),
            ScanPos(x + 1, y, z),
            ScanPos(x + 1, y, z + 1),
            ScanPos(x, y, z + 1),
            ScanPos(x - 1, y, z + 1),
            ScanPos(x - 1, y, z)
        )
    }

    fun toBlockPos(): BlockPos {
        return BlockPos(x, y, z)
    }

    companion object {
        fun fromBlockPos(pos: BlockPos): ScanPos {
            return ScanPos(pos.x, pos.y, pos.z)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is ScanPos) return false
        val xzEqual = (x == other.x) and (z == other.z)
        val yWithinRange = (y >= other.y - 1) and (y <= other.y + 1)
        return xzEqual and yWithinRange
    }

    override fun hashCode(): Int {
        var result = x
        result = 31 * result + y
        result = 31 * result + z
        return result
    }
}

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

    fun writeFiles(path: String) {
        for (removalPos in chunkRemovals) RMFS.removeFile(removalPos.toFilename())
        for (updatePos in chunkUpdates.keys) if (chunkUpdates[updatePos] ?: false) {
            val chunk = chunks[updatePos]
            RMFS.writeFile(path + (chunk?.pos?.toFilename() ?: "scan_null"), chunk.toString())
        }
    }

    companion object {
        fun readFiles(path: String): RMMap {
            val result = RMMap()
            for (file in RMFS.listFiles(path))
                if (Regex("scan_(-?\\d+)_(-?\\d+)").matches(file.nameWithoutExtension))
                    result.addChunk(RMChunk.fromString(RMFS.readFile(file)))
            return result
        }
    }
}

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
            result += "\n" + block.toString()
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

data class RMBlock(var pos: BlockPos, var name: String, var clearance: Int) {
    var scanPos = ScanPos.fromBlockPos(pos)

    override fun toString(): String {
        return if (clearance == 0) {
            "${pos.x} ${pos.y} ${pos.z} $name"
        } else {
            "${pos.x} ${pos.y} ${pos.z} $name $clearance"
        }
    }

    companion object {
        fun fromString(value: String): RMBlock {
            val parts = value.split(' ')
            val pos = BlockPos(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
            val block = parts[3]
            var clearance = 0
            if (parts.count() == 5)
                clearance = parts[4].toInt()
            return RMBlock(pos, block, clearance)
        }
    }
}

data class ChunkPos(var x: Int, var z: Int) {
    fun toBlockPos(): BlockPos {
        return BlockPos(x * 16, 64, z * 16)
    }

    fun toFilename(): String {
        return "scan_${x}_${z}.txt"
    }

    companion object {
        fun fromBlockPos(pos: BlockPos): ChunkPos {
            return ChunkPos(pos.x / 16, pos.z / 16)
        }

        fun fromFilename(filename: String): ChunkPos {
            val parts = filename.substring(0, filename.length - 4).split('_')
            return ChunkPos(parts[1].toInt(), parts[2].toInt())
        }
    }
}

class RMConfig {
    val gson = Gson()
    var scanRadius = 64
    var scanHeight = 16
    var roadBlocks = mutableListOf("minecraft:gravel", "minecraft:dirt_path")

    fun saveFile(path: String) {
        val json = JsonObject()
        json.add("scanRadius", JsonPrimitive(scanRadius))
        json.add("scanHeight", JsonPrimitive(scanHeight))
        json.add("roadBlocks", JsonPrimitive(roadBlocks.joinToString(", ")))
        RMFS.writeFile(path, json.toString())
    }

    fun loadFile(path: String) {
        val content = RMFS.readFile(path)
        val json = gson.fromJson(content, JsonObject::class.java) ?: return
        scanRadius = json.getAsJsonPrimitive("scanRadius").asInt
        scanHeight = json.getAsJsonPrimitive("scanHeight").asInt
        roadBlocks = json.getAsJsonPrimitive("roadBlocks").asString.split(", ").toMutableList()
    }
}

object RMFS {
    const val basePath = "roadmap/"

    fun listFiles(path: String): List<File> {
        val dir = File(basePath + path)
        return dir.listFiles()?.toList() ?: listOf()
    }

    fun readFile(path: String): String {
        val file = File(basePath + path)
        return readFile(file)
    }

    fun readFile(file: File): String {
        return file.readText()
    }

    fun writeFile(path: String, contents: String) {
        val file = File(basePath + path)
        createFile(file)
        file.writeText(contents)
    }

    fun createFile(path: String): Boolean {
        val file = File(basePath + path)
        return createFile(file)
    }

    fun createFile(file: File): Boolean {
        file.parentFile.mkdirs()
        return file.createNewFile()
    }

    fun removeFile(path: String): Boolean {
        val file = File(basePath + path)
        return removeFile(file)
    }

    fun removeFile(file: File): Boolean {
        if (!containsFile(file)) return false
        file.delete()
        return true
    }

    fun containsFiles(path: String): Boolean {
        return listFiles(path).isNotEmpty()
    }

    fun containsFile(path: String): Boolean {
        val file = File(basePath + path)
        return containsFile(file)
    }

    fun containsFile(file: File): Boolean {
        return file.exists()
    }
}
