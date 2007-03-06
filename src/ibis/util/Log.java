package ibis.util;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.WriterAppender;

public class Log {

    public static synchronized void initLog4J(String name) {
        initLog4J(Logger.getLogger(name));
    }
    
    public static synchronized void initLog4J(Logger logger) {
        
        Logger rootLogger = Logger.getRootLogger();

        if (rootLogger.getAllAppenders().hasMoreElements()) {
            // there is a root logger
            return;
        }

        String[] path = logger.getName().split(".");

        String currentPath = "";
        for (int i = 0; i < path.length; i++) {
            currentPath += path[i];

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
    }
}
