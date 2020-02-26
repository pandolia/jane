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

val clients = LinkedList<WsConnectContext>()

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
                Log.debug("Client-${ctx.sessionId} connected")
            }
        }

        ws.onClose { ctx ->
            mainQueue.put {
                clients.removeOne { it.sessionId == ctx.sessionId }
                Log.debug("Client-${ctx.sessionId} disconnected")
            }
        }
    }

    Log.info("Start development server at http://localhost:$serverPort/")
    app.start(serverPort)

    mainQueue.onStop { app.stop() }
    mainQueue.put { Desk.openBrowser("http://localhost:$serverPort/") }
}

fun onHttpGet(urlPath: String): Response {
    return try {
        onHttpGet0(urlPath)
    } catch (ex: Exception) {
        http500("${ex.message}\n${ex.stackTrace.joinToString("\n")}")
    }
}

fun onHttpGet0(urlPath: String): Response {
    val file = File("$staticDir$urlPath")
    if (file.isFile) {
        return fileResponse(file)
    }

    val pageName = when  {
        urlPath == "/" -> "index"
        urlPath.endsWith(".html") -> urlPath.substring(1, urlPath.length - 5)
        else -> return http404
    }

    val page = getPage("$pageDir/$pageName.md") ?: return http404

    return Response(page.renderToInputStream(), "text/html; charset=utf-8", 200)
}

fun notifyClientsToReload() {
    clients.forEach { ctx ->
        Try.get("Notify client-${ctx.sessionId} to reload") {
            ctx.send("Reload")
        }
    }
}