class CSSWalker(private var code: String, private val base : String = "/") {
    init {
        //println("CSSWalker")
        process()
    }

    // TODO css imports and so on
    private val toDownload: Map<String, Pair<Options, Types>> = mapOf(
        "background" to Pair(Options.ASK, Types.IMAGE)
    )

    fun getCode() = code

    fun deleteComments() {
        code = code.replace("""/\*.+?\*/""".toRegex(), "")
    }

    fun getUrlProperties(): List<String> {
        val propertyRegEx = """[A-Za-z\-]+[ ]*:[A-Za-z\- ]*?url\(.*?\)""".toRegex()
        return propertyRegEx.findAll(code).toList().map { it.value }
    }

    private fun processUrl(property: String, value: String) {
        var address = value.trim()
        if (property == "background-image") {
            address = address
                .removeSurrounding("url(", ")") // TODO case
                .removeSurrounding("\"", "\"")
                .removeSurrounding("'", "'")
//            if (address.take(4).toLowerCase() == "url(" && address.takeLast(1) == ")") {
//                address = address.drop(4).dropLast(1)
//            }
            // TODO decisions
            LoaderUtil.addReplacerAuto(address, base)
        }
    }

    private fun process() {
        deleteComments()
        val properties = getUrlProperties()
        properties.map { it.split(':') }.forEach { processUrl(it[0], it[1]) }
    }
}