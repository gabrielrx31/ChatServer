package server.utils;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.nio.file.*;

// A custom, lightweight logger for writing formatted messages to the console and a log file.
// Note: This is an alternative implementation to the one using java.util.logging.
public class Logger {

    // Categorizes log entries, making it easier to filter and search the log file.
    public enum LogEvent {
        SERVER_LIFECYCLE,
        USER_SESSION,
        CHAT_MESSAGE,
        FILE_TRANSFER,
        SYSTEM_ERROR,
        DATABASE
    }

    private static final DateTimeFormatter dtf =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Path LOG_PATH = Paths.get("logs/server.log");

    // This static block runs once when the class is first loaded.
    // It ensures the 'logs' directory exists before any logging occurs.
    static {
        try {
            Files.createDirectories(LOG_PATH.getParent());
        } catch (IOException e) {
            System.err.println("Could not create logs directory: " + e.getMessage());
        }
    }

    // A private constructor prevents anyone from creating an instance of this utility class.
    private Logger() {}

    public static void info(LogEvent event, String message) {
        log("INFO", event, message, null);
    }

    public static void warning(LogEvent event, String message) {
        log("WARNING", event, message, null);
    }

    public static void error(LogEvent event, String message, Throwable t) {
        log("ERROR", event, message, t);
    }

    // The core logging method. It's synchronized to prevent garbled output
    // from multiple threads writing to the file at the same time.
    private static synchronized void log(String level, LogEvent event, String message, Throwable t) {
        String time = dtf.format(LocalDateTime.now());
        String line = String.format("[%s] [%s] [%s] - %s", time, level, event, message);

        // Always print the formatted line to the console.
        System.out.println(line);
        if (t != null) t.printStackTrace(System.out);

        // Append the log entry to the file. Using try-with-resources ensures the writer is always closed.
        try (PrintWriter pw = new PrintWriter(new FileWriter(LOG_PATH.toFile(), true))) {
            pw.println(line);
            if (t != null) t.printStackTrace(pw);
        } catch (IOException e) {
            System.err.println("Failed to write to log file: " + e.getMessage());
        }
    }
}