package vn.nhom7.device;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Lớp cơ sở cho mọi thiết bị mô phỏng: kết nối Hub, đăng ký, gửi trạng thái, nhận lệnh. */
public abstract class DeviceClient {

    public static final int STATUS_INTERVAL_SECONDS = 5;

    protected final String deviceId;
    protected final String deviceType;
    protected final String name;

    private Socket socket;
    private PrintWriter out;
    private ScheduledExecutorService scheduler;
    private volatile boolean connected;

    protected DeviceClient(String deviceId, String deviceType, String name) {
        this.deviceId = deviceId;
        this.deviceType = deviceType;
        this.name = name;
    }

    /** Trạng thái hiện tại để gửi trong STATUS. Mỗi loại thiết bị tự định nghĩa. */
    protected abstract JsonObject getState();

    /** Thực thi lệnh, trả về câu mô tả kết quả. Ném IllegalArgumentException nếu lệnh không hỗ trợ. */
    protected abstract String executeCommand(String action);

    /** Kết nối tới Hub, gửi REGISTER, bật luồng nghe lệnh và lịch gửi STATUS định kỳ. */
    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        out = new PrintWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        connected = true;

        Thread listener = new Thread(this::listen, deviceId + "-listener");
        listener.setDaemon(true);
        listener.start();

        JsonObject payload = new JsonObject();
        payload.addProperty("role", "DEVICE");
        payload.addProperty("deviceType", deviceType);
        payload.addProperty("name", name);
        send("REGISTER", null, payload);

        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::sendStatus, 0, STATUS_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    /** Chạy trên luồng riêng: đọc từng dòng JSON Hub gửi xuống. */
    private void listen() {
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("[" + deviceId + "] Nhan: " + line);
                handleLine(line);
            }
        } catch (IOException e) {
            System.out.println("[" + deviceId + "] Mat ket noi: " + e.getMessage());
        } finally {
            connected = false;
            if (scheduler != null) {
                scheduler.shutdownNow();
            }
        }
    }

    /** Chỉ xử lý message COMMAND; các loại khác (ví dụ ACK của Hub) chỉ in ra. */
    private void handleLine(String line) {
        JsonObject msg;
        try {
            msg = JsonParser.parseString(line).getAsJsonObject();
        } catch (RuntimeException e) {
            System.out.println("[" + deviceId + "] JSON sai dinh dang: " + line);
            return;
        }
        if (!"COMMAND".equals(getString(msg, "type"))) {
            return;
        }

        JsonObject p = msg.has("payload") ? msg.getAsJsonObject("payload") : null;
        JsonObject ack = new JsonObject();
        ack.addProperty("commandId", getString(p, "commandId"));
        try {
            String result = executeCommand(getString(p, "action"));
            ack.addProperty("ok", true);
            ack.addProperty("message", result);
        } catch (IllegalArgumentException e) {
            ack.addProperty("ok", false);
            ack.addProperty("message", e.getMessage());
        }
        send("ACK", getString(msg, "from"), ack);
        sendStatus(); // báo ngay trạng thái mới sau khi thực thi lệnh
    }

    public void sendStatus() {
        send("STATUS", null, getState());
    }

    /** synchronized vì luồng nghe lệnh và luồng gửi định kỳ cùng ghi vào socket. */
    protected synchronized void send(String type, String to, JsonObject payload) {
        if (!connected) {
            return;
        }
        JsonObject msg = new JsonObject();
        msg.addProperty("type", type);
        msg.addProperty("from", deviceId);
        if (to != null) {
            msg.addProperty("to", to);
        }
        msg.addProperty("timestamp", System.currentTimeMillis());
        msg.add("payload", payload);
        out.println(msg.toString()); // mỗi message một dòng
    }

    public void disconnect() {
        connected = false;
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
    }

    public boolean isConnected() {
        return connected;
    }

    private static String getString(JsonObject o, String key) {
        return (o != null && o.has(key) && !o.get(key).isJsonNull()) ? o.get(key).getAsString() : null;
    }
}