package vn.nhom7.device;

import javax.swing.JFrame;

/** Tạo cửa sổ thiết bị theo loại. Dùng cho trình khởi chạy. */
public final class DeviceFactory {

    public static final String[] TYPES = {"LIGHT", "TEMP_SENSOR", "DOOR_LOCK", "CAMERA"};

    private DeviceFactory() {
    }

    /** Tên tiếng Việt hiện trên giao diện. */
    public static String label(String type) {
        switch (type) {
            case "LIGHT":
                return "Đèn";
            case "TEMP_SENSOR":
                return "Cảm biến nhiệt";
            case "DOOR_LOCK":
                return "Khóa cửa";
            case "CAMERA":
                return "Camera";
            default:
                return type;
        }
    }

    /** Tiền tố để gợi ý ID: light1, temp1, door1, cam1. */
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
            default:
                return "dev";
        }
    }

    /** Tạo cửa sổ thiết bị (chưa hiện, chưa kết nối). */
    public static JFrame create(String type, String id, String name) {
        switch (type) {
            case "LIGHT":
                return new LightFrame(new LightDevice(id, name));
            case "TEMP_SENSOR":
                return new TempSensorFrame(new TempSensorDevice(id, name));
            case "DOOR_LOCK":
                return new DoorLockFrame(new DoorLockDevice(id, name));
            case "CAMERA":
                return new CameraFrame(new CameraDevice(id, name));
            default:
                throw new IllegalArgumentException("Loai thiet bi khong ho tro: " + type);
        }
    }

    /** Kết nối Hub cho cửa sổ vừa tạo. */
    public static void connect(JFrame frame) {
        if (frame instanceof LightFrame f) {
            f.connectToHub();
        } else if (frame instanceof TempSensorFrame f) {
            f.connectToHub();
        } else if (frame instanceof DoorLockFrame f) {
            f.connectToHub();
        } else if (frame instanceof CameraFrame f) {
            f.connectToHub();
        }
    }
}