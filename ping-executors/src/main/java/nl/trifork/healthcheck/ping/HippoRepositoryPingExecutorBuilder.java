package nl.trifork.healthcheck.ping;

/**
 * <p>Special builder class to create a {@link HippoRepositoryPingExecutor}. The builder contains a number of default
 * values. By using the special setter function you can override these defaults.</p>
 * <p>Start the creation by calling the create method. Override values with the setters and call build to actually
 * construct the object.</p>
 *
 * @author Jettro Coenradie
 */
public class HippoRepositoryPingExecutorBuilder {
    private String repositoryAddress = "rmi://localhost:1099/hipporepository";
    private String username = "admin";
    private String password = "admin";
    private String nodeToUse = "content/documents";
    private String writePath = "pingcheck";
    private String customMessage = null;

    public static HippoRepositoryPingExecutorBuilder create() {
        return new HippoRepositoryPingExecutorBuilder();
    }

    public HippoRepositoryPingExecutor build() {
        return new HippoRepositoryPingExecutor(repositoryAddress, username, password, nodeToUse, customMessage, writePath);
    }

    public HippoRepositoryPingExecutorBuilder setNodeToUse(String nodeToUse) {
        this.nodeToUse = nodeToUse;
        return this;
    }

    public HippoRepositoryPingExecutorBuilder setPassword(String password) {
        this.password = password;
        return this;
    }

    public HippoRepositoryPingExecutorBuilder setRepositoryAddress(String repositoryAddress) {
        this.repositoryAddress = repositoryAddress;
        return this;
    }

    public HippoRepositoryPingExecutorBuilder setUsername(String username) {
        this.username = username;
        return this;
    }

    public HippoRepositoryPingExecutorBuilder setWritePath(String writePath) {
        this.writePath = writePath;
        return this;
    }

    public HippoRepositoryPingExecutorBuilder setCustomMessage(String customMessage) {
        this.customMessage = customMessage;
        return this;
    }
}
