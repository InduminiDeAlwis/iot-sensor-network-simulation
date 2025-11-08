package server;

import utils.Config;
import utils.LoggerUtil;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IoTServer {
    private static final ExecutorService POOL = Executors.newFixedThreadPool(8);

    public static void main(String[] args) {
        LoggerUtil.info("Starting IoTServer on port " + Config.SERVER_PORT);
        try (ServerSocket server = new ServerSocket(Config.SERVER_PORT)) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                LoggerUtil.info("Shutting down server");
                POOL.shutdownNow();
            }));

            while (!server.isClosed()) {
                Socket client = server.accept();
                LoggerUtil.info("Accepted connection from " + client.getRemoteSocketAddress());
                POOL.submit(new ClientHandler(client));
            }
        } catch (Exception e) {
            LoggerUtil.error("Server error: " + e.getMessage());
        }
    }
}
