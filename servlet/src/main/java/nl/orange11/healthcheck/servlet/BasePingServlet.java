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
 * <p>Your implementation must provide the executor to expose.</p>
 * <p>The servlet contains a mechanism that only one request at a time is actually going to the backend.</p>
 * <p/>
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
     * variables that are required to configure the PingExecutor.
     *
     * @return The PingExecutor to execute
     */
    protected abstract PingExecutor obtainExecutor(ServletConfig servletConfig);

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        level = PingLevel.valueOf(getParameter(config, PARAM_PINGLEVEL, DEFAULT_LEVEL.toString()));
        pingExecutor = obtainExecutor(config);
        pingResultReference.set(new PingResult("init", OK, "only for initialization"));
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        PingResult pingResult = obtainPingResult(level);

        int responseCode = obtainStatusOfResponse(pingResult);
        res.setStatus(responseCode);
        if ("application/json".equals(req.getHeader("Content-Type")) || "json".equals(req.getParameter("type"))) {
            writeJsonResponse(res, pingResult);
        } else {
            writeHtmlToResponse(res, pingResult);
        }

        closeHttpSession(req);
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
                .append("\"message\":\"").append(pingResult.getMessage()).append("\",");
        if (pingResult instanceof ThoroughPingResult) {
            sb.append("\"thoroughResults\" : {");
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
