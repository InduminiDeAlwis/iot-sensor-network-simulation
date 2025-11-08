package test.client;

public class ClientTest {
    public static void main(String[] args) throws Exception {
        // quick manual test runner: start server then run this
        String host = "localhost";
        int port = 9000;
        String id = "testDevice";
        String[] clientArgs = new String[]{host, String.valueOf(port), id};
        client.IoTDevice.main(clientArgs);
    }
}
