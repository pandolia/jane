package net.pandolia.jane.libs

import com.github.mustachejava.DefaultMustacheFactory
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet
import java.io.*
import java.security.MessageDigest

object Mustache {
    fun renderToFile(templateFile: String, scope: Any, outFile: String): String {
        val i = outFile.lastIndexOf('/')
        if (i != -1) {
            File(outFile.substring(0, i + 1)).mkdirs()
        }

        val mustache = DefaultMustacheFactory().compile(templateFile)
        val writer = OutputStreamWriter(FileOutputStream(outFile))
        mustache.execute(writer, scope)
        writer.flush()
        writer.close()

        return "ok"
    }

    fun renderToInputStream(templateFile: String, scope: Any): InputStream {
        val mustache = DefaultMustacheFactory().compile(templateFile)
        val outputStream = ByteArrayOutputStream()
        val writer = OutputStreamWriter(outputStream)

        mustache.execute(writer, scope)
        writer.flush()

        return ByteArrayInputStream(outputStream.toByteArray())
    }
}

object Markdown {
    private val mdOptions = MutableDataSet()
    private val mdParser = Parser.builder(mdOptions).build()
    private val mdRenderer = HtmlRenderer.builder(mdOptions).build()

    fun md2html(md: String) =  mdRenderer.render(mdParser.parse(md))
}

fun ByteArray.toHex(): String {
    return joinToString("") { "%02x".format(it) }
}

@Suppress("unused")
fun String.md5(): String {
    return MessageDigest.getInstance("MD5").digest(this.toByteArray()).toHex()
}

@Suppress("unused")
fun String.toCapital(): String {
    return this.split('-').joinToString(" ") { it.capitalize() }
}