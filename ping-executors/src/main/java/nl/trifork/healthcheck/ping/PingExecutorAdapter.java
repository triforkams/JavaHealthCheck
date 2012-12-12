package nl.trifork.healthcheck.ping;

import nl.trifork.healthcheck.api.PingExecutor;
import nl.trifork.healthcheck.api.PingLevel;
import nl.trifork.healthcheck.api.PingResult;
import nl.trifork.healthcheck.api.ThoroughPingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

/**
 * @author Jettro Coenradie
 */
public abstract class PingExecutorAdapter implements PingExecutor {
    private static final Logger logger = LoggerFactory.getLogger(PingExecutorAdapter.class);

    @Override public PingResult execute() {
        logger.debug("Execute the basic ping.");
        return execute(PingLevel.BASIC);
    }

    @Override
    public PingResult executeExtended() {
        logger.debug("Execute the extended ping.");
        return execute(PingLevel.EXTENDED);
    }

    @Override
    public ThoroughPingResult executeThorough() {
        logger.debug("Execute the thorough ping.");
        PingResult pingResult = execute(PingLevel.THOROUGH);
        return new ThoroughPingResult(pingResult.getPingExecutorName(), pingResult.getSystemStatus(), pingResult.getMessage(),
                new HashMap<String, String>());
    }

}
