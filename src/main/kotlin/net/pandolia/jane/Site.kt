package net.pandolia.jane

import net.pandolia.jane.libs.*
import java.time.LocalDate
import java.util.LinkedList

@Suppress("unused", "PropertyName")
class Category(val cate_name: String) { val cate_pages = LinkedList<Page>() }

object Site {
    var year = ""
    var title = ""
    var owner_email = ""
    var owner_name = ""
    var owner_icp_url = ""
    var owner_icp = ""

    val pages = LinkedList<Page>()

    @Suppress("unused")
    val categories: LinkedList<Category> get() {
        val cateList = LinkedList<Category>()

        pages.forEach { page ->
            if (!page.is_article) {
                return@forEach
            }

            var cate = cateList.find { it.cate_name == page.category }
            if (cate == null) {
                cate = Category(page.category)
                cateList.add(cate)
            }

            cate.cate_pages.addFirst(page)
        }

        cateList.sortByDescending { it.cate_pages.count() }
        return cateList
    }

    val nav_pages get() = pages.filter { !it.is_article }

    val development_mode = (Proc.command == "dev")

    fun assignConfig(props: Map<String, String>) {
        year = LocalDate.now().year.toString()
        title = props["title"] ?: "NO-TITLE"
        owner_email = props["owner_email"] ?: "NO-OWNER-EMAIL"
        owner_name = props["owner_name"] ?: "NO-OWNER-NAME"
        owner_icp_url = props["owner_icp_url"] ?: "NO-OWNER-ICP-URL"
        owner_icp = props["owner_icp"] ?: "NO-OWNER-ICP"

        Log.debug("""
        |    site.year: $year
        |    site.title: $title
        |    site.owner_email: $owner_email
        |    site.owner_name: $owner_name
        |    site.owner_icp_url: $owner_icp_url
        |    site.owner_icp: $owner_icp
        |    site.development_mode: $development_mode
        """)
    }
}

fun loadConfig() {
    Log.info("Jane project's root directory: $rootDir")

    testDirectory(pageDir)
    testDirectory(templateDir)
    testDirectory(staticDir)

    reloadConfig()
}

fun reloadConfig() {
    val props = tryExec("Read site config from ./$configFile") {
        getPropsFromFile(configFile)
    }

    Site.assignConfig(props)
}