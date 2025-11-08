package test.client;

import client.IoTDevice;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import server.DataStorage;
import server.IoTServer;
import server.ServerSingleton;

import java.net.ServerSocket;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ClientIT {
    private static int port;

    @BeforeAll
    public static void setup() throws Exception {
        // pick an ephemeral port to avoid collisions
        try (ServerSocket ss = new ServerSocket(0)) {
            port = ss.getLocalPort();
        }
        // start server on ephemeral port
        IoTServer.start(port);
        // give server a moment to boot
        Thread.sleep(200);
    }

    @AfterAll
    public static void teardown() {
        IoTServer.stop();
    }

    @Test
    public void testClientSendsDataAndServerStoresIt() throws Exception {
        // run the IoTDevice main to send several readings
        String[] args = new String[]{"localhost", String.valueOf(port), "itest-device"};
        IoTDevice.main(args);

        // wait briefly for server to process
        Thread.sleep(300);

        DataStorage storage = ServerSingleton.getStorage();
        assertTrue(storage.all().size() > 0, "Server should have stored at least one SensorData");
    }
}
