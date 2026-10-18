package vn.nhom7.device;

import com.google.gson.JsonObject;
import java.util.function.Consumer;

/** Khóa cửa thông minh: trạng thái locked, nhận lệnh LOCK/UNLOCK. */
public class DoorLockDevice extends DeviceClient {

    private volatile boolean locked = true;
    private Consumer<Boolean> onChange = state -> { };

    public DoorLockDevice(String deviceId, String name) {
        super(deviceId, "DOOR_LOCK", name);
    }

    public void setOnChange(Consumer<Boolean> listener) {
        this.onChange = (listener != null) ? listener : state -> { };
    }

    public String getDeviceId() {
        return deviceId;
    }

    public boolean isLocked() {
        return locked;
    }

    /** Người dùng bấm nút ngay trên khóa: đổi trạng thái và báo Hub luôn. */
    public void toggle() {
        setLocked(!locked);
        sendStatus();
    }

    private void setLocked(boolean value) {
        locked = value;
        onChange.accept(value);
    }

    @Override
    protected JsonObject getState() {
        JsonObject s = new JsonObject();
        s.addProperty("locked", locked);
        return s;
    }

    @Override
    protected String executeCommand(String action) {
        if ("LOCK".equals(action)) {
            setLocked(true);
            return "Da khoa cua";
        }
        if ("UNLOCK".equals(action)) {
            setLocked(false);
            return "Da mo khoa";
        }
        throw new IllegalArgumentException("Lenh khong ho tro: " + action);
    }
}