package nl.orange11.healthcheck.servlet;

import nl.orange11.healthcheck.api.PingExecutor;
import nl.orange11.healthcheck.api.PingLevel;
import nl.orange11.healthcheck.api.PingResult;
import nl.orange11.healthcheck.api.ThoroughPingResult;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

import static nl.orange11.healthcheck.api.SystemStatus.OK;

/**
 * <p>Abstract base class for your servlet exposing a ping executor. Based on the requested content type, the servlet
 * can return json or html. If you need json, provide <em>application/json</em> as the requested content type or add
 * <em>?type=json</em> to your request. In all other cases, html is returned.</p>
 * <p>Your implementation must provide the executor to expose. And provide the init-param pinglevel if you need
 * another level than the BASIC level for your ping. The following code block shows some example configuration to do that.</p>
 * <pre>
 * &lt;servlet&gt;
 *     &lt;servlet-name&gt;PingServlet&lt;/servlet-name&gt;
 *     &lt;servlet-class&gt;nl.orange11.healthcheck.example.ExampleServlet&lt;/servlet-class&gt;
 *     &lt;init-param&gt;
 *         &lt;param-name&gt;pinglevel&lt;/param-name&gt;
 *         &lt;param-value&gt;THOROUGH&lt;/param-value&gt;
 *     &lt;/init-param&gt;
 * &lt;/servlet&gt;
 * </pre>
 * <p>You can also provide the requested ping level in a request. This takes precedence over the servlet init param.
 * Valid options are the text representations of the {@link PingLevel} items. Use the parameter
 * <strong>{@value BasePingServlet#PARAM_PINGLEVEL}</strong> to provide your own value. You can also pass a number from
 * 1 to and including 3 that we translate into the three levels of ping. 1 being Basic, 2 extended and 3 Thorough.</p>
 * <p>The servlet contains a mechanism that only one request at a time is actually going to the backend.</p>
 *
 * @author Jettro Coenradie
 */
public abstract class BasePingServlet extends HttpServlet {
    private static final String PARAM_PINGLEVEL = "pinglevel";
    private static final PingLevel DEFAULT_LEVEL = PingLevel.BASIC;

    private PingExecutor pingExecutor;
    private PingLevel level;

    // Binary semaphore, only one thread can have acquire access
    private final Semaphore pingResultSemaphore = new Semaphore(1);
    private final AtomicReference<PingResult> pingResultReference = new AtomicReference<PingResult>();

    /**
     * Returns the {@link PingExecutor} to execute. The ServletConfig is provided to the subclass to be able to obtain
     * variables that are required to configure the PingExecutor. When we cannot obtain the executor in the right state
     * a ServletException is thrown.
     *
     * @return The PingExecutor to execute
     * @throws ServletException when an errors occurs obtaining the executor this exception is thrown.
     */
    protected abstract PingExecutor obtainExecutor(ServletConfig servletConfig) throws ServletException;

    /**
     * Reeds the ping level and obtains the ping executor from the subclass.
     *
     * @param config ServletConfig used to read the init parameters from.
     * @throws ServletException Exception thrown when interacting with ServletConfig
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        level = PingLevel.valueOf(getParameter(config, PARAM_PINGLEVEL, DEFAULT_LEVEL.toString()));
        pingExecutor = obtainExecutor(config);
        pingResultReference.set(new PingResult("init", OK, "only for initialization"));
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String reqPingLevel = req.getParameter(PARAM_PINGLEVEL);
        PingLevel pingLevel = obtainPingLevel(reqPingLevel, level);

        PingResult pingResult = obtainPingResult(pingLevel);

        int responseCode = obtainStatusOfResponse(pingResult);
        res.setStatus(responseCode);
        if ("application/json".equals(req.getHeader("Content-Type")) || "json".equals(req.getParameter("type"))) {
            writeJsonResponse(res, pingResult);
        } else {
            writeHtmlToResponse(res, pingResult);
        }

        closeHttpSession(req);
    }

    /**
     * Returns the PingLevel that belongs to the requestedPingLevel. If the requested ping level does not result in a
     * valid PingLevel, we return the provided defaultLevel. You can provide the name of the item in the enum PingLevel
     * or a number representing the items in the order of the enum. The enum consists of three items, therefore the
     * numeric value can be 1, 2 or 3.
     *
     * @param requestedPingLevel String representation of the PingLevel.
     * @param defaultLevel       Default PingLevel to return in case problems
     * @return PingLevel found for the provided requestedPingLevel or the provided defaultPingLevel
     */
    PingLevel obtainPingLevel(String requestedPingLevel, PingLevel defaultLevel) {
        if (requestedPingLevel == null || "".equals(requestedPingLevel)) {
            return defaultLevel;
        }

        PingLevel pingLevel;
        if (requestedPingLevel.matches("[1-3]")) {
            pingLevel = PingLevel.values()[Integer.parseInt(requestedPingLevel) - 1];
        } else {
            try {
                pingLevel = PingLevel.valueOf(requestedPingLevel);
            } catch (IllegalArgumentException e) {
                pingLevel = defaultLevel;
            }
        }
        return pingLevel;
    }

    /*
     *  UTILITY FUNCTIONS
     */

    /**
     * Returns an int representing a {@link HttpServletResponse} value.
     *
     * @param pingResult The obtained ping result that contains the information to determine the response code
     * @return int representing the HttpServletResponse code
     */
    int obtainStatusOfResponse(PingResult pingResult) {
        int responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        switch (pingResult.getSystemStatus()) {
            case OK:
                responseCode = HttpServletResponse.SC_OK;
                break;
            case ERROR:
                responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
                break;
            case AUTHENTICATION_ERROR:
                responseCode = HttpServletResponse.SC_UNAUTHORIZED;
                break;
            case TIMEOUT_ERROR:
                responseCode = HttpServletResponse.SC_SERVICE_UNAVAILABLE;
                break;
            case MAINTENANCE:
                responseCode = HttpServletResponse.SC_SERVICE_UNAVAILABLE;
                break;
        }
        return responseCode;
    }

    /**
     * Writes the PingResult as html back to the response object in html format.
     *
     * @param res        The HttpServletResponse object to write the html content to
     * @param pingResult PingResult containing the information to write to the response.
     * @throws IOException Thrown if writing to the response goes wrong.
     */
    void writeHtmlToResponse(HttpServletResponse res, PingResult pingResult) throws IOException {
        res.setContentType("text/html");
        PrintWriter writer = res.getWriter();
        writer.println("<html><head><title>Ping Result</title></head><body><h1>Ping Result</h1>");
        writer.println("<h2>" + pingResult.getPingExecutorName() + "</h2>");
        writer.println("<p>" + pingResult.getMessage() + "</p>");
        if (pingResult instanceof ThoroughPingResult) {
            writer.println("<table><thead><tr><th>Key</th><th>Value</th></tr></thead><tbody>");
            ThoroughPingResult thoroughPingResult = (ThoroughPingResult) pingResult;
            Map<String, String> thoroughExtraValues = thoroughPingResult.getThoroughExtraValues();
            for (String key : thoroughExtraValues.keySet()) {
                writer.println("<tr><td>" + key + "</td><td>" + thoroughExtraValues.get(key) + "</td></tr>");
            }
            writer.println("</tbody></table>");
        }
        writer.println("</body></html>");
    }

    /**
     * Writes the results of the PingExecutor to the response object in JSON format.
     *
     * @param res        The HttpServletResponse object to write the json content to
     * @param pingResult PingResult containing the information to write to the response.
     * @throws IOException Thrown if writing to the response goed wrong
     */
    void writeJsonResponse(HttpServletResponse res, PingResult pingResult) throws IOException {
        res.setContentType("application/json");
        PrintWriter writer = res.getWriter();
        StringBuilder sb = new StringBuilder();
        sb.append("{")
                .append("\"executorName\":\"").append(pingResult.getPingExecutorName()).append("\",")
                .append("\"message\":\"").append(pingResult.getMessage()).append("\"");
        if (pingResult instanceof ThoroughPingResult) {
            sb.append(",\"thoroughResults\" : {");
            ThoroughPingResult thoroughPingResult = (ThoroughPingResult) pingResult;
            Map<String, String> thoroughExtraValues = thoroughPingResult.getThoroughExtraValues();
            boolean firstItem = true;
            for (String key : thoroughExtraValues.keySet()) {
                if (!firstItem) {
                    sb.append(",");
                } else {
                    firstItem = false;
                }
                sb.append("\"").append(key).append("\":\"").append(thoroughExtraValues.get(key)).append("\"");
            }
            sb.append("}");
        }
        sb.append("}");
        writer.println(sb.toString());
    }

    /**
     * Makes use of the ping executor that is provided by the subclass. This method returns when another thread is updating
     * the ping response. In that case it returns the old response.
     *
     * @return PingResult as obtained using the ping executor
     */
    PingResult obtainPingResult(PingLevel level) {
        PingResult result;
        if (pingResultSemaphore.tryAcquire()) {
            try {
                result = pingExecutor.execute(level);
                if (result != null) {
                    pingResultReference.set(result);
                }
            } finally {
                pingResultSemaphore.release();
            }
        }
        return pingResultReference.get();
    }

    /**
     * Obtains the parameter from the servlet init params or from the servlet context params if the init param is empty.
     * If both are empty, the default value is returned.
     *
     * @param config       ServletConfig to obtain the parameter values from.
     * @param paramName    String containing the name of the parameter to obtain.
     * @param defaultValue The default value for the parameter. If no value could be found, this value is used.
     * @return String containing the value for the provided parameter.
     */
    String getParameter(ServletConfig config, String paramName, String defaultValue) {
        String initValue = config.getInitParameter(paramName);
        String contextValue = config.getServletContext().getInitParameter(paramName);

        if (isNotNullAndNotEmpty(initValue)) {
            return initValue;
        } else if (isNotNullAndNotEmpty(contextValue)) {
            return contextValue;
        } else {
            return defaultValue;
        }
    }

    /**
     * Checks if the provided string is not null nor empty.
     *
     * @param s String to check
     * @return True if the string has content, false otherwise
     */
    boolean isNotNullAndNotEmpty(String s) {
        return (s != null && s.length() != 0);
    }

    /**
     * Closes any available session.
     *
     * @param req HttpServletRequest to obtain the session from.
     */
    void closeHttpSession(HttpServletRequest req) {
        if (req != null) {
            // close open session
            HttpSession httpSession = req.getSession(false);
            if (httpSession != null) {
                httpSession.invalidate();
            }
        }
    }

}
