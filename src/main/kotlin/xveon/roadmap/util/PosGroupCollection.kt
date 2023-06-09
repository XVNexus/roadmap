package xveon.roadmap.util

import net.minecraft.util.math.BlockPos

class PosGroupCollection(var mergeRange: Int) {
    private val groups = mutableSetOf<PosGroup>()

    fun getCenterPositions(): MutableSet<BlockPos> {
        val result = mutableSetOf<BlockPos>()
        for (group in groups)
            result.add(group.center)
        return result
    }

    fun getGroups(): MutableSet<PosGroup> {
        return groups
    }

    fun getPosCount(): Int {
        var result = 0
        for (group in groups)
            result += group.getPosCount()
        return result
    }

    fun getGroupCount(): Int {
        return groups.count()
    }

    fun addPos(pos: BlockPos): Boolean {
        for (group in groups)
            if (group.isPosNear(pos, mergeRange))
                return group.addPos(pos)

        val newGroup = PosGroup()
        newGroup.addPos(pos)
        groups.add(newGroup)
        return true
    }

    fun removePos(pos: BlockPos): Boolean {
        for (group in groups)
            if (group.isPosNear(pos, mergeRange))
                return group.removePos(pos)
        return false
    }

    fun clearPositions(): Boolean {
        if (groups.isEmpty()) return false
        groups.clear()
        return true
    }

    fun removeEmptyGroups() {
        val groupsToRemove = mutableSetOf<PosGroup>()
        for (group in groups)
            if (group.getPosCount() == 0)
                groupsToRemove.add(group)

        for (group in groupsToRemove)
            groups.remove(group)
    }
}
