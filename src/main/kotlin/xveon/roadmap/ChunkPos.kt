package xveon.roadmap

import net.minecraft.util.math.BlockPos

data class ChunkPos(var x: Int, var z: Int) {
    fun toBlockPos(): BlockPos {
        return BlockPos(x * Constants.CHUNK_SIZE, 0, z * Constants.CHUNK_SIZE)
    }

    fun toFilename(): String {
        return "scan_${x}_${z}.${Constants.SCAN_FILE_EXTENSION}"
    }

    companion object {
        fun fromBlockPos(pos: BlockPos): ChunkPos {
            return ChunkPos(pos.x shr Constants.CHUNK_BIT_SHIFT, pos.z shr Constants.CHUNK_BIT_SHIFT)
        }

        fun fromFilename(filename: String): ChunkPos {
            val parts = filename.substring(0, filename.length - Constants.SCAN_FILE_EXTENSION.length - 1).split('_')
            return ChunkPos(parts[1].toInt(), parts[2].toInt())
        }
    }
}
