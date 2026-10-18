package vn.nhom7.device;

import com.google.gson.JsonObject;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/** Camera mô phỏng: ghi hình (recording) và phát hiện chuyển động (motion). */
public class CameraDevice extends DeviceClient {

    public static final int MOTION_SECONDS = 5;

    private volatile boolean recording;
    private volatile boolean motion;
    private Runnable onChange = () -> { };

    // Luồng hẹn giờ tắt "chuyển động" sau MOTION_SECONDS giây; daemon để không giữ chương trình chạy
    private final ScheduledExecutorService motionTimer = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "motion-timer");
        t.setDaemon(true);
        return t;
    });
    private ScheduledFuture<?> motionReset;

    public CameraDevice(String deviceId, String name) {
        super(deviceId, "CAMERA", name);
    }

    public void setOnChange(Runnable listener) {
        this.onChange = (listener != null) ? listener : () -> { };
    }

    public String getDeviceId() {
        return deviceId;
    }

    public boolean isRecording() {
        return recording;
    }

    public boolean isMotion() {
        return motion;
    }

    /** Người dùng bấm nút ghi ngay trên camera. */
    public void toggleRecording() {
        recording = !recording;
        onChange.run();
        sendStatus();
    }

    /** Giả lập có người đi qua: motion = true trong MOTION_SECONDS giây rồi tự tắt. */
    public synchronized void triggerMotion() {
        motion = true;
        onChange.run();
        sendStatus();
        if (motionReset != null) {
            motionReset.cancel(false); // bấm liên tiếp thì tính lại từ đầu
        }
        motionReset = motionTimer.schedule(() -> {
            motion = false;
            onChange.run();
            sendStatus();
        }, MOTION_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    protected JsonObject getState() {
        JsonObject s = new JsonObject();
        s.addProperty("recording", recording);
        s.addProperty("motion", motion);
        return s;
    }

    @Override
    protected String executeCommand(String action) {
        if ("START_RECORDING".equals(action)) {
            recording = true;
            onChange.run();
            return "Da bat dau ghi hinh";
        }
        if ("STOP_RECORDING".equals(action)) {
            recording = false;
            onChange.run();
            return "Da dung ghi hinh";
        }
        throw new IllegalArgumentException("Lenh khong ho tro: " + action);
    }
}