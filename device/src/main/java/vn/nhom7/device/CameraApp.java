package vn.nhom7.device;

import javax.swing.SwingUtilities;
import vn.nhom7.common.ui.Ui;

/** Mở một camera. Mặc định ID cam1. */
public class CameraApp {

    public static void main(String[] args) {
        String id = (args.length > 0) ? args[0] : "cam1";
        String name = (args.length > 1) ? args[1] : "Camera cửa trước";
        SwingUtilities.invokeLater(() -> {
            Ui.install();
            CameraFrame frame = new CameraFrame(new CameraDevice(id, name));
            frame.setVisible(true);
            frame.connectToHub();
        });
    }
}