package xveon.roadmap.util

import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import xveon.roadmap.storage.Config
import xveon.roadmap.storage.Constants

object UtilMain {
    val markersFilenameRegex = Regex("^markers$")
    val chunkFilenameRegex = Regex("^chunk_(-?\\d+)_(-?\\d+)$")
    val roadmapFilenameRegex = Regex("^(markers|chunk_(-?\\d+)_(-?\\d+))$")

    fun BlockPos.getAdjPositions(): Set<BlockPos> {
        return setOf(
            this.add(0, 0, -1), // North
            this.add(1, 0, 0), // East
            this.add(0, 0, 1), // South
            this.add(-1, 0, 0), // West
        )
    }

    fun BlockPos.isInRange(other: Vec3d, range: Double): Boolean {
        return this.isWithinDistance(other, range)
    }

    fun BlockPos.isInRange(other: BlockPos, rangeY: Int = 1): Boolean {
        val xzEqual = this.x == other.x && this.z == other.z
        val yWithinRange = this.y >= other.y - rangeY && this.y <= other.y + rangeY
        return xzEqual && yWithinRange
    }

    fun genMarkersFilename(): String {
        return "markers.${Constants.SCAN_FILE_EXTENSION}"
    }

    fun genChunkFilename(chunkPos: ChunkPos): String {
        return "chunk_${chunkPos.x}_${chunkPos.z}.${Constants.SCAN_FILE_EXTENSION}"
    }

    fun isBlockSolid(block: BlockState): Boolean {
        val name = getRegistryName(block)
        return if ((Config["terrain_blocks"] as MutableList<String>).contains(name))
            true
        else if ((Config["ignored_blocks"] as MutableList<String>).contains(name))
            false
        else
            block.isOpaque
    }

    fun getRegistryName(itemStack: ItemStack): String {
        return getRegistryName(itemStack.item)
    }

    fun getRegistryName(item: Item): String {
        return Registries.ITEM.getId(item).toString()
    }

    fun getRegistryName(blockState: BlockState): String {
        return getRegistryName(blockState.block)
    }

    fun getRegistryName(block: Block): String {
        return Registries.BLOCK.getId(block).toString()
    }

    fun compressRegistryName(name: String): String {
        return if (name.startsWith("minecraft:"))
            name.substring(10)
        else
            name
    }

    fun expandRegistryName(name: String): String {
        return if (!name.contains(':') && name != "_")
            "minecraft:$name"
        else
            name
    }
}
