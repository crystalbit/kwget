enum class Options {
    ALWAYS, EXTERNAL_ASK, ASK, LOCAL, LOCAL_ASK, PROCESS
}

enum class Types {
    IMAGE, HTML, CSS, MEDIA, JS, UNIVERSAL, STRICT
}


fun main(args: Array<String>) {
    LoaderUtil.prepareDownload()

    if (args.isEmpty()) {
        println("no args")
        return
    }

    LoaderUtil.addReplacerAuto(args[0], "")!!
    LoaderUtil.printReplacers()
}