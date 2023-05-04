package xveon.roadmap

import net.minecraft.util.math.BlockPos

data class ChunkPos(var x: Int, var z: Int) {
    fun toBlockPos(): BlockPos {
        return BlockPos(x * Constants.CHUNK_SIZE, 0, z * Constants.CHUNK_SIZE)
    }

    companion object {
        fun fromBlockPos(pos: BlockPos): ChunkPos {
            return ChunkPos(pos.x shr Constants.CHUNK_BIT_SHIFT, pos.z shr Constants.CHUNK_BIT_SHIFT)
        }
    }
}
