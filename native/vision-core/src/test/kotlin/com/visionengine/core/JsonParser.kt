// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
package com.visionengine.core

fun parseJson(text: String): Any? {
    val parser = JsonParser(text)
    val value = parser.parseValue()
    parser.skipWs()
    require(parser.atEnd()) { "trailing content in JSON" }
    return value
}

private class JsonParser(val text: String) {
    var pos: Int = 0

    fun atEnd(): Boolean {
        skipWs()
        return pos >= text.length
    }

    fun skipWs() {
        while (pos < text.length && text[pos].isWhitespace()) {
            pos += 1
        }
    }

    fun parseValue(): Any? {
        skipWs()
        require(pos < text.length) { "unexpected end of JSON" }
        return when (val head = text[pos]) {
            '{' -> parseObject()
            '[' -> parseArray()
            '"' -> parseString()
            't' -> expectLiteral("true", true)
            'f' -> expectLiteral("false", false)
            'n' -> expectLiteral("null", null)
            else -> {
                require(head == '-' || head.isDigit()) {
                    "unexpected character: $head"
                }
                parseNumber()
            }
        }
    }

    fun parseObject(): Map<String, Any?> {
        pos += 1
        val out = LinkedHashMap<String, Any?>()
        skipWs()
        if (pos < text.length && text[pos] == '}') {
            pos += 1
            return out
        }
        while (true) {
            skipWs()
            require(pos < text.length && text[pos] == '"') {
                "expected object key"
            }
            val key = parseString()
            skipWs()
            require(pos < text.length && text[pos] == ':') {
                "expected colon"
            }
            pos += 1
            out[key] = parseValue()
            skipWs()
            require(pos < text.length) { "unterminated object" }
            if (text[pos] == '}') {
                pos += 1
                return out
            }
            require(text[pos] == ',') { "expected comma" }
            pos += 1
        }
    }

    fun parseArray(): List<Any?> {
        pos += 1
        val out = mutableListOf<Any?>()
        skipWs()
        if (pos < text.length && text[pos] == ']') {
            pos += 1
            return out
        }
        while (true) {
            out.add(parseValue())
            skipWs()
            require(pos < text.length) { "unterminated array" }
            if (text[pos] == ']') {
                pos += 1
                return out
            }
            require(text[pos] == ',') { "expected comma" }
            pos += 1
        }
    }

    fun parseString(): String {
        pos += 1
        val out = StringBuilder()
        while (true) {
            require(pos < text.length) { "unterminated string" }
            val head = text[pos]
            if (head == '"') {
                pos += 1
                return out.toString()
            }
            if (head != '\\') {
                out.append(head)
                pos += 1
                continue
            }
            pos += 1
            require(pos < text.length) { "bad escape" }
            when (val esc = text[pos]) {
                '"', '\\', '/' -> out.append(esc)
                'b' -> out.append('\b')
                'f' -> out.append('\u000C')
                'n' -> out.append('\n')
                'r' -> out.append('\r')
                't' -> out.append('\t')
                'u' -> {
                    require(pos + 4 < text.length) { "bad unicode escape" }
                    val code = text.substring(pos + 1, pos + 5).toInt(16)
                    out.append(code.toChar())
                    pos += 4
                }
                else -> throw IllegalArgumentException("bad escape: $esc")
            }
            pos += 1
        }
    }

    fun parseNumber(): Number {
        val start = pos
        while (pos < text.length && "-+0123456789.eE".contains(text[pos])) {
            pos += 1
        }
        val raw = text.substring(start, pos)
        require(raw.isNotEmpty()) { "bad number" }
        if (raw.none { it == '.' || it == 'e' || it == 'E' }) {
            return raw.toLong()
        }
        return raw.toDouble()
    }

    fun expectLiteral(word: String, value: Any?): Any? {
        require(text.startsWith(word, pos)) { "bad literal" }
        pos += word.length
        return value
    }
}
