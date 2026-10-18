package vn.nhom7.hub;

import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import vn.nhom7.common.JsonUtil;
import vn.nhom7.common.Message;
import vn.nhom7.common.MessageType;
import vn.nhom7.common.Protocol;

/** Xử lý một kết nối (thiết bị hoặc Dashboard) trên một luồng riêng. */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private final DeviceRegistry registry;
    private final AlertService alerts;
    private PrintWriter out;
    private volatile String clientId; // null cho tới khi REGISTER thành công

    public ClientHandler(Socket socket, DeviceRegistry registry, AlertService alerts) {
        this.socket = socket;
        this.registry = registry;
        this.alerts = alerts;
    }

    @Override
    public void run() {
        String address = socket.getRemoteSocketAddress().toString();
        try (BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter writer = new PrintWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {

            this.out = writer;
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("[HUB] Nhan tu " + address + ": " + line);
                handleLine(line);
            }
        } catch (IOException e) {
            System.out.println("[HUB] Loi ket noi " + address + ": " + e.getMessage());
        } finally {
            if (clientId != null) {
                boolean wasDevice = registry.remove(clientId, this);
                System.out.println("[HUB] Da go " + clientId + " khoi bang");
                ActivityLogger.log("Ngat ket noi " + clientId);
                if (wasDevice) {
                    JsonObject p = new JsonObject();
                    p.addProperty("deviceId", clientId);
                    registry.broadcastToDashboards(
                            new Message(MessageType.DEVICE_OFFLINE, Protocol.HUB_ID, null, p));
                }
                registry.printTable();
            }
            System.out.println("[HUB] Ngat ket noi " + address);
            close();
        }
    }

    private void handleLine(String line) {
        Message msg;
        try {
            msg = JsonUtil.fromJson(line);
        } catch (IllegalArgumentException e) {
            sendError(e.getMessage());
            return;
        }

        if (clientId == null && msg.getType() != MessageType.REGISTER) {
            sendError("Phai gui REGISTER truoc");
            return;
        }

        switch (msg.getType()) {
            case REGISTER:
                handleRegister(msg);
                break;
            case STATUS:
                handleStatus(msg);
                break;
            case COMMAND:
                handleCommand(msg);
                break;
            case ACK:
                handleAck(msg);
                break;
            default:
                sendError("Chua ho tro loai message: " + msg.getType());
        }
    }

    private void handleRegister(Message msg) {
        if (clientId != null) {
            sendError("Da dang ky voi ID " + clientId);
            return;
        }
        String id = msg.getFrom();
        if (id == null || id.isBlank()) {
            sendError("Thieu truong from");
            return;
        }

        boolean isDashboard = Protocol.ROLE_DASHBOARD.equals(msg.getString("role"));
        DeviceInfo info = null;
        boolean ok;
        if (isDashboard) {
            ok = registry.registerDashboard(id, this);
        } else {
            info = new DeviceInfo(id, msg.getString("deviceType"), msg.getString("name"), this);
            ok = registry.registerDevice(info);
        }
        if (!ok) {
            sendError("ID " + id + " dang duoc su dung");
            ActivityLogger.log("Tu choi dang ky " + id + ": trung ID");
            return;
        }

        clientId = id;
        ActivityLogger.log("Dang ky " + (isDashboard ? "Dashboard " : "thiet bi ") + id
                + (isDashboard ? "" : " (" + msg.getString("deviceType") + ")"));
        JsonObject p = new JsonObject();
        p.addProperty("ok", true);
        p.addProperty("message", "Dang ky thanh cong");
        send(new Message(MessageType.ACK, Protocol.HUB_ID, id, p));

        if (isDashboard) {
            JsonObject list = new JsonObject();
            list.add("devices", registry.toJsonArray());
            send(new Message(MessageType.DEVICE_LIST, Protocol.HUB_ID, id, list));
        } else {
            broadcastDeviceUpdate(info);
        }
        registry.printTable();
    }

    private void handleStatus(Message msg) {
        DeviceInfo d = registry.getDevice(clientId);
        if (d == null) {
            sendError("Chi thiet bi moi duoc gui STATUS");
            return;
        }
        JsonObject before = d.getState();   // trạng thái cũ để so sánh
        d.updateState(msg.getPayload());
        alerts.check(d, before, d.getState());
        broadcastDeviceUpdate(d);
    }

    /** Dashboard -> Hub -> thiết bị. */
    private void handleCommand(Message msg) {
        if (!registry.isDashboard(clientId)) {
            sendError("Chi Dashboard moi duoc gui COMMAND");
            return;
        }
        String target = msg.getTo();
        DeviceInfo d = registry.getDevice(target);
        if (d == null) {
            sendError("Thiet bi " + target + " khong ton tai hoac da ngat ket noi");
            ActivityLogger.log("Lenh tu " + clientId + " toi " + target + " that bai: khong tim thay thiet bi");
            return;
        }
        d.getHandler().send(new Message(MessageType.COMMAND, clientId, target, msg.getPayload()));
        System.out.println("[HUB] Chuyen COMMAND " + msg.getString("action")
                + " tu " + clientId + " toi " + target);
        ActivityLogger.log("Lenh " + msg.getString("action") + " (" + msg.getString("commandId")
                + ") tu " + clientId + " toi " + target);
    }

    /** Thiết bị -> Hub -> đúng Dashboard đã ra lệnh. */
    private void handleAck(Message msg) {
        ActivityLogger.log("Ket qua lenh " + msg.getString("commandId") + " tu " + clientId + ": "
                + (msg.getBoolean("ok", false) ? "thanh cong" : "that bai") + " - " + msg.getString("message"));
        ClientHandler dash = registry.getDashboard(msg.getTo());
        if (dash == null) {
            System.out.println("[HUB] Bo qua ACK tu " + clientId + ": khong tim thay Dashboard " + msg.getTo());
            return;
        }
        dash.send(new Message(MessageType.ACK, clientId, msg.getTo(), msg.getPayload()));
    }

    private void broadcastDeviceUpdate(DeviceInfo d) {
        registry.broadcastToDashboards(
                new Message(MessageType.DEVICE_UPDATE, Protocol.HUB_ID, null, d.toJson()));
    }

    private void sendError(String text) {
        JsonObject p = new JsonObject();
        p.addProperty("message", text);
        send(new Message(MessageType.ERROR, Protocol.HUB_ID, clientId, p));
    }

    /** synchronized vì luồng của Dashboard và luồng của chính thiết bị có thể cùng gửi vào socket này. */
    public synchronized void send(Message message) {
        if (out != null) {
            out.println(JsonUtil.toJson(message));
        }
    }

    /** Đóng kết nối từ phía Hub (dùng khi thiết bị im lặng quá lâu). */
    public void close() {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    public String getClientId() {
        return clientId;
    }
}