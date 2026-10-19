package vn.nhom7.hub;

import com.google.gson.JsonObject;
import java.util.concurrent.atomic.AtomicInteger;
import vn.nhom7.common.Message;
import vn.nhom7.common.MessageType;
import vn.nhom7.common.Protocol;

/**
 * Quy tắc tự động hóa chạy ngay trên Hub: khi một thiết bị gửi STATUS thỏa điều kiện,
 * Hub tự gửi COMMAND tới thiết bị khác mà không cần người bấm trên Dashboard.
 *  1. Nhiệt độ vượt 40 °C           -> bật mọi máy lạnh ở 22 °C.
 *  2. Có chuyển động khi cửa đang khóa -> bật mọi đèn, mọi camera bắt đầu ghi hình.
 *  3. Có khách bấm chuông          -> mọi camera bắt đầu ghi hình.
 */
public class AutomationService {

    private final DeviceRegistry registry;
    private final AlertService alerts;
    private final AtomicInteger counter = new AtomicInteger(); // mã lệnh auto-1, auto-2...
    private volatile boolean enabled = true; // Dashboard bật/tắt bằng COMMAND gửi tới "hub"

    public AutomationService(DeviceRegistry registry, AlertService alerts) {
        this.registry = registry;
        this.alerts = alerts;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean value) {
        enabled = value;
        ActivityLogger.log("Tu dong hoa: " + (value ? "BAT" : "TAT"));
    }

    /** Gọi sau mỗi STATUS, cùng chỗ với AlertService.check. So sánh trước/sau để chỉ chạy một lần. */
    public void check(DeviceInfo d, JsonObject before, JsonObject after) {
        if (!enabled) {
            return;
        }
        switch (d.getType() == null ? "" : d.getType()) {
            case "TEMP_SENSOR": {
                double limit = Protocol.TEMP_ALERT_THRESHOLD;
                boolean wasHot = AlertService.number(before, "temperature") > limit;
                boolean isHot = AlertService.number(after, "temperature") > limit;
                if (isHot && !wasHot) {
                    int n = sendToType("AIR_CONDITIONER", "SET_TEMP", Protocol.AUTO_COOL_TEMP);
                    if (n > 0) {
                        alerts.send(d.getId(), "INFO", "Tự động: nhiệt độ cao → bật " + n
                                + " máy lạnh ở " + Protocol.AUTO_COOL_TEMP + " °C");
                    }
                }
                break;
            }
            case "CAMERA": {
                boolean motionStart = AlertService.bool(after, "motion") && !AlertService.bool(before, "motion");
                if (motionStart && alerts.anyDoorLocked()) {
                    int lights = sendToType("LIGHT", "ON", null);
                    int cams = sendToType("CAMERA", "START_RECORDING", null);
                    alerts.send(d.getId(), "INFO", "Tự động: có chuyển động khi cửa khóa → bật "
                            + lights + " đèn, " + cams + " camera ghi hình");
                }
                break;
            }
            case "DOOR_LOCK": {
                if (AlertService.bool(after, "ringing") && !AlertService.bool(before, "ringing")) {
                    int cams = sendToType("CAMERA", "START_RECORDING", null);
                    if (cams > 0) {
                        alerts.send(d.getId(), "INFO", "Tự động: có khách → " + cams + " camera bắt đầu ghi hình");
                    }
                }
                break;
            }
            default:
                break;
        }
    }

    /** Gửi một lệnh tới mọi thiết bị cùng loại. Trả về số thiết bị đã gửi. */
    private int sendToType(String type, String action, Integer value) {
        int count = 0;
        for (DeviceInfo x : registry.getDevices()) {
            if (type.equals(x.getType())) {
                JsonObject p = new JsonObject();
                String id = "auto-" + counter.incrementAndGet();
                p.addProperty("commandId", id);
                p.addProperty("action", action);
                if (value != null) {
                    p.addProperty("value", value);
                }
                x.getHandler().send(new Message(MessageType.COMMAND, Protocol.HUB_ID, x.getId(), p));
                System.out.println("[HUB] Tu dong gui " + action + " toi " + x.getId());
                ActivityLogger.log("Tu dong: lenh " + action + (value != null ? " " + value : "")
                        + " (" + id + ") toi " + x.getId());
                count++;
            }
        }
        return count;
    }
}
