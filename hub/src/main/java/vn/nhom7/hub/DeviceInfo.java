package vn.nhom7.hub;

import com.google.gson.JsonObject;

/** Thông tin một thiết bị đang kết nối, lưu trong DeviceRegistry. */
public class DeviceInfo {

    private final String id;
    private final String type;
    private final String name;
    private final ClientHandler handler;
    private volatile JsonObject state = new JsonObject();
    private volatile long lastSeen = System.currentTimeMillis();

    public DeviceInfo(String id, String type, String name, ClientHandler handler) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.handler = handler;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public ClientHandler getHandler() {
        return handler;
    }

    public JsonObject getState() {
        return state.deepCopy();
    }

    public long getLastSeen() {
        return lastSeen;
    }

    /** Lưu trạng thái mới nhất từ STATUS và ghi lại thời điểm nhận. */
    public void updateState(JsonObject newState) {
        state = (newState != null) ? newState.deepCopy() : new JsonObject();
        lastSeen = System.currentTimeMillis();
    }

    /** Dạng JSON để gửi cho Dashboard trong DEVICE_LIST (Issue #8). */
    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("deviceId", id);
        o.addProperty("deviceType", type);
        o.addProperty("name", name);
        o.add("state", state.deepCopy());
        return o;
    }
}