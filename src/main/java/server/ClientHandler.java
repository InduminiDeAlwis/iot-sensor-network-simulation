package server;

import client.SensorData;
import utils.LoggerUtil;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final DataStorage storage = ServerSingleton.getStorage();
    private final AlertSystem alertSystem = ServerSingleton.getAlertSystem();

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            String line;
            while ((line = in.readLine()) != null) {
                // expected format: deviceId,timestamp,value
                // verbose logging: log raw RX line
                LoggerUtil.info("[RX] " + line);
                appendCommLog("RX", line);

                String[] parts = line.split(",");
                if (parts.length >= 3) {
                    String deviceId = parts[0];
                    long ts = Long.parseLong(parts[1]);
                    double value = Double.parseDouble(parts[2]);
                    SensorData data = new SensorData(deviceId, ts, value);
                    storage.store(data);
                    alertSystem.checkAndAlert(deviceId, value);
                    String resp = "ACK";
                    out.println(resp);
                    LoggerUtil.info("[TX] " + resp + " for " + deviceId + "," + ts + "," + value);
                    appendCommLog("TX", resp + " for " + deviceId + "," + ts + "," + value);
                } else {
                    String resp = "ERR: bad format";
                    out.println(resp);
                    LoggerUtil.info("[TX] " + resp + " (bad format)");
                    appendCommLog("TX", resp + " (bad format)");
                }
            }

        } catch (Exception e) {
            LoggerUtil.error("ClientHandler error: " + e.getMessage());
        } finally {
            try { socket.close(); } catch (Exception ignored) {}
        }
    }

        // append a simple comms log entry to data/comm.log
        private void appendCommLog(String direction, String msg) {
            try {
                java.nio.file.Path dataDir = java.nio.file.Paths.get("data");
                if (!java.nio.file.Files.exists(dataDir)) java.nio.file.Files.createDirectories(dataDir);
                java.nio.file.Path log = dataDir.resolve("comm.log");
                String line = String.format("[%s] %s: %s\n", java.time.Instant.now().toString(), direction, msg);
                // append atomically
                java.nio.file.Files.write(log, line.getBytes(java.nio.charset.StandardCharsets.UTF_8), java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
            } catch (Exception e) {
                LoggerUtil.error("Failed to write comm.log: " + e.getMessage());
            }
        }
}
