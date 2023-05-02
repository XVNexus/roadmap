package xveon.roadmap

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive

object Config {
    var drawParticles = true
    var particleChunkRadius = 2
    var scanRadius = 64.0
    var scanHeight = 16
    var scanEverything = false
    var roadBlocks = mutableListOf("minecraft:gravel", "minecraft:dirt_path")
    var terrainBlocks = mutableListOf<String>()
    var ignoredBlocks = mutableListOf("minecraft:snow")

    val gson = Gson()

    fun reloadFile() {
        loadFile()
        saveFile()
    }

    fun saveFile() {
        val json = JsonObject()
        json.add("drawParticles", JsonPrimitive(drawParticles))
        json.add("particleChunkRadius", JsonPrimitive(particleChunkRadius))
        json.add("scanRadius", JsonPrimitive(scanRadius))
        json.add("scanHeight", JsonPrimitive(scanHeight))
        json.add("scanAll", JsonPrimitive(scanEverything))
        json.add("roadBlocks", JsonPrimitive(roadBlocks.joinToString(", ")))
        json.add("solidBlocks", JsonPrimitive(terrainBlocks.joinToString(", ")))
        json.add("transparentBlocks", JsonPrimitive(ignoredBlocks.joinToString(", ")))
        FileSys.writeFile(Constants.CONFIG_PATH, json.toString())
    }

    fun loadFile() {
        if (!FileSys.containsFile(Constants.CONFIG_PATH))
            return
        val content = FileSys.readFile(Constants.CONFIG_PATH)
        val json = gson.fromJson(content, JsonObject::class.java) ?: return
        drawParticles = loadProperty(json, "drawParticles")?.asBoolean ?: drawParticles
        particleChunkRadius = loadProperty(json, "particleChunkRadius")?.asInt ?: particleChunkRadius
        scanRadius = loadProperty(json, "scanRadius")?.asDouble ?: scanRadius
        scanHeight = loadProperty(json, "scanHeight")?.asInt ?: scanHeight
        scanEverything = loadProperty(json, "scanAll")?.asBoolean ?: scanEverything
        roadBlocks = loadProperty(json, "roadBlocks")?.asString?.split(", ")?.toMutableList() ?: roadBlocks
        terrainBlocks = loadProperty(json, "solidBlocks")?.asString?.split(", ")?.toMutableList() ?: terrainBlocks
        ignoredBlocks = loadProperty(json, "transparentBlocks")?.asString?.split(", ")?.toMutableList() ?: ignoredBlocks
    }

    private fun loadProperty(json: JsonObject, property: String): JsonPrimitive? {
        if (!json.has(property)) return null
        return json.getAsJsonPrimitive(property)
    }
}

enum class ConfigType {
    BOOLEAN, INT, DOUBLE, STRING, ITEM_ID, BLOCK_ID
}
