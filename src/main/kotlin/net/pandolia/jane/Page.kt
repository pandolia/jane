package net.pandolia.jane

import net.pandolia.jane.libs.*
import java.io.File

val pageRelPathRegex = Regex("(\\d{4}/\\d{2}-\\d{2}-[0-9a-z-]+.md)|([^./]+.md)")

@Suppress("PropertyName", "MemberVisibilityCanBePrivate")
class Page(val page_name: String) {
    val page_path: String
    val target_path: String
    val template_path: String
    val is_article: Boolean
    val layer_string: String
    val create_date: String

    var is_valid: Boolean = false
    var title: String = ""
    var image: String = ""
    var category: String = ""
    var content: String = ""

    val category_id get() = category.packValue

    init {
        page_path = "$pageDir/$page_name.md"
        target_path = "$buildDir/$page_name.html"

        if (page_name.contains('/')) {
            is_article = true
            template_path = "$templateDir/article.mustache"
            layer_string = ".."
            create_date = "${page_name.substring(0, 4)}-${page_name.substring(5, 10)}"
        } else {
            is_article = false
            template_path = if (Fs.isFile("$templateDir/$page_name.mustache")) {
                "$templateDir/$page_name.mustache"
            } else {
                "$templateDir/default.mustache"
            }
            layer_string = "."
            create_date = ""
        }

        if (!Fs.isFile(template_path)) {
            Proc.abort("Template file $template_path for $page_path does not exist")
        }

        readProps()
    }

    fun readProps(): Boolean {
        val text = Try.get("Read $page_path") { Fs.readFile(page_path).trim() } ?: return false

        val pagePropsMarker = "---"
        val n = pagePropsMarker.length

        if (!text.startsWith(pagePropsMarker)) {
            Log.warn("Content of $page_path does not start with '---'")
            return false
        }

        val i = text.indexOf(pagePropsMarker, n)
        if (i == -1) {
            Log.warn("Content of $page_path does not contain two '---'")
            return false
        }

        val props = text.substring(n, i).parseToDict().toMutableMap()

        val title0 = props["title"] ?: ""
        val image0 = getImage(props["image"])
        val category0 = if (is_article) (props["category"] ?: "") else "NO-CATEGORY"
        val content0 = Markdown.md2html(text.substring(i + 3).trim())
        val isValid0 = title0.isNotEmpty() && category0.isNotEmpty()

        Log.debug("""
        |    page_name: $page_name
        |    page_path: $page_path
        |    target_path: $target_path
        |    template_path: $template_path
        |    is_article: $is_article
        |    layer_string: $layer_string
        |    create_date: $create_date
        |    title: $title0
        |    image: $image0
        |    category: $category0
        |    content_length: ${content0.length}
        |    is_valid: $isValid0
        """)

        if (!isValid0) {
            Log.warn("Tilte|category of $page_path is empty, this page is discarded")
            return false
        }

        title = title0
        image = image0
        category = category0
        content = content0
        is_valid = isValid0

        return true
    }

    fun getImage(imgId: String?) = when {
        imgId.isNullOrEmpty() -> "https://picsum.photos/2560/600"
        Fs.isFile("$staticDir/resources/image/$imgId.jpg") -> "$layer_string/resources/image/$imgId.jpg"
        else -> "https://picsum.photos/id/$imgId/2560/600"
    }

    fun renderIfNeeded() {
        if (!needRender()) {
            Log.info("Render($page_path, $template_path) -> $target_path. Skip")
            return
        }

        render()
    }

    private fun needRender(): Boolean {
        if (page_name == "index") {
            return true
        }

        val tTarget = File(target_path).lastModified()

        val tSrc = listOf(page_path, template_path, headerTmplPath, footerTmplPath, configFile)
            .map { File(it).lastModified() }
            .max()!!

        return tTarget <= tSrc
    }

    private fun getScope() = mapOf("site" to Site, "page" to this)

    private fun render() {
        Try.exec("Render($page_path, $template_path) -> $target_path") {
            Mustache.renderToFile(template_path, getScope(), target_path)
        }
    }

    fun renderToInputStream() = Mustache.renderToInputStream(template_path, getScope())
}

fun loadPages() {
    Fs.getChildFiles(pageDir).forEach { makePage(it.substring(rootDir.length + 1)) }
}

fun makePage(path: String): Boolean {
    val relPath = path.substring(pageDir.length + 1)

    if (pageRelPathRegex.matchEntire(relPath) == null) {
        Log.warn("Ilegal page file name: $relPath")
        return false
    }

    val page = Page(relPath.substringBeforeLast('.'))
    if (!page.is_valid) {
        return false
    }

    Site.pages.insert(page) { e, ei -> e.page_name <= ei.page_name }
    Log.info("Add Page ${page.page_path}")
    return true
}

fun renderPages() {
    Site.pages.forEach { it.renderIfNeeded() }
}

fun deletePage(path: String): Boolean {
    return Site.pages.removeOne { it.page_path == path }
}

fun getPage(path: String): Page? {
    return Site.pages.find { it.page_path == path }
}

fun hasPage(templatePath: String): Boolean {
    return templatePath == footerTmplPath
        || templatePath == headerTmplPath
        || Site.pages.any { it.template_path == templatePath }
}