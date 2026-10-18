package vn.nhom7.hub;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import vn.nhom7.common.Message;
import vn.nhom7.common.MessageType;
import vn.nhom7.common.Protocol;

/** Kiểm tra dữ liệu thiết bị sau mỗi STATUS, phát ALERT cho mọi Dashboard (theo PROTOCOL.md mục 4). */
public class AlertService {

    private final DeviceRegistry registry;

    public AlertService(DeviceRegistry registry) {
        this.registry = registry;
    }

    /** So sánh trạng thái trước và sau: chỉ cảnh báo lúc vừa chuyển trạng thái, không lặp lại mỗi 5 giây. */
    public void check(DeviceInfo d, JsonObject before, JsonObject after) {
        if ("TEMP_SENSOR".equals(d.getType())) {
            checkTemperature(d, before, after);
        } else if ("CAMERA".equals(d.getType())) {
            checkMotion(d, before, after);
        }
    }

    private void checkTemperature(DeviceInfo d, JsonObject before, JsonObject after) {
        double limit = Protocol.TEMP_ALERT_THRESHOLD;
        double now = number(after, "temperature");
        boolean wasHot = number(before, "temperature") > limit; // chưa có số liệu (NaN) -> false
        boolean isHot = now > limit;

        if (isHot && !wasHot) {
            send(d.getId(), "WARNING", "Nhiệt độ " + now + "°C vượt ngưỡng " + (int) limit + "°C");
        } else if (!isHot && wasHot) {
            send(d.getId(), "INFO", "Nhiệt độ đã trở lại bình thường: " + now + "°C");
        }
    }

    private void checkMotion(DeviceInfo d, JsonObject before, JsonObject after) {
        boolean was = bool(before, "motion");
        boolean is = bool(after, "motion");
        if (is && !was && anyDoorLocked()) {
            send(d.getId(), "INFO", "Phát hiện chuyển động tại " + d.getId() + " trong khi cửa đang khóa");
        }
    }

    private boolean anyDoorLocked() {
        for (DeviceInfo x : registry.getDevices()) {
            if ("DOOR_LOCK".equals(x.getType()) && bool(x.getState(), "locked")) {
                return true;
            }
        }
        return false;
    }

    private void send(String deviceId, String level, String text) {
        JsonObject p = new JsonObject();
        p.addProperty("deviceId", deviceId);
        p.addProperty("level", level);
        p.addProperty("message", text);
        registry.broadcastToDashboards(new Message(MessageType.ALERT, Protocol.HUB_ID, null, p));
        System.out.println("[HUB] ALERT " + level + " " + deviceId + ": " + text);
        ActivityLogger.log("ALERT " + level + " " + deviceId + ": " + text);
    }

    private static double number(JsonObject o, String key) {
        JsonElement e = (o != null) ? o.get(key) : null;
        return (e == null || e.isJsonNull()) ? Double.NaN : e.getAsDouble();
    }

    private static boolean bool(JsonObject o, String key) {
        JsonElement e = (o != null) ? o.get(key) : null;
        return e != null && !e.isJsonNull() && e.getAsBoolean();
    }
}