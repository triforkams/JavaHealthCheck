package nl.trifork.healthcheck.servlet;

import nl.trifork.healthcheck.api.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

/**
 * @author Jettro Coenradie
 */
public class BasePingServletMultiThreadTest {

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
                return new StubPingExecutor();
            }
        };

        when(mockServletConfig.getServletContext()).thenReturn(mockServletContext);
        when(mockServletContext.getInitParameter(BasePingServlet.PARAM_PINGLEVEL)).thenReturn(PingLevel.BASIC.name());

        basePingServlet.init(mockServletConfig);
    }

    @Test
    public void checkOnlyOneThreadExecutesPing() throws InterruptedException, ExecutionException {
        Callable<PingResult> task = createTask();
        List<Callable<PingResult>> tasks = Collections.nCopies(3, task);
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        List<Future<PingResult>> futures = executorService.invokeAll(tasks);

        assertEquals("We are fine.", futures.get(0).get().getMessage());
        assertEquals("only for initialization", futures.get(1).get().getMessage());
        assertEquals("only for initialization", futures.get(2).get().getMessage());
    }

    Callable<PingResult> createTask() {
        return new Callable<PingResult>() {
            @Override
            public PingResult call() throws Exception {
                return basePingServlet.obtainPingResult(PingLevel.BASIC);
            }
        };
    }


    public class StubPingExecutor implements PingExecutor {

        @Override
        public String getName() {
            return "Stub ping executor";
        }

        @Override
        public PingResult execute() {
            throw new UnsupportedOperationException();
        }

        @Override
        public PingResult executeExtended() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ThoroughPingResult executeThorough() {
            throw new UnsupportedOperationException();
        }

        @Override
        public PingResult execute(PingLevel pingLevel) {
            try {
                Thread.sleep(2000l);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return new PingResult("stub ping", SystemStatus.OK, "We are fine.");
        }
    }
}
