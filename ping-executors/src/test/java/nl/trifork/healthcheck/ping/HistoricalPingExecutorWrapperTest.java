package nl.trifork.healthcheck.ping;

import nl.trifork.healthcheck.api.PingExecutor;
import nl.trifork.healthcheck.api.PingLevel;
import nl.trifork.healthcheck.api.PingResult;
import nl.trifork.healthcheck.api.SystemStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author Jettro Coenradie
 */
public class HistoricalPingExecutorWrapperTest {

    @Mock
    PingExecutor delegate;

    HistoricalPingExecutorWrapper wrapper;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        wrapper = new HistoricalPingExecutorWrapper(delegate,10);
    }

    @Test
    public void testAdditems() throws Exception {
        when(delegate.execute(PingLevel.BASIC)).thenReturn(new PingResult("mock", SystemStatus.OK,"nothing"));
        PingResult result = wrapper.execute();

        assertNotNull(result);
        assertEquals(SystemStatus.OK,result.getSystemStatus());

        PingResult[] items = wrapper.getItems();

        assertEquals(1,items.length);
    }

    @Test
    public void testAdditems_moreThanMax() throws Exception {
        when(delegate.execute(PingLevel.BASIC)).thenReturn(new PingResult("mock", SystemStatus.OK,"nothing"));
        for (int i=0;i < 15; i++) {
            wrapper.execute();
        }
        PingResult[] items = wrapper.getItems();

        assertEquals(10,items.length);
    }

    @Test
    public void testAdditems_withErrors() throws Exception {
        when(delegate.execute(PingLevel.EXTENDED)).thenReturn(new PingResult("mock", SystemStatus.ERROR,"an error"));
        when(delegate.execute(PingLevel.BASIC)).thenReturn(new PingResult("mock", SystemStatus.OK,"nothing special"));

        PingResult result = wrapper.executeExtended();
        for (int i=0;i < 9; i++) {
            result = wrapper.execute();
        }
        PingResult[] items = wrapper.getItems();

        assertEquals(10,items.length);
        assertNotNull(result);
        assertEquals(SystemStatus.WARNING,result.getSystemStatus());
        assertEquals("There was an error percentage of 10 in the last runs: an error",result.getMessage());
    }

    @Test
    public void testAdditems_onlyErrors() throws Exception {
        when(delegate.execute(PingLevel.BASIC)).thenReturn(new PingResult("mock", SystemStatus.ERROR,"an error"));
        PingResult result = null;
        for (int i=0;i < 15; i++) {
            result = wrapper.execute();
        }
        PingResult[] items = wrapper.getItems();

        assertEquals(10,items.length);
        assertNotNull(result);
        assertEquals(SystemStatus.ERROR,result.getSystemStatus());
        assertEquals("an error",result.getMessage());
    }



}
