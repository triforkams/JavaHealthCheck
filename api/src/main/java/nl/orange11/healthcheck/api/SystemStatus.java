package nl.orange11.healthcheck.api;

/**
 * The different states the server ping can return. The ping can return OK, an Error and some more specific errors. The
 * final status is the maintenance status.
 *
 * @author Jettro Coenradie
 */
public enum SystemStatus {
    OK, ERROR, AUTHENTICATION_ERROR, TIMEOUT_ERROR, MAINTENANCE
}
