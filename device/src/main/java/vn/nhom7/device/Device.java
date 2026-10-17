package vn.nhom7.device;

import com.google.gson.JsonObject;

/** Chạy thử DeviceClient bằng một đèn đơn giản (chưa có giao diện). */
public class Device {

    public static void main(String[] args) throws Exception {
        DeviceClient demo = new DeviceClient("light1", "LIGHT", "Den thu nghiem") {
            private boolean on = false;

            @Override
            protected JsonObject getState() {
                JsonObject s = new JsonObject();
                s.addProperty("power", on ? "ON" : "OFF");
                return s;
            }

            @Override
            protected String executeCommand(String action) {
                if ("ON".equals(action)) {
                    on = true;
                    return "Da bat den";
                }
                if ("OFF".equals(action)) {
                    on = false;
                    return "Da tat den";
                }
                throw new IllegalArgumentException("Lenh khong ho tro: " + action);
            }
        };

        demo.connect();
        Thread.sleep(30_000); // giữ chương trình 30 giây để thấy STATUS gửi mỗi 5 giây
        demo.disconnect();
    }
}