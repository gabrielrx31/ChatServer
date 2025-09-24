package server.utils;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.nio.file.*;

public class Logger {

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

    static {
        try {
            Files.createDirectories(LOG_PATH.getParent()); // Opret "logs" mappe hvis den ikke findes
        } catch (IOException e) {
            System.err.println("Kunne ikke oprette logs mappe: " + e.getMessage());
        }
    }

    private Logger() {} // Ingen instansiering

    public static void info(LogEvent event, String message) {
        log("INFO", event, message, null);
    }

    public static void warning(LogEvent event, String message) {
        log("WARNING", event, message, null);
    }

    public static void error(LogEvent event, String message, Throwable t) {
        log("ERROR", event, message, t);
    }

    private static void log(String level, LogEvent event, String message, Throwable t) {
        String time = dtf.format(LocalDateTime.now());
        String line = String.format("[%s] [%s] [%s] - %s", time, level, event, message);

        // Skriv til konsol
        System.out.println(line);
        if (t != null) t.printStackTrace(System.out);

        // Skriv til fil (append)
        try (PrintWriter pw = new PrintWriter(new FileWriter(LOG_PATH.toFile(), true))) {
            pw.println(line);
            if (t != null) t.printStackTrace(pw);
        } catch (IOException e) {
            System.err.println("Kunne ikke skrive til logfil: " + e.getMessage());
        }
    }
}
