package vn.nhom7.device;

import com.google.gson.JsonObject;

/** Đèn thông minh: bật/tắt, độ sáng 1-100 %, màu ánh sáng. */
public class LightDevice extends DeviceClient {

    public static final String DEFAULT_COLOR = "#FFC83D";

    private volatile boolean on;
    private volatile int brightness = 100;
    private volatile String color = DEFAULT_COLOR;

    public LightDevice(String deviceId, String name) {
        super(deviceId, "LIGHT", name);
    }

    public boolean isOn() {
        return on;
    }

    public int getBrightness() {
        return brightness;
    }

    public String getColor() {
        return color;
    }

    /** Người dùng bấm nút ngay trên thiết bị: đổi trạng thái và báo Hub luôn. */
    public void toggle() {
        on = !on;
        changed();
    }

    public void setBrightness(int value) {
        brightness = Math.max(1, Math.min(100, value));
        on = true;
        changed();
    }

    public void setColor(String hex) {
        color = hex;
        on = true;
        changed();
    }

    private void changed() {
        stateChanged();
        sendStatus();
    }

    @Override
    protected JsonObject getState() {
        JsonObject s = new JsonObject();
        s.addProperty("power", on ? "ON" : "OFF");
        s.addProperty("brightness", brightness);
        s.addProperty("color", color);
        return s;
    }

    @Override
    protected String executeCommand(String action) {
        if ("ON".equals(action)) {
            on = true;
            stateChanged();
            return "Da bat den";
        }
        if ("OFF".equals(action)) {
            on = false;
            stateChanged();
            return "Da tat den";
        }
        throw new IllegalArgumentException("Lenh khong ho tro: " + action);
    }

    /** BRIGHTNESS và COLOR cần thêm trường value trong payload. */
    @Override
    protected String executeCommand(String action, JsonObject payload) {
        if ("BRIGHTNESS".equals(action)) {
            int v = payload.get("value").getAsInt();
            brightness = Math.max(1, Math.min(100, v));
            on = true;
            stateChanged();
            return "Do sang " + brightness + "%";
        }
        if ("COLOR".equals(action)) {
            String hex = payload.get("value").getAsString();
            if (!hex.matches("#[0-9A-Fa-f]{6}")) {
                throw new IllegalArgumentException("Mau khong hop le: " + hex);
            }
            color = hex.toUpperCase();
            on = true;
            stateChanged();
            return "Doi mau " + color;
        }
        return executeCommand(action);
    }
}
