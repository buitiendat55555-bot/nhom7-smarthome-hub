package vn.nhom7.dashboard;

import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import vn.nhom7.common.JsonUtil;
import vn.nhom7.common.Message;
import vn.nhom7.common.MessageType;
import vn.nhom7.common.Protocol;

/** Kết nối TCP tới Hub dưới vai trò Dashboard. */
public class HubConnection {

    private final String dashboardId;
    private final Consumer<Message> onMessage;
    private final Runnable onDisconnect;
    private final AtomicInteger commandCounter = new AtomicInteger();
    // Báo cho tab "Gói tin" từng dòng JSON đi/đến: (true = gửi đi, dòng JSON)
    private volatile BiConsumer<Boolean, String> traffic = (out, line) -> { };

    private Socket socket;
    private PrintWriter out;
    private volatile boolean connected;

    /**
     * @param onMessage    được gọi mỗi khi nhận một message từ Hub (chạy trên luồng mạng)
     * @param onDisconnect được gọi khi mất kết nối tới Hub
     */
    public HubConnection(String dashboardId, Consumer<Message> onMessage, Runnable onDisconnect) {
        this.dashboardId = dashboardId;
        this.onMessage = onMessage;
        this.onDisconnect = onDisconnect;
    }

    public void setTrafficListener(BiConsumer<Boolean, String> listener) {
        this.traffic = (listener != null) ? listener : (o, l) -> { };
    }

    /** Kết nối, bật luồng nghe, rồi đăng ký với role DASHBOARD. */
    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        out = new PrintWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        connected = true;

        Thread listener = new Thread(this::listen, "dashboard-listener");
        listener.setDaemon(true);
        listener.start();

        JsonObject p = new JsonObject();
        p.addProperty("role", Protocol.ROLE_DASHBOARD);
        send(new Message(MessageType.REGISTER, dashboardId, null, p));
    }

    /** Luồng riêng: đọc từng dòng JSON Hub gửi về rồi chuyển cho giao diện. */
    private void listen() {
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                traffic.accept(false, line);
                try {
                    onMessage.accept(JsonUtil.fromJson(line));
                } catch (IllegalArgumentException e) {
                    System.out.println("[DASHBOARD] Bo qua dong loi: " + line);
                }
            }
        } catch (IOException e) {
            System.out.println("[DASHBOARD] Mat ket noi: " + e.getMessage());
        } finally {
            connected = false;
            onDisconnect.run();
        }
    }

    /**
     * Gửi lệnh tới một thiết bị qua Hub. Trả về commandId để đối chiếu với ACK.
     * AtomicInteger để hai lần bấm nhanh liên tiếp không bị trùng mã.
     */
    public String sendCommand(String deviceId, String action) {
        return sendCommand(deviceId, action, null);
    }

    /** Lệnh có giá trị: BRIGHTNESS 30, COLOR "#60A5FA", SET_TEMP 22. value = null nếu không cần. */
    public String sendCommand(String deviceId, String action, Object value) {
        String commandId = "c-" + commandCounter.incrementAndGet();
        JsonObject p = new JsonObject();
        p.addProperty("commandId", commandId);
        p.addProperty("action", action);
        if (value instanceof Number) {
            p.addProperty("value", (Number) value);
        } else if (value != null) {
            p.addProperty("value", value.toString());
        }
        send(new Message(MessageType.COMMAND, dashboardId, deviceId, p));
        return commandId;
    }

    /** synchronized vì giao diện và luồng nghe có thể cùng gửi. */
    public synchronized void send(Message message) {
        if (connected && out != null) {
            String line = JsonUtil.toJson(message);
            out.println(line);
            traffic.accept(true, line);
        }
    }

    public void disconnect() {
        connected = false;
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

    public String getDashboardId() {
        return dashboardId;
    }
}