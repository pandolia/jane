package net.pandolia.jane

import io.javalin.Javalin
import io.javalin.websocket.WsConnectContext
import net.pandolia.jane.libs.*
import java.io.File
import java.io.InputStream
import java.util.*

class Response(
    val resultStream: InputStream,
    val contentType: String,
    val statusCode: Int
)

val http404 = Response("Not found".byteInputStream(), "text/plain; charset=utf-8", 404)

fun http500(text: String) = Response(text.byteInputStream(), "text/plain; charset=utf-8", 500)

fun fileResponse(file: File) = Response(file.inputStream(), file.mimeType, 200)

fun jarResourceResponse(resPath: String) = Response(
    Fs.getResourceURL(resPath).readBytes().inputStream(),
    Fs.getMimeTypeByFileName(resPath),
    200
)

private val clients = LinkedList<WsConnectContext>()

fun serveProject() {
    System.setProperty("org.slf4j.simpleLogger.logFile", "System.out")
    System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "error")

    val app = Javalin.create()

    app.get("/*") { ctx ->
        val path = ctx.path()
        val futrue = Futrue { onHttpGet(path) }
        val resp = futrue.wait()

        ctx.result(resp.resultStream)
            .contentType(resp.contentType)
            .status(resp.statusCode)
    }

    app.ws("/reload-on-change") { ws ->
        ws.onConnect { ctx ->
            mainQueue.put {
                clients.add(ctx)
                Log.info("Client-${ctx.sessionId.substring(0, 4)} connected")
                try {
                    ctx.send("heartbeat")
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }

        ws.onClose { ctx ->
            mainQueue.put {
                clients.removeOne { it.sessionId == ctx.sessionId }
                Log.info("Client-${ctx.sessionId.substring(0, 4)} disconnected")
            }
        }
    }

    Log.info("Start development server at http://localhost:$serverPort/")
    app.start(serverPort)

    mainQueue.onStop {
        broadcast("close page")
        Thread.sleep(1000)
        app.stop()
    }

    mainQueue.put {
        Desk.openBrowser("http://localhost:$serverPort/")
    }

    newThread {
        while (true) {
            Thread.sleep(30000)
            mainQueue.put { broadcast("heartbeat") }
        }
    }
}

private fun onHttpGet(urlPath: String): Response {
    return try {
        onHttpGet0(urlPath)
    } catch (ex: Exception) {
        http500("${ex.message}\n${ex.stackTrace.joinToString("\n")}")
    }
}

private fun onHttpGet0(urlPath: String): Response {
    val file = File("$staticDir$urlPath")
    if (file.isFile) {
        return fileResponse(file)
    }

    if (urlPath == "/reload.js") {
        return jarResourceResponse("/reload.js")
    }

    val pageName = when  {
        urlPath == "/" -> "index"
        urlPath.endsWith(".html") -> urlPath.substring(1, urlPath.length - 5)
        else -> return http404
    }

    val page = getPage("$pageDir/$pageName.md") ?: return http404

    return Response(page.renderToInputStream(), "text/html; charset=utf-8", 200)
}

fun broadcast(message: String) {
    if (clients.isEmpty()) {
        Log.info("Notify 0 client to $message")
        return
    }

    clients.forEach { ctx ->
        Try.get("Notify client-${ctx.sessionId.substring(0, 4)} to $message") {
            ctx.send(message)
        }
    }
}