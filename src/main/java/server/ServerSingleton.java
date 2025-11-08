package server;

/**
 * Small holder for server-wide singletons (storage, alert system).
 */
public class ServerSingleton {
    private static final DataStorage STORAGE = new DataStorage();
    private static final AlertSystem ALERT = new AlertSystem();

    public static DataStorage getStorage() { return STORAGE; }
    public static AlertSystem getAlertSystem() { return ALERT; }
}
