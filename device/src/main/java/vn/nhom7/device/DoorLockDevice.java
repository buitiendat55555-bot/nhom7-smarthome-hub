package vn.nhom7.device;

import com.google.gson.JsonObject;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/** Khóa cửa thông minh: trạng thái locked, nhận lệnh LOCK/UNLOCK, có chuông cửa. */
public class DoorLockDevice extends DeviceClient {

    public static final int RING_SECONDS = 4;

    private volatile boolean locked = true;
    private volatile boolean ringing;
    private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "doorbell-timer");
        t.setDaemon(true);
        return t;
    });
    private ScheduledFuture<?> ringReset;

    public DoorLockDevice(String deviceId, String name) {
        super(deviceId, "DOOR_LOCK", name);
    }

    public boolean isLocked() {
        return locked;
    }

    /** Người dùng bấm nút ngay trên khóa: đổi trạng thái và báo Hub luôn. */
    public void toggle() {
        locked = !locked;
        stateChanged();
        sendStatus();
    }

    /** Khách bấm chuông: ringing = true trong RING_SECONDS giây; Hub sẽ báo lên Dashboard. */
    public synchronized void ring() {
        ringing = true;
        stateChanged();
        sendStatus();
        if (ringReset != null) {
            ringReset.cancel(false);
        }
        ringReset = timer.schedule(() -> {
            ringing = false;
            stateChanged();
            sendStatus();
        }, RING_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    protected JsonObject getState() {
        JsonObject s = new JsonObject();
        s.addProperty("locked", locked);
        s.addProperty("ringing", ringing);
        return s;
    }

    @Override
    protected String executeCommand(String action) {
        if ("LOCK".equals(action)) {
            locked = true;
            stateChanged();
            return "Da khoa cua";
        }
        if ("UNLOCK".equals(action)) {
            locked = false;
            stateChanged();
            return "Da mo khoa";
        }
        throw new IllegalArgumentException("Lenh khong ho tro: " + action);
    }
}
