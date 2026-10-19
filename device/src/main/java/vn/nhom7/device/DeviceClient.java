package vn.nhom7.device;

import com.google.gson.JsonObject;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import vn.nhom7.common.JsonUtil;
import vn.nhom7.common.Message;
import vn.nhom7.common.MessageType;
import vn.nhom7.common.Protocol;

/** Lớp cơ sở cho mọi thiết bị mô phỏng: kết nối Hub, đăng ký, gửi trạng thái, nhận lệnh, tự kết nối lại. */
public abstract class DeviceClient {

    public static final int RECONNECT_DELAY_SECONDS = 3;

    protected final String deviceId;
    protected final String deviceType;
    protected final String name;

    private String host = Protocol.DEFAULT_HOST;
    private int port = Protocol.PORT;
    private Socket socket;
    private PrintWriter out;
    private ScheduledExecutorService scheduler;
    private volatile boolean connected;
    private volatile boolean stopped; // true khi người dùng chủ động ngắt -> không tự kết nối lại
    private final AtomicBoolean reconnecting = new AtomicBoolean(); // tránh chạy 2 vòng thử lại cùng lúc

    // Giao diện đăng ký để hiện "vừa nhận lệnh" và "vừa gửi STATUS" (gọi trên luồng mạng)
    private volatile Consumer<String> onCommand = text -> { };
    private volatile Runnable onStatusSent = () -> { };
    private volatile Runnable onStateChange = () -> { };

    protected DeviceClient(String deviceId, String deviceType, String name) {
        this.deviceId = deviceId;
        this.deviceType = deviceType;
        this.name = name;
    }

    /** Trạng thái hiện tại để gửi trong STATUS. Mỗi loại thiết bị tự định nghĩa. */
    protected abstract JsonObject getState();

    /** Thực thi lệnh, trả về câu mô tả kết quả. Ném IllegalArgumentException nếu lệnh không hỗ trợ. */
    protected abstract String executeCommand(String action);

    /**
     * Lệnh có kèm giá trị (ví dụ BRIGHTNESS với value = 30). Mặc định bỏ qua value;
     * thiết bị nào cần giá trị thì override hàm này.
     */
    protected String executeCommand(String action, JsonObject payload) {
        return executeCommand(action);
    }

    public void setOnCommand(Consumer<String> listener) {
        this.onCommand = (listener != null) ? listener : text -> { };
    }

    public void setOnStatusSent(Runnable listener) {
        this.onStatusSent = (listener != null) ? listener : () -> { };
    }

    /** Giao diện đăng ký để vẽ lại mỗi khi trạng thái thiết bị thay đổi. */
    public void setOnStateChange(Runnable listener) {
        this.onStateChange = (listener != null) ? listener : () -> { };
    }

    /** Lớp con gọi sau khi đổi trạng thái (do người bấm hoặc do lệnh từ Hub). */
    protected void stateChanged() {
        onStateChange.run();
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public String getName() {
        return name;
    }

    /** Kết nối tới Hub mặc định (127.0.0.1:5000). */
    public void connect() throws IOException {
        connect(Protocol.DEFAULT_HOST, Protocol.PORT);
    }

    /** Kết nối tới Hub. Nếu thất bại vẫn bật vòng tự thử lại, rồi báo lỗi cho giao diện. */
    public void connect(String host, int port) throws IOException {
        this.host = host;
        this.port = port;
        stopped = false;
        try {
            openConnection();
        } catch (IOException e) {
            startReconnectLoop();
            throw e;
        }
    }

    /** Mở socket, bật luồng nghe, gửi REGISTER, bật lịch gửi STATUS. synchronized để không mở 2 kết nối cùng lúc. */
    private synchronized void openConnection() throws IOException {
        if (connected) {
            return;
        }
        Socket s = new Socket(host, port);
        socket = s;
        out = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8), true);
        connected = true;

        Thread listener = new Thread(() -> listen(s), deviceId + "-listener");
        listener.setDaemon(true);
        listener.start();

        JsonObject payload = new JsonObject();
        payload.addProperty("role", Protocol.ROLE_DEVICE);
        payload.addProperty("deviceType", deviceType);
        payload.addProperty("name", name);
        send(MessageType.REGISTER, null, payload);

        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::sendStatus, 0,
                Protocol.STATUS_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    /** Chạy trên luồng riêng: đọc từng dòng JSON Hub gửi xuống. */
    private void listen(Socket s) {
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8))) {
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
            if (!stopped) {
                System.out.println("[" + deviceId + "] Se thu ket noi lai moi "
                        + RECONNECT_DELAY_SECONDS + " giay");
                startReconnectLoop();
            }
        }
    }

    /** Luồng nền: cứ RECONNECT_DELAY_SECONDS giây thử kết nối lại một lần cho tới khi được. */
    private void startReconnectLoop() {
        if (stopped || !reconnecting.compareAndSet(false, true)) {
            return;
        }
        Thread t = new Thread(() -> {
            try {
                while (!stopped && !connected) {
                    Thread.sleep(RECONNECT_DELAY_SECONDS * 1000L);
                    if (stopped) {
                        break;
                    }
                    try {
                        openConnection();
                        System.out.println("[" + deviceId + "] Da ket noi lai Hub");
                    } catch (IOException e) {
                        System.out.println("[" + deviceId + "] Thu ket noi lai that bai: " + e.getMessage());
                    }
                }
            } catch (InterruptedException ignored) {
            } finally {
                reconnecting.set(false);
            }
        }, deviceId + "-reconnect");
        t.setDaemon(true);
        t.start();
    }

    /** Chỉ xử lý message COMMAND; các loại khác (ví dụ ACK của Hub) chỉ in ra. */
    private void handleLine(String line) {
        Message msg;
        try {
            msg = JsonUtil.fromJson(line);
        } catch (IllegalArgumentException e) {
            System.out.println("[" + deviceId + "] " + e.getMessage());
            return;
        }
        if (msg.getType() != MessageType.COMMAND) {
            return;
        }

        JsonObject ack = new JsonObject();
        ack.addProperty("commandId", msg.getString("commandId"));
        String action = msg.getString("action");
        String value = msg.getString("value");
        String what = action + (value != null ? " " + value : "") + "  ← " + msg.getFrom();
        try {
            String result = executeCommand(action, msg.getPayload());
            ack.addProperty("ok", true);
            ack.addProperty("message", result);
            onCommand.accept("✔ " + what);
        } catch (RuntimeException e) { // lệnh lạ hoặc value sai kiểu số
            ack.addProperty("ok", false);
            ack.addProperty("message", e.getMessage());
            onCommand.accept("✖ Từ chối " + what);
        }
        send(MessageType.ACK, msg.getFrom(), ack);
        sendStatus(); // báo ngay trạng thái mới sau khi thực thi lệnh
    }

    public void sendStatus() {
        if (connected) {
            send(MessageType.STATUS, null, getState());
            onStatusSent.run();
        }
    }

    /** synchronized vì luồng nghe lệnh, luồng gửi định kỳ và giao diện cùng ghi vào socket. */
    protected synchronized void send(MessageType type, String to, JsonObject payload) {
        if (!connected) {
            return;
        }
        out.println(JsonUtil.toJson(new Message(type, deviceId, to, payload))); // mỗi message một dòng
    }

    /** Người dùng chủ động ngắt (đóng cửa sổ): dừng hẳn, không tự kết nối lại. */
    public void disconnect() {
        stopped = true;
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
}