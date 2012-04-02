package nl.orange11.healthcheck.api;

/**
 * <p>Interface for ping executor implementations. Each implementation must provide a mechanism for a client to
 * execute a ping. The ping can be executed in three different levels:</p>
 * <ul>
 * <li>Basic - This should be as light weight as possible for the underlying system. Just to see if it is up.</li>
 * <li>Extended - A more extensive ping that means a more extensive check of the system health is performed.</li>
 * <li>Thorough - Returns data about the system that can help in understanding the state of the system.</li>
 * </ul>
 *
 * @author Jettro Coenradie
 */
public interface PingExecutor {

    /**
     * Returns the name of the PingExecutor. Can be used in overviews to list all available ping executors.
     *
     * @return String containing the name of the PingExecutor
     */
    String getName();

    /**
     * Executes the default ping, usually this is the Basic ping. It is up to the implementation to adhere to this soft
     * requirement. Check the documentation of the implementation to learn more about it.
     *
     * @return PingResult containing the System status and a short description.
     */
    PingResult execute();

    /**
     * Executes the extended ping, usually this is a less light weight ping than the Basic ping. It is up to the
     * implementation to adhere to this soft requirement. Check the documentation of the implementation to learn more
     * about it.
     *
     * @return PingResult containing the System status and a short description
     */
    PingResult executeExtended();

    /**
     * Executes the thorough ping, this ping returns the Thorough results containing more information about the system.
     *
     * @return ThoroughPingResult containing the data from the system as well as the basic ping results.
     */
    ThoroughPingResult executeThorough();

    /**
     * Function that can be used to determine your own level of the ping besides the Basic and Thorough.
     *
     * @param pingLevel PingLevel used to execute the ping
     * @return PingResult containing information about the ping. The ThoroughPingResult can be used if you want to provide
     *         more information
     */
    PingResult execute(PingLevel pingLevel);
}
