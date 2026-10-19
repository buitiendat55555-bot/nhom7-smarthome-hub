package vn.nhom7.device;

import javax.swing.SwingUtilities;
import vn.nhom7.common.ui.Ui;

/** Mở một đèn. Có thể truyền ID và tên qua tham số, mặc định light1. */
public class LightApp {

    public static void main(String[] args) {
        String id = (args.length > 0) ? args[0] : "light1";
        String name = (args.length > 1) ? args[1] : "Đèn phòng khách";

        SwingUtilities.invokeLater(() -> {
            Ui.install();
            LightFrame frame = new LightFrame(new LightDevice(id, name));
            frame.setVisible(true);
            frame.connectToHub();
        });
    }
}