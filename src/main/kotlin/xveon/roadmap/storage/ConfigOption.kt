package xveon.roadmap.storage

import xveon.roadmap.util.UtilMain

class ConfigOption(val type: ConfigType, val isList: Boolean, val defaultValues: MutableList<Any>? = null, val defaultValue: Any? = null) {
    var values = mutableListOf<Any>()
    var value: Any
        get() = if (!isList && values.isNotEmpty()) values[0]
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
                result += ", ${stringifyValue(value, type)}"
            if (result.isNotEmpty()) result.substring(2) else ""
        } else {
            stringifyValue(value, type)
        }
    }

    fun fromString(raw: String) {
        if (isList) {
            values.clear()
            for (item in raw.split(Regex(", *"))) if (item.isNotEmpty())
                values.add(parseValue(item, type))
        } else {
            value = parseValue(raw, type)
        }
    }

    companion object {
        fun stringifyValue(value: Any?, type: ConfigType): String {
            return when (type) {
                ConfigType.BOOLEAN -> (if (value as Boolean) "true" else "false")
                ConfigType.INT -> value.toString()
                ConfigType.DOUBLE -> value.toString()
                ConfigType.STRING -> value as String
                ConfigType.ENUM -> value.toString().lowercase().replace('_', ' ')
                ConfigType.ID -> UtilMain.compressRegistryName(value as String)
            }
        }

        fun parseValue(raw: String, type: ConfigType): Any {
            val trimmed = raw.trim()
            return when (type) {
                ConfigType.BOOLEAN -> trimmed.startsWith('t') || trimmed.startsWith('y')
                ConfigType.INT -> trimmed.toInt()
                ConfigType.DOUBLE -> trimmed.toDouble()
                ConfigType.STRING -> trimmed
                ConfigType.ENUM -> trimmed.uppercase().replace(' ', '_')
                ConfigType.ID -> UtilMain.expandRegistryName(trimmed)
            }
        }
    }
}
