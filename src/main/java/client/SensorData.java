package client;

public class SensorData {
    private String deviceId;
    private long timestamp;
    private double value;

    public SensorData(String deviceId, long timestamp, double value) {
        this.deviceId = deviceId;
        this.timestamp = timestamp;
        this.value = value;
    }

    public String getDeviceId() { return deviceId; }
    public long getTimestamp() { return timestamp; }
    public double getValue() { return value; }
}
