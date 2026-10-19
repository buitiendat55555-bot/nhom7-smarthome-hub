package vn.nhom7.device;

import com.google.gson.JsonObject;

/** Cảm biến nhiệt độ: chỉ gửi dữ liệu, không nhận lệnh điều khiển. */
public class TempSensorDevice extends DeviceClient {

    private volatile double temperature = 28.0;

    public TempSensorDevice(String deviceId, String name) {
        super(deviceId, "TEMP_SENSOR", name);
    }

    public double getTemperature() {
        return temperature;
    }

    /** Đổi nhiệt độ (thanh trượt, nút có sẵn) và báo Hub ngay, không chờ chu kỳ 5 giây. */
    public void setTemperature(double value) {
        temperature = Math.round(Math.max(0, Math.min(60, value)) * 10) / 10.0; // làm tròn 1 chữ số
        stateChanged();
        sendStatus();
    }

    @Override
    protected JsonObject getState() {
        JsonObject s = new JsonObject();
        s.addProperty("temperature", temperature);
        return s;
    }

    @Override
    protected String executeCommand(String action) {
        throw new IllegalArgumentException("Cam bien nhiet do khong nhan lenh");
    }
}
