package nl.orange11.healthcheck.servlet;

import nl.orange11.healthcheck.api.PingExecutor;
import nl.orange11.healthcheck.api.PingResult;
import nl.orange11.healthcheck.api.SystemStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

/**
 * @author Jettro Coenradie
 */
public class BasePingServletTest {
    @Mock
    PingExecutor mockPingExecutor;
    @Mock
    ServletConfig mockServletConfig;
    @Mock
    ServletContext mockServletContext;

    BasePingServlet basePingServlet;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        basePingServlet = new BasePingServlet() {
            @Override
            protected PingExecutor obtainExecutor(ServletConfig servletConfig) {
                return mockPingExecutor;
            }
        };
    }

    @Test
    public void testInit() throws Exception {
        when(mockServletConfig.getInitParameter("pinglevel")).thenReturn("BASIC");
        when(mockServletConfig.getServletContext()).thenReturn(mockServletContext);
        when(mockServletContext.getInitParameter("pinglevel")).thenReturn(null);

        basePingServlet.init(mockServletConfig);
    }

    @Test
    public void testObtainStatusOfResponse() throws Exception {
        verifyStatus(200, SystemStatus.OK);
        verifyStatus(500, SystemStatus.ERROR);
        verifyStatus(401, SystemStatus.AUTHENTICATION_ERROR);
        verifyStatus(503, SystemStatus.TIMEOUT_ERROR);
    }

    private void verifyStatus(int expected, SystemStatus status) {
        assertEquals(expected, basePingServlet.obtainStatusOfResponse(new PingResult("stub", status, "The message")));
    }
}
