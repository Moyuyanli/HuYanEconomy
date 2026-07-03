package cn.chahuyun.economy.common.text

/**
 * Small text replacement helper for config-driven messages.
 */
object TextTemplate {
    @JvmStatic
    fun replace(template: String, variables: Map<String, Any?>): String {
        return variables.entries.fold(template) { text, (key, value) ->
            text.replace(key, value?.toString().orEmpty())
        }
    }

    @JvmStatic
    fun replace(template: String, variable: Array<String>, vararg content: Any?): String {
        require(variable.size == content.size) {
            "Variable count(${variable.size}) must match content count(${content.size})"
        }
        return variable.indices.fold(template) { text, index ->
            text.replace(variable[index], content[index]?.toString().orEmpty())
        }
    }
}
