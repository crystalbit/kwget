import java.io.File

class HTMLWalker(private var code: String, private var base: String = "") {
    private val toDownload: Map<Pair<String, String>, Pair<Options, Types>> = mapOf(
        Pair("link", "href") to Pair(Options.ASK, Types.CSS),
        Pair("iframe", "src") to Pair(Options.ASK, Types.HTML),
        Pair("meta", "property") to Pair(Options.ASK, Types.HTML),
        Pair("meta", "content") to Pair(Options.ASK, Types.STRICT),
        Pair("script", "src") to Pair(Options.EXTERNAL_ASK, Types.JS),
        Pair("style", "src") to Pair(Options.EXTERNAL_ASK, Types.CSS),
        Pair("img", "src") to Pair(Options.EXTERNAL_ASK, Types.IMAGE),
        Pair("a", "href") to Pair(Options.LOCAL_ASK, Types.HTML),
        Pair("form", "action") to Pair(Options.ASK, Types.HTML),
        Pair("*", "style") to Pair(Options.PROCESS, Types.CSS)
    )

    init {
        //println("HTMLWalker")
        processLoop()
    }

    var inTag = false
    var inTagName = false
    var inTagDeclaration = false
    var inTagPropertyName = false
    var inTagPropertyValue = false
    var inScript = false
    var inStyle = false
    var inComment = false
    var tagName = ""
    var tag = ""
    var tagPropertyName = ""
    var tagPropertyValue = ""
    var comment = ""

    private fun processTagName(_tagName: String) {
        inTagName = false
        when (_tagName) {
            "script" -> inScript = true
            "/script" -> inScript = false
            "style" -> inStyle = true
            "/style" -> inStyle = false
        }
        // TODO: ignore when inStyle or inScript
        //println("$_tagName ->")
    }

    private fun processTag(_tag: String) {
        //println("TAG $_tag")
        inTag = false
    }

    private fun askAndDownload(address: String): String? {
        var decision = LoaderUtil.getDecision(address)
        //println(decision)
        if (decision == Decision.NOT_DECIDED) {
            print("Download $address? (y/n)")
            val ynDecision = readLine()!!
            decision = if (ynDecision.toLowerCase() == "y") {
                LoaderUtil.addAgreed(address)
                Decision.AGREED
            } else {
                LoaderUtil.addDeclined(address)
                Decision.DECLINED
            }
        }

        if (decision == Decision.AGREED) {
            return LoaderUtil.addReplacerAuto(address, base)
        }
        return null
    }

    private fun getAction(tag: String, property: String): Pair<Options, Types>? {
        val exact = toDownload[tag to property]
        return if (exact == null) toDownload["*" to property] else exact
    }

    private fun processByType(type: Types, file: String, source: String) {
        when (type) {
            Types.CSS -> CSSWalker(File(file).readText(), LoaderUtil.getUrlBase(source))
        }
    }

    private fun offerDownload(_tagName: String, _tagPropertyName: String, _tagPropertyValue: String) {
        //println("OFFER $_tagName $_tagPropertyName $_tagPropertyValue")
        var value = _tagPropertyValue
        val action = getAction(_tagName, _tagPropertyName)
        val isAddress = action?.first != Options.PROCESS

        val strict = action?.second == Types.STRICT
        if (isAddress && !LoaderUtil.shallDownload(value, strict)) return

        if (isAddress && !value.contains("://")) {
            value = base + (if (base.lastOrNull() == '/' || value.firstOrNull() == '/') "" else "/") + value
        }

        value = value.split("#")[0]
        if (value.last() == '?') value = value.dropLast(1)
        if (value.last() == '/') value = value.dropLast(1)

        when (action?.first) {
            Options.PROCESS -> {
                when (action.second) {
                    Types.CSS -> CSSWalker(value, base)
                }
            }
            Options.ALWAYS -> {
                LoaderUtil.addReplacerAuto(value)
            }
            Options.ASK -> {
                askAndDownload(value)
            }
            Options.EXTERNAL_ASK -> {
                if (LoaderUtil.isExternal(_tagPropertyValue)) {
                    askAndDownload(value)
                } else {
                    LoaderUtil.addReplacerAuto(value)
                }
            }
            Options.LOCAL -> {
                if (!LoaderUtil.isExternal(_tagPropertyValue)) {
                    LoaderUtil.addReplacerAuto(value)
                }
            }
            Options.LOCAL_ASK -> {
                if (!LoaderUtil.isExternal(_tagPropertyValue)) {
                    askAndDownload(value)
                }
            }
            // else null and we don't hold it
        }
        //println("$_tagName -> $_tagPropertyName = $_tagPropertyValue")
    }

    private fun processTagProperty(_tagProperty: String) {
        var trimmedProperty = _tagProperty.trim()
        //println("PROCESS |$trimmedProperty|")
        when (trimmedProperty[0]) {
            '"' -> {
                if (trimmedProperty.takeLast(1) == "\"") trimmedProperty = trimmedProperty.drop(1).dropLast(1)
                else return
            }
            '\'' -> {
                if (trimmedProperty.takeLast(1) == "'") trimmedProperty = trimmedProperty.drop(1).dropLast(1)
                else return
            }
        }
        if (trimmedProperty != "") offerDownload(tagName.dropLast(1), tagPropertyName.dropLast(1), trimmedProperty)
        inTagPropertyValue = false
    }

    private fun processLoop() {
        symbolLoop@ for (i in 0 until code.length) {
            when {
                inComment -> {
                    comment += code[i]
                    if ("${code[i - 2]}${code[i - 1]}${code[i]}" == "-->") {
                        inComment = false
                        //println("COMMENT ${comment.dropLast(3)}")
                        // next unuseful
                        inTag = false
                        inTagName = false
                        tagName = ""
                        tag = ""
                    }
                    else continue@symbolLoop
                }
                inTagName || inTag || inTagPropertyValue || inTagPropertyName -> {
                    if (inTagName) tagName += code[i]
                    if (inTag) tag += code[i]
                    if (inTagPropertyValue) tagPropertyValue += code[i]
                    if (inTagPropertyName) tagPropertyName += code[i]
                }
            }
            if (tagName == "!--") {
                inComment = true
                comment = ""
                tagName = "was-a-comment" + " "
                continue@symbolLoop
            }
            when (code[i]) {
                '<' -> {
                    if (!inTag) {
                        inTag = true
                        tag = ""
                    }
                    if (!inTagName) {
                        inTagName = true
                        tagName = ""
                    }
                }
                ' ', '/' -> {
                    if (tagName == "/") {
                        continue@symbolLoop
                    }
                    if (inTag && inTagName && tagName.trim() != "") processTagName(tagName.dropLast(1))
                    if (inTagPropertyValue) processTagProperty(tagPropertyValue.dropLast(1))
                    if (!inTagName && !inTagPropertyValue) {
                        inTagPropertyName = true
                        tagPropertyName = ""
                    }
                }
                '>' -> {
                    if (inTagName) processTagName(tagName.dropLast(1))
                    if (inTagPropertyValue) processTagProperty(tagPropertyValue.dropLast(1))
                    inTagPropertyName = false
                    processTag(tag.dropLast(1))
                }
                '=' -> {
                    if (inTag && !inTagPropertyValue) {
                        inTagPropertyName = false
                        inTagPropertyValue = true
                        tagPropertyValue = ""
                    }
                }
            }
        }
    }
}