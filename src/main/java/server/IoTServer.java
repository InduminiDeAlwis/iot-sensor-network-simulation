package server;

import utils.Config;
import utils.JSONUtil;
import utils.LoggerUtil;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IoTServer {
    private static ExecutorService POOL = Executors.newFixedThreadPool(8);
    private static ServerSocket serverSocket;
    private static Thread acceptThread;
    private static volatile int port = Config.SERVER_PORT;
    private static HttpServer httpServer;


    public static synchronized void start(int p) throws IOException {
        if (serverSocket != null && !serverSocket.isClosed()) return;
        port = p;
        serverSocket = new ServerSocket(port);
        POOL = Executors.newFixedThreadPool(8);
        acceptThread = new Thread(() -> {
            try {
                LoggerUtil.info("IoTServer accepting on port " + serverSocket.getLocalPort());
                while (!serverSocket.isClosed()) {
                    Socket client = serverSocket.accept();
                    LoggerUtil.info("Accepted connection from " + client.getRemoteSocketAddress());
                    POOL.submit(new ClientHandler(client));
                }
            } catch (IOException e) {
                if (!serverSocket.isClosed()) LoggerUtil.error("Server accept error: " + e.getMessage());
            }
        }, "IoTServer-Accept");
        acceptThread.start();
        // start a simple HTTP API for the dashboard
        startHttpApi();
    }

    private static void startHttpApi() {
        try {
            if (httpServer != null) return;
            httpServer = HttpServer.create(new InetSocketAddress(Config.DASHBOARD_PORT), 0);
            httpServer.createContext("/api/data", new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                        exchange.sendResponseHeaders(405, -1);
                        return;
                    }
                    String resp = JSONUtil.toJson(ServerSingleton.getStorage().all());
                    byte[] bytes = resp.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
                    // Allow dashboard to access API from same origin or file served
                    exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
                }
            });
            // provide recent comms from data/comm.log
            httpServer.createContext("/api/comms", new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                        exchange.sendResponseHeaders(405, -1);
                        return;
                    }
                    java.nio.file.Path log = java.nio.file.Paths.get("data").resolve("comm.log");
                    java.util.List<String> lines = new java.util.ArrayList<>();
                    if (java.nio.file.Files.exists(log)) {
                        try {
                            lines = java.nio.file.Files.readAllLines(log, java.nio.charset.StandardCharsets.UTF_8);
                        } catch (Exception e) {
                            LoggerUtil.error("Failed to read comm.log: " + e.getMessage());
                        }
                    }
                    // take last 200 lines
                    int max = 200;
                    int start = Math.max(0, lines.size() - max);
                    java.util.List<String> tail = lines.subList(start, lines.size());

                    // parse lines like: [ISO_INSTANT] DIR: message
                    StringBuilder sb = new StringBuilder();
                    sb.append('[');
                    boolean first = true;
                    for (String l : tail) {
                        if (l == null || l.trim().isEmpty()) continue;
                        String ts = "";
                        String dir = "";
                        String msg = "";
                        try {
                            int br = l.indexOf(']');
                            if (l.startsWith("[") && br > 0) {
                                ts = l.substring(1, br);
                                String rest = l.substring(br + 1).trim();
                                int colon = rest.indexOf(':');
                                if (colon > 0) {
                                    dir = rest.substring(0, colon).trim();
                                    msg = rest.substring(colon + 1).trim();
                                } else {
                                    msg = rest;
                                }
                            } else {
                                msg = l;
                            }
                        } catch (Exception ignored) {}
                        if (!first) sb.append(',');
                        first = false;
                        sb.append('{')
                          .append("\"timestamp\":\"").append(ts.replace("\"","\\\"")).append('\"')
                          .append(',')
                          .append("\"direction\":\"").append(dir.replace("\"","\\\"")).append('\"')
                          .append(',')
                          .append("\"msg\":\"").append(msg.replace("\"","\\\"" )).append('"')
                          .append('}');
                    }
                    sb.append(']');

                    byte[] bytes = sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
                    exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
                }
            });
            // serve static dashboard files from ./dashboard
            httpServer.createContext("/", new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    String uriPath = exchange.getRequestURI().getPath();
                    if (uriPath == null || uriPath.equals("/")) uriPath = "/index.html";
                    // resolve file under dashboard directory
                    java.nio.file.Path base = java.nio.file.Paths.get("dashboard").toAbsolutePath().normalize();
                    java.nio.file.Path resolved = base.resolve(uriPath.substring(1)).normalize();
                    if (!resolved.startsWith(base) || !java.nio.file.Files.exists(resolved) || java.nio.file.Files.isDirectory(resolved)) {
                        exchange.sendResponseHeaders(404, -1);
                        return;
                    }
                    byte[] bytes = java.nio.file.Files.readAllBytes(resolved);
                    String contentType = guessContentType(resolved.toString());
                    exchange.getResponseHeaders().add("Content-Type", contentType);
                    // allow CORS for local file from browser if needed
                    exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
                }
            });
            httpServer.setExecutor(Executors.newSingleThreadExecutor(r -> new Thread(r, "HttpApi")));
            httpServer.start();
            LoggerUtil.info("HTTP API started on port " + Config.DASHBOARD_PORT);
        } catch (IOException e) {
            LoggerUtil.error("Failed to start HTTP API: " + e.getMessage());
        }
    }

    private static String guessContentType(String path) {
        String lc = path.toLowerCase();
        if (lc.endsWith(".html") || lc.endsWith(".htm")) return "text/html; charset=utf-8";
        if (lc.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (lc.endsWith(".css")) return "text/css; charset=utf-8";
        if (lc.endsWith(".json")) return "application/json; charset=utf-8";
        if (lc.endsWith(".png")) return "image/png";
        if (lc.endsWith(".jpg") || lc.endsWith(".jpeg")) return "image/jpeg";
        if (lc.endsWith(".svg")) return "image/svg+xml";
        return "application/octet-stream";
    }

    public static synchronized void stop() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            LoggerUtil.error("Error closing server socket: " + e.getMessage());
        }
        try {
            if (acceptThread != null) acceptThread.join(1000);
        } catch (InterruptedException ignored) {}
        if (POOL != null) POOL.shutdownNow();
        serverSocket = null;
        acceptThread = null;
        if (httpServer != null) {
            httpServer.stop(0);
            httpServer = null;
        }
    }

    public static int getPort() { return port; }

    public static void main(String[] args) {
        int p = Config.SERVER_PORT;
        if (args != null && args.length > 0) {
            try { p = Integer.parseInt(args[0]); } catch (Exception ignored) {}
        }
        try {
            start(p);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                LoggerUtil.info("Shutting down server");
                stop();
            }));
            // block main thread while accept thread runs
            if (acceptThread != null) acceptThread.join();
        } catch (Exception e) {
            LoggerUtil.error("Server error: " + e.getMessage());
        }
    }
}
