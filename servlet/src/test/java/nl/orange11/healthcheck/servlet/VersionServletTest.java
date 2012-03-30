package nl.orange11.healthcheck.servlet;

import org.junit.Test;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Jettro Coenradie
 */
public class VersionServletTest {
    @Test
    public void loadManifest() throws ServletException, IOException, InterruptedException {
        ServletConfig config = mock(ServletConfig.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        ServletContext context = mock(ServletContext.class);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        PrintWriter output = new PrintWriter(outputStream, true);

        when(response.getWriter()).thenReturn(output);
        when(config.getServletContext()).thenReturn(context);

        String theManifestFile = "Manifest-Version: 1.0\n" +
                "Archiver-Version: Plexus Archiver\n" +
                "Build-Jdk: 1.6.0_29\n" +
                "Implementation-Title: site - war\n" +
                "Assembled-At: 2011/12/05 09:48:30\n" +
                "Implementation-Build: 124009\n" +
                "Implementation-Version: 2011.14-build-1-SNAPSHOT\n";

        ByteArrayInputStream value = new ByteArrayInputStream(theManifestFile.getBytes("UTF-8"));
        when(context.getResourceAsStream(isA(String.class))).thenReturn(value);

        VersionServlet servlet = new VersionServlet();
        servlet.init(config);
        servlet.service(request, response);
        output.flush();

        String result = outputStream.toString("UTF-8");
        assertEquals("<html><head><title>Application Build Properties</title></head><body><h2>Manifest file properties</h2><table><tr><td>Implementation-Title</td><td>site - war</td></tr><tr><td>Implementation-Version</td><td>2011.14-build-1-SNAPSHOT</td></tr><tr><td>Build-Jdk</td><td>1.6.0_29</td></tr><tr><td>Manifest-Version</td><td>1.0</td></tr><tr><td>Implementation-Build</td><td>124009</td></tr><tr><td>Assembled-At</td><td>2011/12/05 09:48:30</td></tr><tr><td>Archiver-Version</td><td>Plexus Archiver</td></tr></table></body></html>"
                , result);
    }

}
