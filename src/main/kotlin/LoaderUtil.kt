import org.apache.commons.io.FilenameUtils
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

var replacers = arrayListOf<Pair<String, String>>()
var agreed = arrayListOf<String>()
var declined = arrayListOf<String>()

enum class Decision { AGREED, DECLINED, NOT_DECIDED }

object LoaderUtil {
    @Throws(Exception::class)
    @JvmStatic
    fun getFileName(url: String) : String {
        var fileName = FilenameUtils.getName(URL(url).path)
        if (fileName == "") {
            val urlParts = url.split("/").filter { it != "" }
            return urlParts.last()
        }
        if (!fileName.contains(".")) return fileName + ".html" // TODO generic types to add not only ".html"
        return fileName
    }

    fun prepareDownload() {
        if (File(downloadPath("")).isDirectory) {
            File(downloadPath("")).listFiles().map {
                File(it.absolutePath).delete()
            }
        } else {
            File(downloadPath("")).mkdirs()
        }
    }

    fun getUrlBase(source: String): String {
        val url = URL(source)
        return "${url.protocol}://${url.host}${url.path.substring(0..url.path.lastIndexOf('/'))}"
    }

    fun hasReplacer(from: String): Boolean {
        return (replacers.find { it.first == from } != null)
    }

    fun getReplacer(from: String): String? {
        return (replacers.find { it.first == from }?.second)
    }

    enum class MimeType {
        HTML, CSS, JS, OTHER
    }

    fun getType(to: String): MimeType {
        val extension = to.split("?").first()
                        .split("#").first()
                        .split(".").last().toLowerCase()
        return when (extension) {
            "php", "asp", "html", "htm", "php5" -> MimeType.HTML
            "css" -> MimeType.CSS
            "js" -> MimeType.JS
            else -> MimeType.OTHER
        }
    }

    fun addReplacer(from: String, to: String, base: String = ""): String? {
        //println("REPL $from - $to - $base")
        if (File(to).exists()) {
            println("$to already exists, but replacement will be added")
            return null
        }

        var source = from
        if (!isExternal(source)) source = combineBaseAndURL(base, from)
        if (replacers.find { it.first == from || it.first == source } != null) {
            println("$source already planned to download/downloaded")
            return null
        }
        replacers.add(from to to)
        replacers.add(source to to)
        download(source, to)
        when (getType(to)) {
            MimeType.HTML, MimeType.CSS, MimeType.JS -> {
                val base = getUrlBase(source)
                //println(to)
                val code = File(downloadPath(to)).readText()
                callWalkerByType(getType(to), code, base)
            }
        }
        return to


    }

    private fun callWalkerByType(type: MimeType, code: String, base: String = "") {
        when (type) {
            LoaderUtil.MimeType.HTML -> HTMLWalker(code, base)
            LoaderUtil.MimeType.CSS -> CSSWalker(code, base)
            LoaderUtil.MimeType.JS -> JSWalker(code, base)
        }
    }

    fun shallDownload(url: String, strict: Boolean = false): Boolean {
        if (url.startsWith("http://", true)) return true
        if (url.startsWith("//", true)) return true
        if (url.startsWith("https://", true)) return true
        if (url.contains(':')) return false

        if (strict && !url.contains(".")) return false
        if (strict && url.contains(" ")) return false
        if (strict && url.contains(",")) return false
        return true
    }

    fun isExternal(url: String): Boolean {
        if (url.startsWith("http://", true)) return true
        if (url.startsWith("//", true)) return true
        if (url.startsWith("https://", true)) return true
        return false
    }

    fun combineBaseAndURL(base: String, url: String): String {
        return base + (if (base.lastOrNull() == '/' || url.firstOrNull() == '/') "" else "/") + url
    }

    fun addReplacerAuto(from: String, base: String = ""): String? {
        val fileName = getFileName(if (isExternal(from)) from else combineBaseAndURL(base, from))
        addReplacer(from, fileName, base)
        return fileName
    }

    fun printReplacers() {
        replacers.forEach { println("REPLACER ${it.first} to ${it.second}") }
        // TODO local replacers in files
    }

    fun downloadPath(filename: String): String {
        return "download/$filename"
    }

    fun download(from: String, to: String) {
        println("loading $from ...")
        try {
            val url = URL(from)
            val writer = File(downloadPath(to)).outputStream()
            val connection = url.openStream()
            connection.copyTo(writer)
            connection.close()
            writer.close()
        } catch (error: java.io.FileNotFoundException) {
            println(error)
        }
    }

    fun addAgreed(url: String) {
        agreed.add(url)
    }

    fun addDeclined(url: String) {
        declined.add(url)
    }

    fun getDecision(url: String): Decision {
        return when (url) {
            in agreed -> Decision.AGREED
            in declined -> Decision.DECLINED
            else -> Decision.NOT_DECIDED
        }
    }
}