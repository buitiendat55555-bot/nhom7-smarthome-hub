package vn.nhom7.device;

import javax.swing.SwingUtilities;
import vn.nhom7.common.ui.Ui;

/** Mở một khóa cửa. Mặc định ID door1. */
public class DoorLockApp {

    public static void main(String[] args) {
        String id = (args.length > 0) ? args[0] : "door1";
        String name = (args.length > 1) ? args[1] : "Khóa cửa chính";
        SwingUtilities.invokeLater(() -> {
            Ui.install();
            DoorLockFrame frame = new DoorLockFrame(new DoorLockDevice(id, name));
            frame.setVisible(true);
            frame.connectToHub();
        });
    }
}