package vn.nhom7.dashboard;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

/** Ghi nhật ký Dashboard ra file logs/dashboard-<id>.log và đọc lại khi cần. */
public class DashboardLog {

    private final Path file;

    public DashboardLog(String dashboardId) {
        this.file = Paths.get("logs", "dashboard-" + dashboardId + ".log");
    }

    /** Nối thêm một dòng vào cuối file (tạo thư mục logs nếu chưa có). */
    public synchronized void append(String line) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, line + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.out.println("[DASHBOARD] Khong ghi duoc log: " + e.getMessage());
        }
    }

    /** Đọc toàn bộ file; chưa có file thì trả về danh sách rỗng. */
    public synchronized List<String> readAll() throws IOException {
        if (!Files.exists(file)) {
            return List.of();
        }
        return Files.readAllLines(file, StandardCharsets.UTF_8);
    }

    public Path getFile() {
        return file.toAbsolutePath();
    }
}