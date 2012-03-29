package nl.orange11.healthcheck.api;

/**
 * Different levels that the ping can perform.
 *
 * @author Jettro Coenradie
 */
public enum PingLevel {
    BASIC, // Lightweight ping
    EXTENDED, // Can do a little more than Basic and is therefore heavier on the system
    THOROUGH // Returns more information about the system and can therefore use more resources of the system
}
