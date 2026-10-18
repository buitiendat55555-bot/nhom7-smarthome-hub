package vn.nhom7.dashboard;

import java.util.LinkedHashMap;
import java.util.Map;

/** Lệnh mà từng loại thiết bị hỗ trợ (theo docs/PROTOCOL.md mục 4). */
public final class DeviceActions {

    private DeviceActions() {
    }

    /** Trả về action -> nhãn nút. Rỗng nếu thiết bị không nhận lệnh (ví dụ cảm biến). */
    public static Map<String, String> forType(String deviceType) {
        Map<String, String> actions = new LinkedHashMap<>(); // giữ đúng thứ tự nút
        if (deviceType == null) {
            return actions;
        }
        switch (deviceType) {
            case "LIGHT":
                actions.put("ON", "Bật đèn");
                actions.put("OFF", "Tắt đèn");
                break;
            case "DOOR_LOCK":
                actions.put("LOCK", "Khóa cửa");
                actions.put("UNLOCK", "Mở khóa");
                break;
            case "CAMERA":
                actions.put("START_RECORDING", "Bắt đầu ghi");
                actions.put("STOP_RECORDING", "Dừng ghi");
                break;
            default:
                break; // TEMP_SENSOR và loại lạ: không có lệnh
        }
        return actions;
    }
}