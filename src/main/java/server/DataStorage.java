package server;

import client.SensorData;
import java.util.ArrayList;
import java.util.List;

public class DataStorage {
    private final List<SensorData> storage = new ArrayList<>();

    public synchronized void store(SensorData record) {
        storage.add(record);
    }

    public synchronized List<SensorData> all() { return new ArrayList<>(storage); }
}
