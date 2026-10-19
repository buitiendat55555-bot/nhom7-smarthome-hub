package vn.nhom7.device;

import vn.nhom7.common.ui.Ui;

/** Cửa sổ camera: bật/dừng ghi hình và giả lập có người đi qua. */
public class CameraFrame extends DeviceFrame {

    private final CameraDevice camera;
    private final Ui.RoundButton recordBtn = new Ui.RoundButton("", Ui.RED);

    public CameraFrame(CameraDevice camera) {
        super(camera);
        this.camera = camera;
        recordBtn.addActionListener(e -> camera.toggleRecording());
        Ui.RoundButton motionBtn = new Ui.RoundButton("Giả lập có người đi qua", Ui.AMBER);
        motionBtn.addActionListener(e -> camera.triggerMotion());
        setControls(column(recordBtn, motionBtn));
    }

    @Override
    protected void onStateChanged() {
        if (camera == null) {
            return;
        }
        recordBtn.setText(camera.isRecording() ? "Dừng ghi hình" : "Bắt đầu ghi hình");
        recordBtn.setColor(camera.isRecording() ? Ui.NAVY : Ui.RED);
    }
}
