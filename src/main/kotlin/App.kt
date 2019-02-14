import java.io.IOException
import java.net.URL

enum class Options {
    ALWAYS, EXTERNAL_ASK, ASK, LOCAL, LOCAL_ASK
}
enum class Types {
    IMAGE, HTML, CSS, MEDIA, JS, UNIVERSAL
}

class CSSWalker(private var code: String, private val relativePath : String = "/") {
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

class HTMLWalker(private val code: String) {
    private val toDownload: Map<Pair<String, String>, Pair<Options, Types>> = mapOf(
        Pair("link", "rel") to Pair(Options.ASK, Types.CSS),
        Pair("iframe", "src") to Pair(Options.ASK, Types.HTML),
        Pair("meta", "property") to Pair(Options.ASK, Types.HTML),
        Pair("script", "src") to Pair(Options.EXTERNAL_ASK, Types.JS),
        Pair("img", "src") to Pair(Options.EXTERNAL_ASK, Types.IMAGE),
        Pair("a", "href") to Pair(Options.LOCAL_ASK, Types.HTML),
        Pair("form", "action") to Pair(Options.ASK, Types.HTML)
    )
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
    private fun offerDownload(_tagName: String, _tagPropertyName: String, _tagPropertyValue: String) {
        when (toDownload[Pair(_tagName, _tagPropertyName)]) {
            Options.ALWAYS -> {
                println("ALWAYS")
            }
            Options.ASK -> {
                println("ASK")
            }
            Options.EXTERNAL_ASK -> {
                println("EXTERNAL_ASK")
            }
            Options.LOCAL -> {
                println("LOCAL")
            }
            Options.LOCAL_ASK -> {
                println("LOCAL_ASK")
            }
            // else null and we don't hold it
        }
        println("$_tagName -> $_tagPropertyName = $_tagPropertyValue")
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

    fun processLoop() {
        symbolLoop@ for (i in 0 until code.length) {
            when {
                inComment -> {
                    comment += code[i]
                    if ("${code[i - 2]}${code[i - 1]}${code[i]}" == "-->") {
                        inComment = false
                        println("COMMENT ${comment.dropLast(3)}")
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
                ' ' -> {
                    //if (inTagPropertyValue) continue@symbolLoop
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



fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("no args")
        return
    }
    val css = CSSWalker("""
/** Default CSS https:/docs/w/load.php?debug=false&amp;lang=en&amp;modules=mediawiki.legacy.commonPrint%2Cshared%7Cmediawiki.ui.button%7Cskins.webplatform&amp;only=styles&amp;skin=webplatform&amp;*  */
@media print{.noprint,div#jump-to-nav,.mw-jump,div.top,div#column-one,#colophon,.mw-editsection,.mw-editsection-like,.toctoggle,#toc.tochidden,div#f-poweredbyico,div#f-copyrightico,li#about,li#disclaimer,li#mobileview,li#privacy,#footer-places,.mw-hidden-catlinks,tr.mw-metadata-show-hide-extended,span.mw-filepage-other-resolutions,#filetoc,.usermessage,.patrollink,.ns-0 .mw-redirectedfrom,#mw-navigation,#siteNotice{display:none}.wikitable,.thumb,img{page-break-inside:avoid}h2,h3,h4,h5,h6{page-break-after:avoid}p{widows:3;orphans:3}body{background:white;color:black;margin:0;padding:0}ul{list-style-type:square}h1,h2,h3,h4,h5,h6{font-weight:bold}dt{font-weight:bold}p{margin:1em 0;line-height:1.2em}pre,.mw-code{border:1pt dashed black;white-space:pre;font-size:8pt;overflow:auto;padding:1em 0;background:white;color:black}#globalWrapper{width:100% !important;min-width:0 !important}.mw-body{background:white;border:none !important;padding:0 !important;margin:0 !important;direction:ltr;color:black}#column-content{margin:0 !important}#column-content .mw-body{padding:1em;margin:0 !important}#toc{border:1px solid #aaaaaa;background-color:#f9f9f9;padding:5px;display:-moz-inline-block;display:inline-block;display:table;zoom:1;*display:inline}#footer{background:white;color:black;margin-top:1em;border-top:1px solid #AAA;direction:ltr}img{border:none;vertical-align:middle}span.texhtml{font-family:serif}a.stub,a.new{color:#ba0000;text-decoration:none}a{color:black !important;background:none !important;padding:0 !important}a:link,a:visited{color:#520;background:transparent;text-decoration:underline}.mw-body a.external.text:after,.mw-body a.external.autonumber:after{content:" (" attr(href) ")"}.mw-body a.external.text[href^='//']:after,.mw-body a.external.autonumber[href^='//']:after{content:" (https:" attr(href) ")"}a,a.external,a.new,a.stub{color:black !important;text-decoration:none !important}a,a.external,a.new,a.stub{color:inherit !important;text-decoration:inherit !important}div.floatright{float:right;clear:right;position:relative;margin:0.5em 0 0.8em 1.4em}div.floatright p{font-style:italic}div.floatleft{float:left;clear:left;position:relative;margin:0.5em 1.4em 0.8em 0}div.floatleft p{font-style:italic}div.center{text-align:center}div.thumb{border:none;width:auto;margin-top:0.5em;margin-bottom:0.8em;background-color:transparent}div.thumbinner{border:1px solid #cccccc;padding:3px !important;background-color:White;font-size:94%;text-align:center;overflow:hidden}html .thumbimage{border:1px solid #cccccc}html .thumbcaption{border:none;text-align:left;line-height:1.4em;padding:3px !important;font-size:94%}div.magnify{display:none}div.tright{float:right;clear:right;margin:0.5em 0 0.8em 1.4em}div.tleft{float:left;clear:left;margin:0.5em 1.4em 0.8em 0}img.thumbborder{border:1px solid #dddddd}li.gallerybox{vertical-align:top;display:inline-block}ul.gallery,li.gallerybox{zoom:1;*display:inline}ul.gallery{margin:2px;padding:2px;display:block}li.gallerycaption{font-weight:bold;text-align:center;display:block;word-wrap:break-word}li.gallerybox div.thumb{text-align:center;border:1px solid #ccc;margin:2px}div.gallerytext{overflow:hidden;font-size:94%;padding:2px 4px;word-wrap:break-word}table.wikitable,table.mw_metadata{margin:1em 0;border:1px #aaa solid;background:white;border-collapse:collapse}table.wikitable > tr > th,table.wikitable > tr > td,table.wikitable > * > tr > th,table.wikitable > * > tr > td,.mw_metadata th,.mw_metadata td{border:1px #aaa solid;padding:0.2em}table.wikitable > tr > th,table.wikitable > * > tr > th,.mw_metadata th{text-align:center;background:white;font-weight:bold}table.wikitable > caption,.mw_metadata caption{font-weight:bold}table.listing,table.listing td{border:1pt solid black;border-collapse:collapse}a.sortheader{margin:0 0.3em}.catlinks ul{display:inline;margin:0;padding:0;list-style:none;list-style-type:none;list-style-image:none;vertical-align:middle !ie}.catlinks li{display:inline-block;line-height:1.15em;padding:0 .4em;border-left:1px solid #AAA;margin:0.1em 0;zoom:1;display:inline !ie}.catlinks li:first-child{padding-left:.2em;border-left:none}.printfooter{padding:1em 0 1em 0}}@media screen{.mw-content-ltr{direction:ltr}.mw-content-rtl{direction:rtl}.sitedir-ltr textarea,.sitedir-ltr input{direction:ltr}.sitedir-rtl textarea,.sitedir-rtl input{direction:rtl}.mw-userlink{unicode-bidi:embed}mark{background-color:yellow;color:black}wbr{display:inline-block}input[type="submit"],input[type="button"],input[type="reset"],input[type="file"]{direction:ltr}textarea[dir="ltr"],input[dir="ltr"]{direction:ltr}textarea[dir="rtl"],input[dir="rtl"]{direction:rtl}abbr[title],.explain[title]{border-bottom:1px dotted;cursor:help}.mw-plusminus-pos{color:#006400}.mw-plusminus-neg{color:#8b0000}.mw-plusminus-null{color:#aaa}.allpagesredirect,.redirect-in-category,.watchlistredir{font-style:italic}span.comment{font-style:italic}span.changedby{font-size:95%}.texvc{direction:ltr;unicode-bidi:embed}img.tex{vertical-align:middle}span.texhtml{font-family:serif}#wikiPreview.ontop{margin-bottom:1em}#editform,#toolbar,#wpTextbox1{clear:both}div.mw-filepage-resolutioninfo{font-size:smaller}h2#filehistory{clear:both}table.filehistory th,table.filehistory td{vertical-align:top}table.filehistory th{text-align:left}table.filehistory td.mw-imagepage-filesize,table.filehistory th.mw-imagepage-filesize{white-space:nowrap}table.filehistory td.filehistory-selected{font-weight:bold}.filehistory a img,#file img:hover{background:white url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAAAAAA6mKC9AAAAGElEQVQYV2N4DwX/oYBhgARgDJjEAAkAAEC99wFuu0VFAAAAAElFTkSuQmCC) repeat;background:white url(/docs/w/resources/src/mediawiki.legacy/images/checker.png?2015-01-30T03:46:40Z) repeat!ie}li span.deleted,span.history-deleted{text-decoration:line-through;color:#888;font-style:italic}.not-patrolled{background-color:#ffa}.unpatrolled{font-weight:bold;color:red}div.patrollink{font-size:75%;text-align:right}td.mw-label{text-align:right}td.mw-input{text-align:left}td.mw-submit{text-align:left}td.mw-label{vertical-align:top}.prefsection td.mw-label{width:20%}.prefsection table{width:100%}.prefsection table.mw-htmlform-matrix{width:auto}.mw-icon-question{background-image:url(/docs/w/resources/src/mediawiki.legacy/images/question.png?2015-01-30T03:46:40Z);background-image:-webkit-linear-gradient(transparent,transparent),url(data:image/svg+xml,%3C%3Fxml%20version%3D%221.0%22%20encoding%3D%22UTF-8%22%3F%3E%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20width%3D%2221.059%22%20height%3D%2221.06%22%3E%3Cpath%20fill%3D%22%23575757%22%20d%3D%22M10.529%200c-5.814%200-10.529%204.714-10.529%2010.529s4.715%2010.53%2010.529%2010.53c5.816%200%2010.529-4.715%2010.529-10.53s-4.712-10.529-10.529-10.529zm-.002%2016.767c-.861%200-1.498-.688-1.498-1.516%200-.862.637-1.534%201.498-1.534.828%200%201.5.672%201.5%201.534%200%20.827-.672%201.516-1.5%201.516zm2.137-6.512c-.723.568-1%20.931-1%201.739v.5h-2.205v-.603c0-1.517.449-2.136%201.154-2.688.707-.552%201.139-.845%201.139-1.637%200-.672-.414-1.051-1.24-1.051-.707%200-1.328.189-1.982.638l-1.051-1.807c.861-.604%201.93-1.034%203.342-1.034%201.912%200%203.516%201.051%203.516%203.066-.001%201.43-.794%202.188-1.673%202.877z%22%2F%3E%3C%2Fsvg%3E);background-image:-webkit-linear-gradient(transparent,transparent),url(/docs/w/resources/src/mediawiki.legacy/images/question.svg?2015-01-30T03:46:40Z)!ie;background-image:linear-gradient(transparent,transparent),url(data:image/svg+xml,%3C%3Fxml%20version%3D%221.0%22%20encoding%3D%22UTF-8%22%3F%3E%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20width%3D%2221.059%22%20height%3D%2221.06%22%3E%3Cpath%20fill%3D%22%23575757%22%20d%3D%22M10.529%200c-5.814%200-10.529%204.714-10.529%2010.529s4.715%2010.53%2010.529%2010.53c5.816%200%2010.529-4.715%2010.529-10.53s-4.712-10.529-10.529-10.529zm-.002%2016.767c-.861%200-1.498-.688-1.498-1.516%200-.862.637-1.534%201.498-1.534.828%200%201.5.672%201.5%201.534%200%20.827-.672%201.516-1.5%201.516zm2.137-6.512c-.723.568-1%20.931-1%201.739v.5h-2.205v-.603c0-1.517.449-2.136%201.154-2.688.707-.552%201.139-.845%201.139-1.637%200-.672-.414-1.051-1.24-1.051-.707%200-1.328.189-1.982.638l-1.051-1.807c.861-.604%201.93-1.034%203.342-1.034%201.912%200%203.516%201.051%203.516%203.066-.001%201.43-.794%202.188-1.673%202.877z%22%2F%3E%3C%2Fsvg%3E);background-image:linear-gradient(transparent,transparent),url(/docs/w/resources/src/mediawiki.legacy/images/question.svg?2015-01-30T03:46:40Z)!ie;background-repeat:no-repeat;background-size:13px 13px;display:inline-block;height:13px;width:13px;margin-left:4px}.mw-icon-question:lang(ar),.mw-icon-question:lang(fa),.mw-icon-question:lang(ur){-webkit-transform:scaleX(-1);-ms-transform:scaleX(-1);transform:scaleX(-1)}td.mw-submit{white-space:nowrap}table.mw-htmlform-nolabel td.mw-label{width:1px}tr.mw-htmlform-vertical-label td.mw-label{text-align:left !important}.mw-htmlform-invalid-input td.mw-input input{border-color:red}.mw-htmlform-flatlist div.mw-htmlform-flatlist-item{display:inline;margin-right:1em;white-space:nowrap}.mw-htmlform-matrix td{padding-left:0.5em;padding-right:0.5em}input#wpSummary{width:80%;margin-bottom:1em}.mw-content-ltr .thumbcaption{text-align:left}.mw-content-ltr .magnify{float:right}.mw-content-rtl .thumbcaption{text-align:right}.mw-content-rtl .magnify{float:left}#catlinks{text-align:left}.catlinks ul{display:inline;margin:0;padding:0;list-style:none;list-style-type:none;list-style-image:none;vertical-align:middle !ie}.catlinks li{display:inline-block;line-height:1.25em;border-left:1px solid #AAA;margin:0.125em 0;padding:0 0.5em;zoom:1;display:inline !ie}.catlinks li:first-child{padding-left:0.25em;border-left:none}.catlinks li a.mw-redirect{font-style:italic}.mw-hidden-cats-hidden{display:none}.catlinks-allhidden{display:none}p.mw-ipb-conve
""".trimIndent())
    css.deleteComments()
    print(css.getUrlProperties())
    return
    val url = URL(args[0])
    var response = try {
        url.openStream().bufferedReader().use { it.readText() }
    } catch (e: IOException) {
        println(e.message)
        "Error with ${e.message}"
    }
    val walker = HTMLWalker(response)
    walker.processLoop()
}