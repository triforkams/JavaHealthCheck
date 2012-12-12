package nl.trifork.healthcheck.ping.hippo;

import nl.trifork.healthcheck.api.SystemStatus;

/**
 * @author Jettro Coenradie
 */
public class HippoPingException extends RuntimeException {
    private SystemStatus proposedStatus;

    public HippoPingException(String s, SystemStatus proposedStatus) {
        super(s);
        this.proposedStatus = proposedStatus;
    }

    public HippoPingException(String s, Throwable throwable, SystemStatus proposedStatus) {
        super(s, throwable);
        this.proposedStatus = proposedStatus;
    }

    public SystemStatus getProposedStatus() {
        return proposedStatus;
    }
}
