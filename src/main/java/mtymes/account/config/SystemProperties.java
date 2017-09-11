package mtymes.account.config;

import java.time.Duration;

public class SystemProperties {

    private final int appPort;
    private final String dbHostName;
    private final int dbPort;
    private final String dbName;
    private final int backgroundWorkerCount;
    private final Duration workerIdleTimeout;

    public SystemProperties(int appPort, String dbHostName, int dbPort, String dbName, int backgroundWorkerCount, Duration workerIdleTimeout) {
        this.appPort = appPort;
        this.dbHostName = dbHostName;
        this.dbPort = dbPort;
        this.dbName = dbName;
        this.backgroundWorkerCount = backgroundWorkerCount;
        this.workerIdleTimeout = workerIdleTimeout;
    }

    public int appPort() {
        return appPort;
    }

    public String dbHostName() {
        return dbHostName;
    }

    public int dbPort() {
        return dbPort;
    }

    public String dbName() {
        return dbName;
    }

    public int backgroundWorkerCount() {
        return backgroundWorkerCount;
    }

    public Duration workerIdleTimeout() {
        return workerIdleTimeout;
    }
}
