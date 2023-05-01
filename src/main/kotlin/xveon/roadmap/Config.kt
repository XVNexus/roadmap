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
    // The list of block ids which are considered as "road"
    var roadBlocks = mutableListOf("minecraft:gravel", "minecraft:dirt_path")

    // Which blocks are considered solid
    var solidBlocks = mutableListOf<String>()
    // Which blocks are considered not solid
    var transparentBlocks = mutableListOf("minecraft:snow")

    val gson = Gson()

    fun saveFile() {
        val json = JsonObject()
        json.add("scanRadius", JsonPrimitive(scanRadius))
        json.add("scanHeight", JsonPrimitive(scanHeight))
        json.add("roadBlocks", JsonPrimitive(roadBlocks.joinToString(",")))
        json.add("solidBlocks", JsonPrimitive(solidBlocks.joinToString(",")))
        json.add("transparentBlocks", JsonPrimitive(transparentBlocks.joinToString(",")))
        FS.writeFile(Constants.CONFIG_FILE_PATH, json.toString())
    }

    fun loadFile() {
        if (!FS.containsFile(Constants.CONFIG_FILE_PATH))
            return
        val content = FS.readFile(Constants.CONFIG_FILE_PATH)
        val json = gson.fromJson(content, JsonObject::class.java) ?: return
        scanRadius = loadProperty(json, "scanRadius")?.asInt ?: scanRadius
        scanHeight = loadProperty(json, "scanHeight")?.asInt ?: scanHeight
        roadBlocks = loadProperty(json, "roadBlocks")?.asString?.split(",")?.toMutableList() ?: roadBlocks
        solidBlocks = loadProperty(json, "solidBlocks")?.asString?.split(",")?.toMutableList() ?: solidBlocks
        transparentBlocks = loadProperty(json, "transparentBlocks")?.asString?.split(",")?.toMutableList() ?: transparentBlocks
    }

    private fun loadProperty(json: JsonObject, property: String): JsonPrimitive? {
        if (!json.has(property)) return null
        return json.getAsJsonPrimitive(property)
    }
}
