package utils;

import client.SensorData;

import java.util.List;

public class JSONUtil {
    public static String toJson(Object o) {
        if (o == null) return "null";
        if (o instanceof SensorData) return sensorToJson((SensorData) o);
        if (o instanceof List) return listToJson((List<?>) o);
        // fallback
        return '"' + escape(o.toString()) + '"';
    }

    private static String listToJson(List<?> list) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        boolean first = true;
        for (Object item : list) {
            if (!first) sb.append(',');
            first = false;
            if (item instanceof SensorData) sb.append(sensorToJson((SensorData) item));
            else sb.append('"').append(escape(String.valueOf(item))).append('"');
        }
        sb.append(']');
        return sb.toString();
    }

    private static String sensorToJson(SensorData s) {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        sb.append("\"deviceId\":\"").append(escape(s.getDeviceId())).append('\"');
        sb.append(',');
        sb.append("\"timestamp\":").append(s.getTimestamp());
        sb.append(',');
        sb.append("\"value\":").append(s.getValue());
        sb.append('}');
        return sb.toString();
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
