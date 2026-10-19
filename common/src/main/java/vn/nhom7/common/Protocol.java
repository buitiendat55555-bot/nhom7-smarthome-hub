package vn.nhom7.common;

public final class Protocol {

    public static final int PORT = 5000;
    public static final String DEFAULT_HOST = "127.0.0.1";
    public static final String HUB_ID = "hub";

    public static final int STATUS_INTERVAL_SECONDS = 5;
    public static final int OFFLINE_TIMEOUT_SECONDS = 15;
    public static final double TEMP_ALERT_THRESHOLD = 40.0;
    /** Nhiệt độ máy lạnh được Hub tự đặt khi cảm biến vượt ngưỡng. */
    public static final int AUTO_COOL_TEMP = 22;

    public static final String ROLE_DEVICE = "DEVICE";
    public static final String ROLE_DASHBOARD = "DASHBOARD";

    private Protocol() {
    }
}