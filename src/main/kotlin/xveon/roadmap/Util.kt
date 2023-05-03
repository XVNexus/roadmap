package xveon.roadmap

import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.registry.Registries

object Util {
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
        val result = if (name.startsWith("minecraft:")) name.substring(10)
            else name
        return result.replace('_', ' ')
    }

    fun expandBlockName(name: String): String {
        val result =
            if (!name.contains(':') and (name != Constants.UNKNOWN_NAME) and (name != Constants.VOID_NAME)) "minecraft:$name"
            else name
        return result.replace(' ', '_')
    }
}
