package nl.trifork.healthcheck.api;

/**
 * Different levels that the ping can perform.
 *
 * @author Jettro Coenradie
 */
public enum PingLevel {
    /**
     * Lightweight ping
     */
    BASIC,
    /**
     * Can do a little more than Basic and is therefore heavier on the system
     */
    EXTENDED,
    /**
     * Returns more information about the system and can therefore use more resources of the system
     */
    THOROUGH
}
