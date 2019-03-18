class JSWalker(private var code: String, private val relativePath : String = "/") {
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
}