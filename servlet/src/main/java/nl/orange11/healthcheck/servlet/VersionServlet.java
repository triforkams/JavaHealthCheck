package nl.orange11.healthcheck.servlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;
import java.util.jar.Manifest;

/**
 * <p>Servlet for displaying the current version and build number of the application. These values are resolved from
 * the web application's manifest file located at <strong>{@value #MANIFEST_FILE_PATH}</strong>.</p>
 * <p>It is easy to use maven to create the manifest file using the maven-war-plugin. Below you will find an example configuration.</p>
 * <pre>
 * &lt;plugin&gt;
 *     &lt;groupId&gt;org.apache.maven.plugins&lt;/groupId&gt;
 * &lt;artifactId&gt;maven-war-plugin&lt;/artifactId&gt;
 * &lt;executions&gt;
 *  &lt;execution&gt;
 *      &lt;phase&gt;package&lt;/phase&gt;
 *      &lt;goals&gt;
 *          &lt;goal&gt;manifest&lt;/goal&gt;
 *      &lt;/goals&gt;
 *  &lt;/execution&gt;
 * &lt;/executions&gt;
 * &lt;configuration&gt;
 *  &lt;archive&gt;
 *      &lt;manifest&gt;
 *          &lt;addDefaultImplementationEntries&gt;true&lt;/addDefaultImplementationEntries&gt;
 *      &lt;/manifest&gt;
 *      &lt;manifestEntries&gt;
 *          &lt;Implementation-Version&gt;${project.version}&lt;/Implementation-Version&gt;
 *          &lt;Assembled-At&gt;${maven.build.timestamp}&lt;/Assembled-At&gt;
 *      &lt;/manifestEntries&gt;
 *  &lt;/archive&gt;
 * &lt;/configuration&gt;
 * &lt;/plugin&gt;
 * </pre>
 *
 * @author Jettro Coenradie
 */
public class VersionServlet extends HttpServlet {
    private static Logger logger = LoggerFactory.getLogger(VersionServlet.class);
    private static final String MANIFEST_FILE_PATH = "/META-INF/MANIFEST.MF";

    private Set<Map.Entry<Object, Object>> properties;


    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        properties = loadPropertiesFromManifest();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        PrintWriter out = response.getWriter();
        response.setContentType("text/html");

        out.write("<html><head><title>Application Build Properties</title></head>");
        out.write("<body>");
        out.write("<h2>Manifest file properties</h2>");
        out.write("<table>");
        for (Map.Entry<Object, Object> entry : properties) {
            out.write("<tr><td>");
            out.write(entry.getKey().toString());
            out.write("</td><td>");
            out.write(entry.getValue().toString());
            out.write("</td></tr>");
        }
        out.write("</table>");
        out.write("</body>");
        out.write("</html>");
    }

    /**
     * Loads all properties from the web application's manifest file.
     *
     * @return a <code>Set</code> containing the properties as found in the manifest file
     * @throws ServletException when a manifest file could not be read
     */
    private Set<Map.Entry<Object, Object>> loadPropertiesFromManifest() throws ServletException {
        try {
            InputStream stream = getServletContext().getResourceAsStream(MANIFEST_FILE_PATH);
            if (stream == null) {
                logger.error("Could not determine version and build number; file not found: '{}'", MANIFEST_FILE_PATH);
                return null;
            }
            Manifest manifest = new Manifest(stream);

            return manifest.getMainAttributes().entrySet();
        } catch (IOException ioe) {
            logger.error("Could not determine version and build number; Failed to read file '{}'", MANIFEST_FILE_PATH);
            return null;
        }
    }

}
