package vn.nhom7.hub;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Ghi log hoạt động của Hub ra file logs/hub.log, mỗi dòng có ngày giờ. */
public final class ActivityLogger {

    private static final Path FILE = Paths.get("logs", "hub.log");
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private ActivityLogger() {
    }

    /** synchronized vì nhiều luồng ClientHandler cùng ghi vào một file. */
    public static synchronized void log(String text) {
        String line = LocalDateTime.now().format(FMT) + " | " + text;
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, line + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.out.println("[HUB] Khong ghi duoc log: " + e.getMessage());
        }
    }
}