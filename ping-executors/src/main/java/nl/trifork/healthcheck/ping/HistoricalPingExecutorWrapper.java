package nl.trifork.healthcheck.ping;

import nl.trifork.healthcheck.api.PingExecutor;
import nl.trifork.healthcheck.api.PingLevel;
import nl.trifork.healthcheck.api.PingResult;
import nl.trifork.healthcheck.api.SystemStatus;

import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * This wrapper for {@link PingExecutor}s keeps track of the previous results of the ping executors.
 *
 * @author Jettro Coenradie
 */
public class HistoricalPingExecutorWrapper extends PingExecutorAdapter implements PingExecutor {
    private PingExecutor delegate;
    private int numberOfItemsToKeep;
    private Deque<PingResult> items;

    public HistoricalPingExecutorWrapper(PingExecutor delegate, int numberOfItemsToKeep) {
        this.delegate = delegate;
        this.numberOfItemsToKeep = numberOfItemsToKeep;
        items = new LinkedList<PingResult>();
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    public PingResult[] getItems() {
        return items.toArray(new PingResult[items.size()]);
    }

    @Override
    public PingResult execute(PingLevel pingLevel) {
        PingResult result = delegate.execute(pingLevel);

        while (items.size() >= numberOfItemsToKeep) {
            items.pollLast();
        }
        items.addFirst(result);

        if (result.getSystemStatus() == SystemStatus.ERROR) {
            return result;
        }

        int numErrors = 0;
        String lastErrorMessage = "";
        for (PingResult pingResult : items) {
            if (pingResult.getSystemStatus() != SystemStatus.OK) {
                numErrors += 1;
                lastErrorMessage = pingResult.getMessage();
            }
        }

        if (numErrors > 0) {
            int errorPercentage = (numErrors * 100) / numberOfItemsToKeep;
            String message = "There was an error percentage of " + errorPercentage + " in the last runs: " + lastErrorMessage;
            return new PingResult(getName(), SystemStatus.WARNING, message);
        } else {
            return result;
        }
    }
}
