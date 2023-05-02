package xveon.roadmap

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive

object Config {
    var scanRadius = 64.0
    var scanHeight = 16
    var scanAll = false
    var roadBlocks = mutableListOf("minecraft:gravel", "minecraft:dirt_path")
    var solidBlocks = mutableListOf<String>()
    var transparentBlocks = mutableListOf("minecraft:snow")

    val gson = Gson()

    fun saveFile() {
        val json = JsonObject()
        json.add("scanRadius", JsonPrimitive(scanRadius))
        json.add("scanHeight", JsonPrimitive(scanHeight))
        json.add("scanAll", JsonPrimitive(scanAll))
        json.add("roadBlocks", JsonPrimitive(roadBlocks.joinToString(",")))
        json.add("solidBlocks", JsonPrimitive(solidBlocks.joinToString(",")))
        json.add("transparentBlocks", JsonPrimitive(transparentBlocks.joinToString(",")))
        FileSys.writeFile(Constants.CONFIG_FILE_PATH, json.toString())
    }

    fun loadFile() {
        if (!FileSys.containsFile(Constants.CONFIG_FILE_PATH))
            return
        val content = FileSys.readFile(Constants.CONFIG_FILE_PATH)
        val json = gson.fromJson(content, JsonObject::class.java) ?: return
        scanRadius = loadProperty(json, "scanRadius")?.asDouble ?: scanRadius
        scanHeight = loadProperty(json, "scanHeight")?.asInt ?: scanHeight
        scanAll = loadProperty(json, "scanAll")?.asBoolean ?: scanAll
        roadBlocks = loadProperty(json, "roadBlocks")?.asString?.split(",")?.toMutableList() ?: roadBlocks
        solidBlocks = loadProperty(json, "solidBlocks")?.asString?.split(",")?.toMutableList() ?: solidBlocks
        transparentBlocks = loadProperty(json, "transparentBlocks")?.asString?.split(",")?.toMutableList() ?: transparentBlocks
    }

    private fun loadProperty(json: JsonObject, property: String): JsonPrimitive? {
        if (!json.has(property)) return null
        return json.getAsJsonPrimitive(property)
    }
}
