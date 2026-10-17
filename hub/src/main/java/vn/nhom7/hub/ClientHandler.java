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
    private PrintWriter out;
    private String clientId; // null cho tới khi REGISTER thành công

    public ClientHandler(Socket socket, DeviceRegistry registry) {
        this.socket = socket;
        this.registry = registry;
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
                registry.remove(clientId, this);
                System.out.println("[HUB] Da go " + clientId + " khoi bang");
                registry.printTable();
            }
            System.out.println("[HUB] Ngat ket noi " + address);
            try {
                socket.close();
            } catch (IOException ignored) {
            }
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

        boolean ok;
        if (Protocol.ROLE_DASHBOARD.equals(msg.getString("role"))) {
            ok = registry.registerDashboard(id, this);
        } else {
            ok = registry.registerDevice(
                    new DeviceInfo(id, msg.getString("deviceType"), msg.getString("name"), this));
        }
        if (!ok) {
            sendError("ID " + id + " dang duoc su dung");
            return;
        }

        clientId = id;
        JsonObject p = new JsonObject();
        p.addProperty("ok", true);
        p.addProperty("message", "Dang ky thanh cong");
        send(new Message(MessageType.ACK, Protocol.HUB_ID, id, p));
        registry.printTable();
    }

    private void handleStatus(Message msg) {
        DeviceInfo d = registry.getDevice(clientId);
        if (d == null) {
            sendError("Chi thiet bi moi duoc gui STATUS");
            return;
        }
        d.updateState(msg.getPayload());
    }

    private void sendError(String text) {
        JsonObject p = new JsonObject();
        p.addProperty("message", text);
        send(new Message(MessageType.ERROR, Protocol.HUB_ID, clientId, p));
    }

    /** synchronized để nhiều luồng không ghi chồng lên nhau. */
    public synchronized void send(Message message) {
        if (out != null) {
            out.println(JsonUtil.toJson(message));
        }
    }

    public String getClientId() {
        return clientId;
    }
}