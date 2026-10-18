package vn.nhom7.dashboard;

import javax.swing.SwingUtilities;

/** Mở Dashboard. Có thể truyền ID qua tham số, mặc định dashboard1. */
public class Dashboard {

    public static void main(String[] args) {
        String id = (args.length > 0) ? args[0] : "dashboard1";
        SwingUtilities.invokeLater(() -> {
            DashboardFrame frame = new DashboardFrame(id);
            frame.setVisible(true);
            frame.connectToHub();
        });
    }
}