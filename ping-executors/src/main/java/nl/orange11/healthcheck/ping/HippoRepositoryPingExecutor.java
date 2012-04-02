package nl.orange11.healthcheck.ping;

import nl.orange11.healthcheck.api.*;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.*;
import java.util.Calendar;
import java.util.HashMap;

/**
 * <p>Implementation for the {@link PingExecutor} that connects to a hippo repository. The PingExecutor interface specifies
 * multiple levels of ping.</p>
 * <ul>
 * <li>Basic - Tries to connect to a repository by reading a node.</li>
 * <li>Extended - Tries to connect to a repository by reading from a node and writing to a node.</li>
 * <li>Thorough - The same as the extended check.</li>
 * </ul>
 * <p>A special builder is available {@link HippoRepositoryPingExecutorBuilder} to make creating the executor easier.</p>
 * <p>A lot of the code is based on the PingServlet as provided by Hippo.</p>
 *
 * @author Jettro Coenradie
 */
public class HippoRepositoryPingExecutor implements PingExecutor {
    private static final Logger logger = LoggerFactory.getLogger(HippoRepositoryPingExecutor.class);

    private String repositoryLocation;
    private String username;
    private String password;
    private String checkNode;
    private String customMessage;
    private String writeTestPath;

    public HippoRepositoryPingExecutor(String repositoryLocation, String username, String password, String checkNode,
                                       String customMessage, String writeTestPath) {
        this.repositoryLocation = repositoryLocation;
        this.username = username;
        this.password = password;
        this.checkNode = checkNode;
        this.customMessage = customMessage;
        this.writeTestPath = writeTestPath;
    }

    @Override
    public String getName() {
        return "Hippo repository ping executor";
    }

    @Override
    public PingResult execute() {
        return execute(PingLevel.BASIC);
    }

    @Override
    public PingResult executeExtended() {
        return execute(PingLevel.EXTENDED);
    }

    @Override
    public ThoroughPingResult executeThorough() {
        PingResult pingResult = execute(PingLevel.THOROUGH);
        return new ThoroughPingResult(pingResult.getPingExecutorName(), pingResult.getSystemStatus(), pingResult.getMessage(),
                new HashMap<String, String>());
    }

    @Override
    public PingResult execute(PingLevel pingLevel) {
        SystemStatus status = SystemStatus.OK;
        String resultMessage = "OK - Repository online and accessible.";
        if (hasCustomMessage()) {
            resultMessage = customMessage;
            status = SystemStatus.TIMEOUT_ERROR;
        } else {
            try {
                doRepositoryChecks(pingLevel);
            } catch (HippoPingException e) {
                status = e.getProposedStatus();
                resultMessage = e.getMessage();
                logger.error("Problem while executing a hippo connection ping.", e);
            } catch (RuntimeException e) {
                status = SystemStatus.ERROR;
                resultMessage = "FAILURE - Serious problem with the ping servlet. Might have lost repository access: "
                        + e.getClass().getName() + ": " + e.getMessage();
                logger.error("Unknown problem while executing a hippo connection ping.", e);
            }

        }
        return new PingResult(getName(), status, resultMessage);
    }

    /**
     * This method determines which checks to execute based on the provided PingLevel.
     *
     * @param pingLevel PingLevel determining the checks to perform
     * @throws HippoPingException thrown by the called functions when something goes wrong.
     */
    void doRepositoryChecks(PingLevel pingLevel) throws HippoPingException {
        Session session = null;
        try {
            HippoRepository repository = obtainRepository();
            session = obtainSession(repository);
            doReadTest(session);
            if (pingLevel != PingLevel.BASIC) {
                doWriteTest(session);
            }
        } finally {
            closeSession(session);
        }
    }

    /**
     * Return the available repository based on the configured location.
     *
     * @return HippoRepository connected to the configured url.
     * @throws HippoPingException thrown if obtaining the repository is not possible
     */
    HippoRepository obtainRepository() throws HippoPingException {
        HippoRepository repository;
        try {
            repository = HippoRepositoryFactory.getHippoRepository(repositoryLocation);
        } catch (RepositoryException e) {
            String msg = "FAILURE - Problem obtaining repository connection in executor.";
            throw new HippoPingException(msg, e, SystemStatus.ERROR);
        }
        return repository;
    }

    /**
     * Returns a new session obtained through the provided repository and the configured username and password.
     *
     * @param repository HippoRepository to obtain a Session to.
     * @return Session connected to the provided repository.
     * @throws HippoPingException thrown if the credentials are wrong or if we cannot obtain a session to the repository
     */
    Session obtainSession(HippoRepository repository) throws HippoPingException {
        try {
            return repository.login(username, password.toCharArray());
        } catch (LoginException e) {
            String msg = "FAILURE - Wrong credentials for obtaining session from repository in ping executor." +
                    "This might be a configuration problem with the username and password.";
            throw new HippoPingException(msg, e, SystemStatus.AUTHENTICATION_ERROR);
        } catch (RepositoryException e) {
            String msg = "FAILURE - Problem obtaining session from repository in ping executor." +
                    "This might be a configuration problem with the username and password.";
            throw new HippoPingException(msg, e, SystemStatus.AUTHENTICATION_ERROR);
        }
    }

    /**
     * The actual read test obtains the configured path from the Root node. If no path is configured, the Root node will
     * be obtained.
     *
     * @param session Session to obtain the Root node from.
     * @throws HippoPingException Thrown if the configured path could not be found or if another repository problem occurs
     */
    void doReadTest(Session session) throws HippoPingException {
        String msg;
        try {
            if (checkNode.length() == 0) {
                session.getRootNode();
            } else {
                session.getRootNode().getNode(checkNode);
            }
        } catch (PathNotFoundException e) {
            msg = "FAILURE - Path for node to lookup '" + checkNode + "' is not found by ping executor. ";
            throw new HippoPingException(msg, e, SystemStatus.ERROR);
        } catch (RepositoryException e) {
            msg = "FAILURE - Could not obtain a node, which is at this point unexpected since we already have a connection."
                    + "Maybe we lost the connection to the repository.";
            throw new HippoPingException(msg, e, SystemStatus.ERROR);
        }
    }

    /**
     * The actual write test obtains the configured node and writes the current date in the node for the cluster node.
     *
     * @param session Session to use for writing the node.
     * @throws HippoPingException Thrown if we could not write to the repository.
     */
    void doWriteTest(Session session) throws HippoPingException {
        try {
            Node writePath = getOrCreateWriteNode(session);
            writePath.setProperty("lastcheck", Calendar.getInstance());
            writePath.save();
        } catch (RepositoryException e) {
            String msg = "FAILURE - Error during write test. There could be an issue with the (connection to) the storage.";
            throw new HippoPingException(msg, e, SystemStatus.ERROR);
        }
    }

    private boolean hasCustomMessage() {
        return (customMessage != null);
    }

    private void closeSession(Session session) {
        if (session != null && session.isLive()) {
            session.logout();
        }
    }

    private Node getOrCreateWriteNode(Session session) throws HippoPingException {
        Node path = getOrCreateWritePath(session);
        String clusterId = getClusterNodeId();
        try {
            if (path.hasNode(clusterId)) {
                return path.getNode(clusterId);
            } else {
                Node node = path.addNode(clusterId);
                session.save();
                return node;
            }
        } catch (RepositoryException e) {
            String msg = "FAILURE - Could not obtain the write test node '" + writeTestPath + "/" + clusterId + "'.";
            throw new HippoPingException(msg, e, SystemStatus.ERROR);
        }
    }

    private Node getOrCreateWritePath(Session session) throws HippoPingException {
        Node path;
        try {
            if (session.getRootNode().hasNode(writeTestPath)) {
                path = session.getRootNode().getNode(writeTestPath);
            } else {
                path = session.getRootNode().addNode(writeTestPath);
                session.save();
            }
            return path;
        } catch (RepositoryException e) {
            String msg = "FAILURE - Could not obtain the write path node '" + writeTestPath + "'.";
            throw new HippoPingException(msg, e, SystemStatus.ERROR);
        }
    }

    private String getClusterNodeId() {
        String id = System.getProperty("org.apache.jackrabbit.core.cluster.node_id");
        if (id == null || id.length() == 0) {
            return "default";
        }
        return id;
    }

}
