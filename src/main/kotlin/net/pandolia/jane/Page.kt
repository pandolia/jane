package net.pandolia.jane

import net.pandolia.jane.libs.*
import java.io.File

val pageRelPathRegex = Regex("(\\d{4}/\\d{2}-\\d{2}-[0-9a-z-]+.md)|([^./]+.md)")

@Suppress("PropertyName", "MemberVisibilityCanBePrivate")
class Page(val page_name: String) {
    val page_path: String
    val target_path: String
    val layer_string: String
    val create_date: String

    var title = ""
    var image = ""
    var category = ""
    var content_is_categories = false
    var content = ""
    var is_valid = false

    @Suppress("unused")
    val category_archor get() = "${Site.categories_page_name}.html#${category.packValue}"

    init {
        page_path = "$pageDir/$page_name.md"
        target_path = "$buildDir/$page_name.html"

        if (page_name.contains('/')) {
            layer_string = ".."
            create_date = "${page_name.substring(0, 4)}-${page_name.substring(5, 10)}"
        } else {
            layer_string = "."
            create_date = ""
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
        var content0 = text.substring(i + 3).trim()

        val title0 = props["title"] ?: ""
        val image0 = getImage(props["image"])
        val category0 = if (create_date.isNotEmpty()) (props["category"] ?: "") else "NO-CATEGORY"
        val isValid0 = title0.isNotEmpty() && category0.isNotEmpty()
        val contentIsCategories0 = (content0 == "[categories]")

        content0 = Markdown.md2html(content0)

        Log.debug("""
        |    page_name: $page_name
        |    page_path: $page_path
        |    target_path: $target_path
        |    layer_string: $layer_string
        |    create_date: $create_date
        |    title: $title0
        |    image: $image0
        |    category: $category0
        |    content_length: ${content0.length}
        |    content_is_categories: $contentIsCategories0
        |    is_valid: $isValid0
        """)

        if (!isValid0) {
            Log.warn("Tilte|category of $page_path is empty, this page is discarded")
            return false
        }

        if (contentIsCategories0) {
            Site.categories_page_name = page_name
        } else if (content_is_categories) {
            Site.categories_page_name = ""
        }

        title = title0
        image = image0
        category = category0
        content = content0
        content_is_categories = contentIsCategories0
        is_valid = isValid0

        return true
    }

    fun getImage(imgName: String?) = when {
        imgName.isNullOrEmpty() -> "https://picsum.photos/2560/600"
        imgName.startsWith("http") -> imgName
        else -> "$layer_string/resources/image/$imgName"
    }

    fun renderIfNeeded() {
        if (!needRender()) {
            Log.info("Render $page_path -> $target_path. Skip")
            return
        }

        render()
    }

    private fun needRender(): Boolean {
        if (content_is_categories) {
            return true
        }

        val tTarget = File(target_path).lastModified()

        val tSrc = listOf(page_path, templatePath, configFile)
            .map { File(it).lastModified() }
            .max()!!

        return tTarget <= tSrc
    }

    private fun getScope() = mapOf("site" to Site, "page" to this)

    private fun render() {
        Try.exec("Render $page_path -> $target_path") {
            Mustache.renderToFile(templatePath, getScope(), target_path)
        }
    }

    fun renderToInputStream() = Mustache.renderToInputStream(templatePath, getScope())
}

fun loadPages() {
    Fs.getChildFiles(pageDir).forEach {
        makePage(it.substring(rootDir.length + 1))
    }
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
    Log.info("Add page ${page.page_path}")
    return true
}

fun renderPages() {
    Site.pages.forEach { it.renderIfNeeded() }
}

fun deletePage(path: String): Boolean {
    if (!Site.pages.removeOne { it.page_path == path }) {
        return false
    }

    if (path.substringBefore('.') == Site.categories_page_name) {
        Site.categories_page_name = ""
    }

    Log.info("Remove page $path")
    return true
}

fun getPage(path: String): Page? {
    return Site.pages.find { it.page_path == path }
}