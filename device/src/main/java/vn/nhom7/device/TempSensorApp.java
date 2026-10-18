package vn.nhom7.device;

import javax.swing.SwingUtilities;

/** Mở một cảm biến nhiệt. Mặc định ID temp1. */
public class TempSensorApp {

    public static void main(String[] args) {
        String id = (args.length > 0) ? args[0] : "temp1";
        String name = (args.length > 1) ? args[1] : "Cảm biến phòng khách";
        SwingUtilities.invokeLater(() -> {
            TempSensorFrame frame = new TempSensorFrame(new TempSensorDevice(id, name));
            frame.setVisible(true);
            frame.connectToHub();
        });
    }
}