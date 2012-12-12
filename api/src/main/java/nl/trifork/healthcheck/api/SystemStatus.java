package nl.trifork.healthcheck.api;

/**
 * The different states the server ping can return. The ping can return OK, an Error and some more specific errors. The
 * final status is the maintenance status.
 *
 * It is also possible to give back a warning, which means the ping is ok, but their might still be something that
 * needs attention.
 *
 * @author Jettro Coenradie
 */
public enum SystemStatus {
    OK, WARNING, ERROR, AUTHENTICATION_ERROR, TIMEOUT_ERROR, MAINTENANCE
}
