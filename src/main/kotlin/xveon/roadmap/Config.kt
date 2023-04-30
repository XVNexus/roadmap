package xveon.roadmap

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive

object Config {
    // The maximum radius around the player the scanner is allowed to search for
    var scanRadius = 64
    // How much to scan up or down when finding the surface at the player position and finding block clearance
    // (distance to ceiling)
    var scanHeight = 16
    // The maximum block height difference for the scanner to crawl up/down hills
    // (a block height difference higher than this will just cause the scanner to skip scanning past that point)
    var scanSlope = 2
    // The radius to search around found road blocks for other road blocks
    var adjacentRadius = 2
    // The list of block ids which are considered as "road"
    var roadBlocks = mutableListOf("minecraft:gravel", "minecraft:dirt_path")

    val gson = Gson()

    fun saveFile(path: String) {
        val json = JsonObject()
        json.add("scanRadius", JsonPrimitive(scanRadius))
        json.add("scanHeight", JsonPrimitive(scanHeight))
        json.add("adjacentRadius", JsonPrimitive(adjacentRadius))
        json.add("roadBlocks", JsonPrimitive(roadBlocks.joinToString(", ")))
        FS.writeFile(path, json.toString())
    }

    fun loadFile(path: String) {
        val content = FS.readFile(path)
        val json = gson.fromJson(content, JsonObject::class.java) ?: return
        scanRadius = json.getAsJsonPrimitive("scanRadius").asInt
        scanHeight = json.getAsJsonPrimitive("scanHeight").asInt
        adjacentRadius = json.getAsJsonPrimitive("adjacentRadius").asInt
        roadBlocks = json.getAsJsonPrimitive("roadBlocks").asString.split(", ").toMutableList()
    }
}
