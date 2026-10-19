package vn.nhom7.device;

import vn.nhom7.common.ui.DeviceText;

/** Tạo cửa sổ thiết bị theo loại. Dùng cho trình khởi chạy. */
public final class DeviceFactory {

    public static final String[] TYPES = {"LIGHT", "TEMP_SENSOR", "DOOR_LOCK", "CAMERA", "AIR_CONDITIONER"};

    private DeviceFactory() {
    }

    /** Tên tiếng Việt hiện trên giao diện. */
    public static String label(String type) {
        return DeviceText.label(type);
    }

    /** Tiền tố để gợi ý ID: light1, temp1, door1, cam1, ac1. */
    public static String idPrefix(String type) {
        switch (type) {
            case "LIGHT":
                return "light";
            case "TEMP_SENSOR":
                return "temp";
            case "DOOR_LOCK":
                return "door";
            case "CAMERA":
                return "cam";
            case "AIR_CONDITIONER":
                return "ac";
            default:
                return "dev";
        }
    }

    /** Tên mặc định cho bộ demo. */
    public static String demoName(String type) {
        switch (type) {
            case "LIGHT":
                return "Đèn phòng khách";
            case "TEMP_SENSOR":
                return "Cảm biến phòng khách";
            case "DOOR_LOCK":
                return "Cửa chính";
            case "CAMERA":
                return "Camera sân trước";
            case "AIR_CONDITIONER":
                return "Máy lạnh phòng khách";
            default:
                return type;
        }
    }

    /** Tạo cửa sổ thiết bị (chưa hiện, chưa kết nối). */
    public static DeviceFrame create(String type, String id, String name) {
        switch (type) {
            case "LIGHT":
                return new LightFrame(new LightDevice(id, name));
            case "TEMP_SENSOR":
                return new TempSensorFrame(new TempSensorDevice(id, name));
            case "DOOR_LOCK":
                return new DoorLockFrame(new DoorLockDevice(id, name));
            case "CAMERA":
                return new CameraFrame(new CameraDevice(id, name));
            case "AIR_CONDITIONER":
                return new AirConditionerFrame(new AirConditionerDevice(id, name));
            default:
                throw new IllegalArgumentException("Loai thiet bi khong ho tro: " + type);
        }
    }
}
