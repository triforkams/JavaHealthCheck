package nl.orange11.healthcheck.example;

import nl.orange11.healthcheck.api.*;
import nl.orange11.healthcheck.servlet.BasePingServlet;

import javax.servlet.ServletConfig;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Jettro Coenradie
 */
public class ExampleServlet extends BasePingServlet {

    @Override
    protected PingExecutor obtainExecutor(ServletConfig servletConfig) {
        return new PingExecutor() {
            public String getName() {
                return "Demo executor";
            }

            public PingResult execute() {
                return new PingResult(getName(), SystemStatus.OK, "This is the demo executor, always returns OK is basic mode.");
            }

            public ThoroughPingResult executeThorough() {
                return null;
            }

            public PingResult execute(PingLevel pingLevel) {
                switch (pingLevel) {
                    case BASIC:
                        return new PingResult(getName(), SystemStatus.OK, "Always returns OK when calling the basic version");
                    case EXTENDED:
                        return new PingResult(getName(), SystemStatus.ERROR, "Always returns ERROR when calling the EXTENDED version");
                    case THOROUGH:
                        Map<String, String> items = new HashMap<String, String>();
                        items.put("item1", "value1");
                        return new ThoroughPingResult(getName(), SystemStatus.ERROR, "Now you have even more stuff ro read", items);
                }
                return new PingResult(getName(), SystemStatus.ERROR, "Unexpected PingLevel");
            }
        };
    }
}
