package vn.nhom7.device;

import javax.swing.SwingUtilities;
import vn.nhom7.common.ui.Ui;

/** Mở một máy lạnh. Mặc định ID ac1. */
public class AirConditionerApp {

    public static void main(String[] args) {
        String id = (args.length > 0) ? args[0] : "ac1";
        String name = (args.length > 1) ? args[1] : "Máy lạnh phòng khách";
        SwingUtilities.invokeLater(() -> {
            Ui.install();
            AirConditionerFrame frame = new AirConditionerFrame(new AirConditionerDevice(id, name));
            frame.setVisible(true);
            frame.connectToHub();
        });
    }
}
