package com.snaptube.clone.core.extractor.youtube

/**
 * Parses and evaluates the YouTube player JavaScript to recover the signature
 * decipher function and, where present, the throttling ("n") parameter
 * transformation.
 *
 * This is a clean-room reimplementation of the well-documented approach used by
 * open-source downloaders (yt-dlp, NewPipe, youtube-dl). It does not contain any
 * code copied from the analysed application.
 */
class SignatureDecipherer private constructor(
    private val decipherFunction: String,
    private val nFunction: String?,
    private val helpers: Map<String, String>
) {

    /** Applies the signature transform extracted from the player JS. */
    fun decipher(signature: String): String =
        applyFunction(decipherFunction, signature)

    /** Applies the "n" throttle transform if one was found. */
    fun transformN(value: String): String {
        val fn = nFunction ?: return value
        return applyFunction(fn, value)
    }

    var hasNTransform: Boolean = nFunction != null
        private set

    // ---------------------------------------------------------------------

    private fun applyFunction(functionName: String, input: String): String {
        val body = helpers[functionName] ?: return input
        var value = input
        for (op in parseOps(body)) {
            value = when (op) {
                is Op.Reverse -> value.reversed()
                is Op.Swap -> swap(value, op.index)
                is Op.Splice -> value.substring(op.index)
                is Op.Helper -> applyFunction(op.name, value)
                is Op.Unknown -> value
            }
        }
        return value
    }

    private fun parseOps(body: String): List<Op> {
        // Body is a comma separated list of statements like:
        //   a.reverse(),  a.splice(0, b),  a = W(a)  -> function calls and array ops
        val ops = mutableListOf<Op>()
        val statementRegex = Regex("""([a-zA-Z0-9_$]+)\.([a-zA-Z]+)\(([^)]*)\)|([a-zA-Z0-9_$]+)\(\1\)""")
        for (m in statementRegex.findAll(body)) {
            val method = m.groupValues[2]
            val args = m.groupValues[3]
            when (method) {
                "reverse" -> ops += Op.Reverse
                "splice" -> {
                    val idx = args.substringBefore(",").trim().toIntOrNull() ?: 0
                    ops += Op.Splice(idx)
                }
                "slice", "substring", "substr" -> {
                    val idx = args.substringBefore(",").trim().toIntOrNull() ?: 0
                    ops += Op.Splice(idx)
                }
                else -> {
                    val helper = helpers.keys.firstOrNull { it == m.groupValues[1] }
                    if (helper != null) ops += Op.Helper(helper) else ops += Op.Unknown
                }
            }
        }
        return ops
    }

    private fun swap(value: String, index: Int): String {
        if (index < 0 || value.isEmpty()) return value
        val i = index % value.length
        val chars = value.toCharArray()
        val tmp = chars[0]
        chars[0] = chars[i]
        chars[i] = tmp
        return String(chars)
    }

    private sealed interface Op {
        data object Reverse : Op
        data class Swap(val index: Int) : Op
        data class Splice(val index: Int) : Op
        data class Helper(val name: String) : Op
        data object Unknown : Op
    }

    companion object {

        /**
         * Builds a decipherer from the raw player JS.
         * Returns null when no signature function can be located, which means
         * the caller should fall back to another client that needs no decipher.
         */
        fun from(playerJs: String): SignatureDecipherer? {
            val helperNames = extractHelperTables(playerJs)
            val splitFn = findFunctionByMarker(playerJs, "split(\"\")") ?: return null
            val reverseOp = helperNames.values.firstOrNull { it.contains("reverse()") }
            val spliceOp = helperNames.values.firstOrNull { it.contains("splice(") }

            val helpers = mutableMapOf<String, String>()
            helperNames.forEach { (name, body) -> helpers[name] = body }
            if (reverseOp != null) {
                val name = extractFunctionName(playerJs, reverseOp) ?: "reverse_helper"
                helpers[name] = reverseOp
            }
            if (spliceOp != null) {
                val name = extractFunctionName(playerJs, spliceOp) ?: "splice_helper"
                helpers[name] = spliceOp
            }

            val nFunction = findNFunction(playerJs)
            return SignatureDecipherer(splitFn.second, nFunction, helpers)
        }

        private fun extractHelperTables(js: String): Map<String, String> {
            val result = mutableMapOf<String, String>()
            // Object literal of the form  var x={name:function(a){...},...}
            val objectRegex = Regex("""\{([a-zA-Z0-9_$]+:function\([a-z]\)\{.*?)\}\}""")
            objectRegex.find(js)?.let { obj ->
                val inner = obj.groupValues[1]
                Regex("""([a-zA-Z0-9_$]+):function\(([a-z])\)\{([^{}]*)\}""").findAll(inner).forEach {
                    result[it.groupValues[1]] = it.groupValues[3]
                }
            }
            return result
        }

        private fun findFunctionByMarker(js: String, marker: String): Pair<String, String>? {
            val idx = js.indexOf(marker)
            if (idx < 0) return null
            // Walk back to the enclosing "name=function(a){"
            val headStart = js.lastIndexOf("function", idx)
            if (headStart < 0) return null
            val headMatch = Regex("""([a-zA-Z0-9_$]+)\s*[:=]\s*function\(""").find(js.substring(0, headStart + 20)) ?: return null
            val bodyEnd = js.indexOf('}', idx)
            if (bodyEnd < 0) return null
            val name = headMatch.groupValues[1]
            return name to js.substring(headStart, bodyEnd + 1)
        }

        private fun findNFunction(js: String): String? {
            val marker = Regex("""\.get\("n"\)\)&&\([a-z]=([a-zA-Z0-9_$]+)\(""")
            val m = marker.find(js) ?: return null
            val fnName = m.groupValues[1]
            val fnIdx = js.indexOf("$fnName=function(")
            if (fnIdx < 0) return null
            val bodyEnd = js.indexOf('}', fnIdx)
            if (bodyEnd < 0) return null
            return js.substring(fnIdx, bodyEnd + 1)
        }

        private fun extractFunctionName(js: String, body: String): String? {
            val idx = js.indexOf(body)
            if (idx < 0) return null
            val head = js.substring(0, idx)
            return Regex("""([a-zA-Z0-9_$]+)\s*[:=]\s*function\(""").findAll(head).lastOrNull()?.groupValues?.get(1)
        }
    }
}
