package nl.orange11.healthcheck.api;

/**
 * <p>Result usually returned by the Basic and Extended ping executors. The information is enough to understand the state
 * of the system. The result contains the SystemStatus code as well as a description. You can also obtain the name of
 * the ping executor that was used to obtain the ping result.</p>
 *
 * @author Jettro Coenradie
 */
public class PingResult {
    private String pingExecutorName;
    private SystemStatus systemStatus;
    private String message;

    public PingResult(String name, SystemStatus systemStatus, String message) {
        this.pingExecutorName = name;
        this.systemStatus = systemStatus;
        this.message = message;
    }

    public String getPingExecutorName() {
        return pingExecutorName;
    }

    public String getMessage() {
        return message;
    }

    public SystemStatus getSystemStatus() {
        return systemStatus;
    }
}
