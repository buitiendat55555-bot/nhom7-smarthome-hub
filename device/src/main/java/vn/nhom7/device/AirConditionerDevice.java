package vn.nhom7.device;

import com.google.gson.JsonObject;

/** Máy lạnh: bật/tắt và nhiệt độ cài đặt 16-30 °C. Thiết bị mới, kế thừa DeviceClient như các loại khác. */
public class AirConditionerDevice extends DeviceClient {

    public static final int MIN_TEMP = 16;
    public static final int MAX_TEMP = 30;

    private volatile boolean on;
    private volatile int targetTemp = 26;

    public AirConditionerDevice(String deviceId, String name) {
        super(deviceId, "AIR_CONDITIONER", name);
    }

    public boolean isOn() {
        return on;
    }

    public int getTargetTemp() {
        return targetTemp;
    }

    public void toggle() {
        on = !on;
        stateChanged();
        sendStatus();
    }

    /** Bấm - / + trên máy: đổi nhiệt độ cài đặt. */
    public void adjust(int delta) {
        targetTemp = clamp(targetTemp + delta);
        stateChanged();
        sendStatus();
    }

    private static int clamp(int t) {
        return Math.max(MIN_TEMP, Math.min(MAX_TEMP, t));
    }

    @Override
    protected JsonObject getState() {
        JsonObject s = new JsonObject();
        s.addProperty("power", on ? "ON" : "OFF");
        s.addProperty("targetTemp", targetTemp);
        return s;
    }

    @Override
    protected String executeCommand(String action) {
        if ("ON".equals(action)) {
            on = true;
            stateChanged();
            return "Da bat may lanh";
        }
        if ("OFF".equals(action)) {
            on = false;
            stateChanged();
            return "Da tat may lanh";
        }
        throw new IllegalArgumentException("Lenh khong ho tro: " + action);
    }

    /** SET_TEMP cần value là số độ, ví dụ {"action":"SET_TEMP","value":22}. */
    @Override
    protected String executeCommand(String action, JsonObject payload) {
        if ("SET_TEMP".equals(action)) {
            targetTemp = clamp(payload.get("value").getAsInt());
            on = true;
            stateChanged();
            return "Dat nhiet do " + targetTemp + " do C";
        }
        return executeCommand(action);
    }
}
