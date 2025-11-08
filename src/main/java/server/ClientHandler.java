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
                String[] parts = line.split(",");
                if (parts.length >= 3) {
                    String deviceId = parts[0];
                    long ts = Long.parseLong(parts[1]);
                    double value = Double.parseDouble(parts[2]);
                    SensorData data = new SensorData(deviceId, ts, value);
                    storage.store(data);
                    alertSystem.checkAndAlert(deviceId, value);
                    out.println("ACK");
                    LoggerUtil.info("Received " + deviceId + "=" + value);
                } else {
                    out.println("ERR: bad format");
                }
            }

        } catch (Exception e) {
            LoggerUtil.error("ClientHandler error: " + e.getMessage());
        } finally {
            try { socket.close(); } catch (Exception ignored) {}
        }
    }
}
