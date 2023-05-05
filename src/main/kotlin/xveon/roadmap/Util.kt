package xveon.roadmap

import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.util.math.BlockPos

object Util {
    val markersFilenameRegex = Regex("^markers$")
    val chunkFilenameRegex = Regex("^chunk_(-?\\d+)_(-?\\d+)$")
    val roadmapFilenameRegex = Regex("^(markers|chunk_(-?\\d+)_(-?\\d+))$")

    fun isPosNearOtherPos(pos: BlockPos, other: BlockPos, rangeY: Int = 1): Boolean {
        val xzEqual = pos.x == other.x && pos.z == other.z
        val yWithinRange = pos.y >= other.y - rangeY && pos.y <= other.y + rangeY
        return xzEqual && yWithinRange
    }

    fun genMarkersFilename(): String {
        return "markers.${Constants.SCAN_FILE_EXTENSION}"
    }

    fun genChunkFilename(chunkPos: ChunkPos): String {
        return "chunk_${chunkPos.x}_${chunkPos.z}.${Constants.SCAN_FILE_EXTENSION}"
    }

    fun isBlockSolid(block: BlockState): Boolean {
        val name = Util.getRegistryName(block)
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
        return if (!name.contains(':') && (name != "_"))
            "minecraft:$name"
        else
            name
    }
}
