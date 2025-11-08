package server;

import utils.Config;
import utils.LoggerUtil;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.Instant;

public class AlertSystem {
    public void checkAndAlert(String deviceId, double value) {
        if (value >= Config.ALERT_THRESHOLD) {
            String msg = String.format("[%s] ALERT: %s value=%.3f", Instant.now().toString(), deviceId, value);
            LoggerUtil.error(msg);
            // append to data/alerts.log relative to project root
            try (FileWriter fw = new FileWriter("data/alerts.log", true);
                 PrintWriter pw = new PrintWriter(fw)) {
                pw.println(msg);
            } catch (Exception e) {
                LoggerUtil.error("Failed to write alert: " + e.getMessage());
            }
        }
    }
}
