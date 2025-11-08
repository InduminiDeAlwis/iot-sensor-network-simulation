package client;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.Random;

public class IoTDevice {
    private final String id;

    public IoTDevice(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    /**
     * Simple main to send a few records to server: args = [host] [port] [deviceId]
     */
    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 9000;
        String deviceId = args.length > 2 ? args[2] : "deviceX";

        IoTDevice dev = new IoTDevice(deviceId);
        try (Socket s = new Socket(host, port);
             PrintWriter out = new PrintWriter(s.getOutputStream(), true);
             java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(s.getInputStream()))) {

            Random rnd = new Random();
            for (int i = 0; i < 5; i++) {
                long ts = System.currentTimeMillis();
                double value = 20 + rnd.nextDouble() * 100; // 20..120
                String line = String.format("%s,%d,%.3f", dev.id, ts, value);
                out.println(line);
                String resp = in.readLine();
                System.out.println("Server response: " + resp + " for " + line);
                Thread.sleep(800);
            }
        }
    }
}
