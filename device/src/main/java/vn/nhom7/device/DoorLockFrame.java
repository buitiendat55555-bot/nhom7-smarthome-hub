package vn.nhom7.device;

import vn.nhom7.common.ui.Ui;

/** Cửa sổ khóa cửa: khóa/mở ngay tại cửa và nút chuông cho khách. */
public class DoorLockFrame extends DeviceFrame {

    private final DoorLockDevice door;
    private final Ui.RoundButton toggleBtn = new Ui.RoundButton("", Ui.GREEN);

    public DoorLockFrame(DoorLockDevice door) {
        super(door);
        this.door = door;
        toggleBtn.addActionListener(e -> door.toggle());
        Ui.RoundButton bell = new Ui.RoundButton("Khách bấm chuông", Ui.AMBER);
        bell.addActionListener(e -> door.ring());
        setControls(column(toggleBtn, bell));
    }

    @Override
    protected void onStateChanged() {
        if (door == null) {
            return;
        }
        toggleBtn.setText(door.isLocked() ? "Mở khóa cửa" : "Khóa cửa");
        toggleBtn.setColor(door.isLocked() ? Ui.ORANGE : Ui.GREEN);
    }
}
