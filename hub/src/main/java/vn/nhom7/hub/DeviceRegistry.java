package vn.nhom7.hub;

import com.google.gson.JsonArray;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Bảng thiết bị và Dashboard đang kết nối, dùng chung giữa nhiều luồng ClientHandler. */
public class DeviceRegistry {

    // ConcurrentHashMap: nhiều luồng thêm/xóa/đọc cùng lúc mà không cần tự khóa
    private final Map<String, DeviceInfo> devices = new ConcurrentHashMap<>();
    private final Map<String, ClientHandler> dashboards = new ConcurrentHashMap<>();

    /** Trả về false nếu ID đã có thiết bị khác đang dùng. */
    public boolean registerDevice(DeviceInfo info) {
        return devices.putIfAbsent(info.getId(), info) == null;
    }

    public boolean registerDashboard(String id, ClientHandler handler) {
        return dashboards.putIfAbsent(id, handler) == null;
    }

    public DeviceInfo getDevice(String id) {
        return devices.get(id);
    }

    public Collection<DeviceInfo> getDevices() {
        return devices.values();
    }

    public Collection<ClientHandler> getDashboards() {
        return dashboards.values();
    }

    /** Gỡ khi ngắt kết nối; chỉ gỡ đúng kết nối đó để không xóa nhầm khi ID được dùng lại. */
    public void remove(String id, ClientHandler handler) {
        devices.computeIfPresent(id, (key, d) -> d.getHandler() == handler ? null : d);
        dashboards.remove(id, handler);
    }

    public JsonArray toJsonArray() {
        JsonArray arr = new JsonArray();
        for (DeviceInfo d : devices.values()) {
            arr.add(d.toJson());
        }
        return arr;
    }

    public void printTable() {
        System.out.println("[HUB] ---- Bang thiet bi (" + devices.size() + ") ----");
        for (DeviceInfo d : devices.values()) {
            System.out.println("[HUB]  " + d.getId() + " | " + d.getType() + " | " + d.getState());
        }
        System.out.println("[HUB]  Dashboard: " + dashboards.keySet());
    }
}