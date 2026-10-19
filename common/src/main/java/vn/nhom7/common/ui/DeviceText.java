package vn.nhom7.common.ui;

import com.google.gson.JsonObject;

/** Tên loại thiết bị và câu mô tả trạng thái bằng tiếng Việt, dùng chung cho mọi giao diện. */
public final class DeviceText {

    private DeviceText() {
    }

    public static String label(String type) {
        if (type == null) {
            return "Thiết bị";
        }
        switch (type) {
            case "LIGHT":
                return "Đèn";
            case "TEMP_SENSOR":
                return "Cảm biến nhiệt";
            case "DOOR_LOCK":
                return "Khóa cửa";
            case "CAMERA":
                return "Camera";
            case "AIR_CONDITIONER":
                return "Máy lạnh";
            default:
                return type;
        }
    }

    /** Ví dụ: "Đang bật · 80%", "45.0 °C · NÓNG", "Đã khóa", "Đang ghi hình". */
    public static String summary(String type, JsonObject s) {
        if (s == null || s.size() == 0) {
            return "Chưa có dữ liệu";
        }
        switch (type == null ? "" : type) {
            case "LIGHT": {
                boolean on = "ON".equals(DeviceIcons.str(s, "power"));
                int b = (int) DeviceIcons.num(s, "brightness", 100);
                return on ? "Đang bật · " + b + "%" : "Đang tắt";
            }
            case "TEMP_SENSOR": {
                double t = DeviceIcons.num(s, "temperature", Double.NaN);
                if (Double.isNaN(t)) {
                    return "Chưa có dữ liệu";
                }
                return String.format("%.1f °C", t) + (t > 40 ? " · NÓNG" : "");
            }
            case "DOOR_LOCK": {
                String txt = DeviceIcons.bool(s, "locked") ? "Đã khóa" : "Đang mở khóa";
                return DeviceIcons.bool(s, "ringing") ? txt + " · Chuông reo" : txt;
            }
            case "CAMERA": {
                String txt = DeviceIcons.bool(s, "recording") ? "Đang ghi hình" : "Không ghi";
                return DeviceIcons.bool(s, "motion") ? txt + " · Có chuyển động" : txt;
            }
            case "AIR_CONDITIONER": {
                boolean on = "ON".equals(DeviceIcons.str(s, "power"));
                return on ? "Đang chạy · " + (int) DeviceIcons.num(s, "targetTemp", 24) + " °C" : "Đang tắt";
            }
            default:
                return s.toString();
        }
    }
}
