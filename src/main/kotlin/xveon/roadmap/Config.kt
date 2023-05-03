package xveon.roadmap

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import java.lang.Exception

object Config {
    var options = mutableMapOf(
        Pair("draw_particles", ConfigOption(ConfigType.BOOLEAN, false, defaultValue = false)),
        Pair("particle_chunk_radius", ConfigOption(ConfigType.INT, false, defaultValue = 2)),
        Pair("scan_radius", ConfigOption(ConfigType.DOUBLE, false, defaultValue = 64.0)),
        Pair("scan_height", ConfigOption(ConfigType.INT, false, defaultValue = 16)),
        Pair("scan_everything", ConfigOption(ConfigType.BOOLEAN, false, defaultValue = false)),
        Pair("road_blocks", ConfigOption(ConfigType.BLOCK_ID, true, defaultValues = mutableListOf("minecraft:dirt_path", "minecraft:gravel"))),
        Pair("terrain_blocks", ConfigOption(ConfigType.BLOCK_ID, true, defaultValues = mutableListOf())),
        Pair("ignored_blocks", ConfigOption(ConfigType.BLOCK_ID, true, defaultValues = mutableListOf("minecraft:snow")))
    )
    val gson = Gson()

    operator fun get(id: String): Any {
        val option = getOption(id) ?: throw IllegalArgumentException("Option $id does not exist")
        return if (option.isList)
            option.values
        else
            option.value
    }

    operator fun set(id: String, value: Any) {
        val option = getOption(id) ?: throw IllegalArgumentException("Option $id does not exist")
        if (option.isList) {
            option.values.clear()
            for (item in (value as MutableList<*>))
                option.values.add(item ?: continue)
        } else {
            option.value = value
        }
    }

    fun getOptionString(id: String): String {
        return getOption(id)?.toString() ?: throw IllegalArgumentException("Option $id does not exist")
    }

    fun setOptionString(id: String, raw: String): Boolean {
        return try {
            val option = getOption(id) ?: throw IllegalArgumentException("Option $id does not exist")
            option.fromString(raw)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun getValues(id: String): MutableList<Any> {
        return getOption(id)?.values ?: mutableListOf()
    }

    fun getValue(id: String): Any? {
        return getOption(id)?.value
    }

    fun setValues(id: String, values: MutableList<Any>) {
        getOption(id)?.values = values
    }

    fun setValue(id: String, value: Any) {
        getOption(id)?.value = value
    }

    fun getOption(id: String): ConfigOption? {
        return options[id]
    }

    fun containsOption(id: String): Boolean {
        return options.containsKey(id)
    }

    fun reloadFile() {
        loadFile()
        saveFile()
    }

    fun saveFile() {
        val json = JsonObject()
        for (key in options.keys)
            json.add(key, JsonPrimitive(options[key].toString()))
        FileSys.writeFile(Constants.CONFIG_PATH, json.toString())
    }

    fun loadFile() {
        if (!FileSys.containsFile(Constants.CONFIG_PATH))
            return
        val content = FileSys.readFile(Constants.CONFIG_PATH)
        val json = gson.fromJson(content, JsonObject::class.java) ?: return
        for (key in options.keys) {
            val property = loadProperty(json, key)?.asString ?: continue
            getOption(key)?.fromString(property)
        }
    }

    private fun loadProperty(json: JsonObject, property: String): JsonPrimitive? {
        if (!json.has(property)) return null
        return json.getAsJsonPrimitive(property)
    }
}

class ConfigOption(val type: ConfigType, val isList: Boolean, val defaultValues: MutableList<Any>? = null, val defaultValue: Any? = null) {
    var values = mutableListOf<Any>()
    var value: Any
        get() = if (!isList and values.isNotEmpty()) values[0]
        else throw IllegalArgumentException("Cannot get a single value from an array config field")
        set(value) = if (!isList) values = mutableListOf(value)
        else values.clear()

    init {
        if (isList) {
            if (defaultValues != null)
                for (value in defaultValues)
                    values.add(value)
        } else {
            if (defaultValue != null)
                value = defaultValue
        }
    }

    override fun toString(): String {
        return if (isList) {
            var result = ""
            for (value in values)
                result += ",${stringifyValue(value, type)}"
            if (result.isNotEmpty()) result.substring(1) else ""
        } else {
            stringifyValue(value, type)
        }
    }

    fun fromString(raw: String) {
        if (isList) {
            values.clear()
            for (item in raw.split(',', ' ')) if (item.isNotEmpty())
                values.add(parseValue(item, type) ?: throw IllegalArgumentException("Failed to parse raw list item $item as type $type"))
        } else {
            value = parseValue(raw, type) ?: throw IllegalArgumentException("Failed to parse raw value $raw as type $type")
        }
    }

    companion object {
        fun stringifyValue(value: Any?, type: ConfigType): String {
            return when (type) {
                ConfigType.BOOLEAN -> (if (value as Boolean) "true" else "false")
                ConfigType.INT -> value.toString()
                ConfigType.DOUBLE -> value.toString()
                ConfigType.STRING -> value as String
                ConfigType.BLOCK_ID -> Util.compressBlockName(value as String)
            }
        }

        fun parseValue(raw: String, type: ConfigType): Any? {
            return when (type) {
                ConfigType.BOOLEAN -> raw == "true"
                ConfigType.INT -> raw.toInt()
                ConfigType.DOUBLE -> raw.toDouble()
                ConfigType.STRING -> raw
                ConfigType.BLOCK_ID -> Util.expandBlockName(raw)
            }
        }
    }
}

enum class ConfigType {
    BOOLEAN, INT, DOUBLE, STRING, BLOCK_ID
}
