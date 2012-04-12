package nl.orange11.healthcheck.ping;

import nl.orange11.healthcheck.api.PingLevel;
import nl.orange11.healthcheck.api.PingResult;
import nl.orange11.healthcheck.api.SystemStatus;
import nl.orange11.healthcheck.api.ThoroughPingResult;
import org.hippoecm.repository.HippoRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;
import java.util.Calendar;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Jettro Coenradie
 */
public class HippoRepositoryPingExecutorTest {
    public static final String DOCUMENT_PATH = "content/documents";
    HippoRepositoryPingExecutor spyExecutor;

    @Mock
    HippoRepository hippoRepository;
    @Mock
    Session session;
    @Mock
    Node rootNode;
    @Mock
    Node documentNode;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        HippoRepositoryPingExecutor pingExecutor = HippoRepositoryPingExecutorBuilder.create().build();
        spyExecutor = spy(pingExecutor);

        when(session.getRootNode()).thenReturn(rootNode);
    }

    @Test
    public void testName() throws Exception {
        assertEquals("Hippo repository ping executor", spyExecutor.getName());
    }

    @Test
    public void testExecute() throws Exception {
        setupExecuterTest();

        PingResult pingResult = spyExecutor.execute();

        assertNotNull(pingResult);
        assertEquals(SystemStatus.OK, pingResult.getSystemStatus());

        verify(spyExecutor).doReadTest(session);
        verify(spyExecutor, never()).doWriteTest(session);
    }

    @Test
    public void testExecuteThorough() throws Exception {
        setupExecuterTest();

        ThoroughPingResult thoroughPingResult = spyExecutor.executeThorough();

        assertNotNull(thoroughPingResult);
        assertEquals(SystemStatus.OK, thoroughPingResult.getSystemStatus());

        verify(spyExecutor).doReadTest(session);
        verify(spyExecutor).doWriteTest(session);
    }

    @Test
    public void testExecuteExtended() throws Exception {
        setupExecuterTest();

        PingResult pingResult = spyExecutor.executeExtended();

        assertNotNull(pingResult);
        assertEquals(SystemStatus.OK, pingResult.getSystemStatus());

        verify(spyExecutor).doReadTest(session);
        verify(spyExecutor).doWriteTest(session);
    }

    private void setupExecuterTest() {
        doReturn(hippoRepository).when(spyExecutor).obtainRepository();
        doReturn(session).when(spyExecutor).obtainSession(hippoRepository);
        doNothing().when(spyExecutor).doReadTest(session);
        doNothing().when(spyExecutor).doWriteTest(session);
    }

    @Test
    public void testExecute_throwHippoPingException() throws Exception {
        doReturn(hippoRepository).when(spyExecutor).obtainRepository();
        doReturn(session).when(spyExecutor).obtainSession(hippoRepository);
        doThrow(new HippoPingException("for testing only", SystemStatus.AUTHENTICATION_ERROR)).when(spyExecutor).doRepositoryChecks(PingLevel.BASIC);

        PingResult pingResult = spyExecutor.execute(PingLevel.BASIC);

        assertNotNull(pingResult);
        assertEquals(SystemStatus.AUTHENTICATION_ERROR, pingResult.getSystemStatus());
    }

    @Test
    public void testExecute_throwRuntimeException() throws Exception {
        doReturn(hippoRepository).when(spyExecutor).obtainRepository();
        doReturn(session).when(spyExecutor).obtainSession(hippoRepository);
        doThrow(new RuntimeException("for testing only")).when(spyExecutor).doRepositoryChecks(PingLevel.BASIC);

        PingResult pingResult = spyExecutor.execute(PingLevel.BASIC);

        assertNotNull(pingResult);
        assertEquals(SystemStatus.ERROR, pingResult.getSystemStatus());
    }

    @Test
    public void testExecute_customMessage() throws Exception {
        HippoRepositoryPingExecutor pingExecutor = HippoRepositoryPingExecutorBuilder.create().setCustomMessage("Custom message for maintenance mode.").build();
        spyExecutor = spy(pingExecutor);

        PingResult pingResult = spyExecutor.execute();

        assertNotNull(pingResult);
        assertEquals(SystemStatus.MAINTENANCE, pingResult.getSystemStatus());
        assertEquals("Custom message for maintenance mode.", pingResult.getMessage());
    }

    @Test
    public void testReadTest() throws Exception {
        when(rootNode.getNode(DOCUMENT_PATH)).thenReturn(documentNode);

        spyExecutor.doReadTest(session);

        verify(rootNode).getNode(DOCUMENT_PATH);
    }

    @Test(expected = HippoPingException.class)
    public void testReadTest_throwException() throws Exception {
        when(rootNode.getNode(DOCUMENT_PATH)).thenThrow(new PathNotFoundException());

        spyExecutor.doReadTest(session);
    }

    @Test
    public void testWriteTest() throws Exception {
        System.setProperty("org.apache.jackrabbit.core.cluster.node_id", "testnode");
        Node writeNode = mock(Node.class);
        Node clusterNode = mock(Node.class);
        when(rootNode.hasNode("pingcheck")).thenReturn(true);
        when(rootNode.getNode("pingcheck")).thenReturn(writeNode);
        when(writeNode.hasNode("testnode")).thenReturn(true);
        when(writeNode.getNode("testnode")).thenReturn(clusterNode);

        spyExecutor.doWriteTest(session);

        verify(clusterNode).setProperty(eq("lastcheck"), isA(Calendar.class));
        verify(clusterNode).save();
    }

    @Test
    public void testObtainSession() throws Exception {
        when(hippoRepository.login("admin", "admin".toCharArray())).thenReturn(session);

        Session obtainedSession = spyExecutor.obtainSession(hippoRepository);

        assertSame(session, obtainedSession);
    }

    @Test(expected = HippoPingException.class)
    public void testObtainSession_loginProblem() throws Exception {
        when(hippoRepository.login("admin", "admin".toCharArray())).thenThrow(new LoginException());

        spyExecutor.obtainSession(hippoRepository);
    }

}
