package nl.trifork.healthcheck.api;

import java.util.Map;

/**
 * Extension to the {@link PingResult}, now you can provided a map with key, value pairs of information to the client.
 *
 * @author Jettro Coenradie
 */
public class ThoroughPingResult extends PingResult {
    private Map<String, String> thoroughExtraValues;

    public ThoroughPingResult(PingResult pingResult, Map<String, String> thoroughExtraValues) {
        this(pingResult.getPingExecutorName(), pingResult.getSystemStatus(), pingResult.getMessage(), thoroughExtraValues);
    }

    public ThoroughPingResult(String name, SystemStatus systemStatus, String message, Map<String, String> thoroughExtraValues) {
        super(name, systemStatus, message);
        this.thoroughExtraValues = thoroughExtraValues;
    }

    public Map<String, String> getThoroughExtraValues() {
        return thoroughExtraValues;
    }
}
