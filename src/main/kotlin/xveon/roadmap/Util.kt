package xveon.roadmap

import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.registry.Registries

object Util {
    val markersFilenameRegex = Regex("^markers$")
    val chunkFilenameRegex = Regex("^chunk_(-?\\d+)_(-?\\d+)$")

    fun genMarkersFilename(): String {
        return "markers.${Constants.SCAN_FILE_EXTENSION}"
    }

    fun genChunkFilename(chunkPos: ChunkPos): String {
        return "chunk_${chunkPos.x}_${chunkPos.z}.${Constants.SCAN_FILE_EXTENSION}"
    }

    fun isBlockSolid(block: BlockState): Boolean {
        val name = Util.getBlockName(block)
        return if ((Config["terrain_blocks"] as MutableList<String>).contains(name))
            true
        else if ((Config["ignored_blocks"] as MutableList<String>).contains(name))
            false
        else
            block.isOpaque
    }

    fun getBlockName(block: BlockState): String {
        return getBlockName(block.block)
    }

    fun getBlockName(block: Block): String {
        return Registries.BLOCK.getId(block).toString()
    }

    fun compressBlockName(name: String): String {
        return if (name.startsWith("minecraft:"))
            name.substring(10)
        else
            name
    }

    fun expandBlockName(name: String): String {
        return if (!name.contains(':') and (name != "_"))
            "minecraft:$name"
        else
            name
    }
}
