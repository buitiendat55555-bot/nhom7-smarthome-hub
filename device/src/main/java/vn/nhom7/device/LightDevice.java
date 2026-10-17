package vn.nhom7.device;

import com.google.gson.JsonObject;
import java.util.function.Consumer;

/** Đèn thông minh: trạng thái ON/OFF, nhận lệnh ON/OFF từ Hub. */
public class LightDevice extends DeviceClient {

    private volatile boolean on;
    private Consumer<Boolean> onChange = state -> { };

    public LightDevice(String deviceId, String name) {
        super(deviceId, "LIGHT", name);
    }

    /** Giao diện đăng ký hàm này để được báo mỗi khi đèn đổi trạng thái. */
    public void setOnChange(Consumer<Boolean> listener) {
        this.onChange = (listener != null) ? listener : state -> { };
    }

    public boolean isOn() {
        return on;
    }

    public String getDeviceId() {
        return deviceId;
    }

    /** Người dùng bấm nút ngay trên thiết bị: đổi trạng thái và báo Hub luôn. */
    public void toggle() {
        setOn(!on);
        sendStatus();
    }

    private void setOn(boolean value) {
        on = value;
        onChange.accept(value);
    }

    @Override
    protected JsonObject getState() {
        JsonObject s = new JsonObject();
        s.addProperty("power", on ? "ON" : "OFF");
        return s;
    }

    /** Được gọi trên luồng nghe lệnh của DeviceClient khi Hub gửi COMMAND. */
    @Override
    protected String executeCommand(String action) {
        if ("ON".equals(action)) {
            setOn(true);
            return "Da bat den";
        }
        if ("OFF".equals(action)) {
            setOn(false);
            return "Da tat den";
        }
        throw new IllegalArgumentException("Lenh khong ho tro: " + action);
    }
}