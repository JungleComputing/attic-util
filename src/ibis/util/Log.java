package ibis.util;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.WriterAppender;

/**
 * Utility to initialize log4j, even in the absence of a log4j.properties
 * file.
 */
public class Log {

    /**
     * Checks for the presence of appenders for the specified name
     * (or appenders higher up in the logger hierarchy). If none is
     * found, a default one is installed for the specified name.
     * @param name the logger name.
     */
    public static synchronized void initLog4J(String name) {
        initLog4J(Logger.getLogger(name));
    }

    /**
     * Checks for the presence of appenders for the specified logger
     * (or appenders higher up in the logger hierarchy). If none is
     * found, a default one is installed for the specified logger.
     * @param logger the specified logger.
     */
    public static synchronized void initLog4J(Logger logger) {

        Logger rootLogger = Logger.getRootLogger();

        if (rootLogger.getAllAppenders().hasMoreElements()) {
            // there is a root logger
            return;
        }

        String[] path = logger.getName().split("\\.");

        //System.err.println("path.length = " + path.length);

        for (int i = 0; i < path.length; i++) {
            String currentPath = path[0];
            for (int j = 1; j <= i;j++) {
                currentPath = currentPath + "." + path[j];
            }

            //System.err.println("checking: " + currentPath);

            Logger currentLogger = Logger.getLogger(currentPath);

            if (currentLogger.getAllAppenders().hasMoreElements()) {
                // a logger exists for this sub-path
                return;
            }
        }

        // No appenders defined, print to standard err by default
        PatternLayout layout = new PatternLayout("%d{HH:mm:ss} %-5p %m%n");
        WriterAppender appender = new WriterAppender(layout, System.err);
        logger.addAppender(appender);
        logger.setLevel(Level.WARN);
        //logger.warn("Added appender to: " + logger.getName());
    }
}
