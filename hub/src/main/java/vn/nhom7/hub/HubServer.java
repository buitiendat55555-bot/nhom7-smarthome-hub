package vn.nhom7.hub;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import vn.nhom7.common.Protocol;

/** Hub trung tâm: lắng nghe cổng, mỗi kết nối giao cho một luồng trong thread pool. */
public class HubServer {

    private final int port;
    private final DeviceRegistry registry;
    private final ExecutorService pool = Executors.newFixedThreadPool(20);
    private final ScheduledExecutorService watchdog = Executors.newSingleThreadScheduledExecutor();
    private volatile boolean running;

    public HubServer(int port, DeviceRegistry registry) {
        this.port = port;
        this.registry = registry;
    }

    public void start() throws IOException {
        running = true;
        // Mỗi 5 giây kiểm tra thiết bị nào im lặng quá lâu
        watchdog.scheduleAtFixedRate(this::checkTimeouts, 5, 5, TimeUnit.SECONDS);
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[HUB] Dang lang nghe cong " + port);
            while (running) {
                Socket socket = serverSocket.accept(); // chờ thiết bị kết nối
                System.out.println("[HUB] Ket noi moi tu " + socket.getRemoteSocketAddress());
                pool.execute(new ClientHandler(socket, registry)); // mọi luồng dùng chung một registry
            }
        } finally {
            watchdog.shutdownNow();
            pool.shutdown();
        }
    }

    /** Thiết bị quá OFFLINE_TIMEOUT_SECONDS không gửi STATUS thì bị ngắt kết nối. */
    private void checkTimeouts() {
        try {
            long limit = Protocol.OFFLINE_TIMEOUT_SECONDS * 1000L;
            long now = System.currentTimeMillis();
            for (DeviceInfo d : registry.getDevices()) {
                if (now - d.getLastSeen() > limit) {
                    System.out.println("[HUB] " + d.getId() + " qua " + Protocol.OFFLINE_TIMEOUT_SECONDS
                            + " giay khong gui STATUS -> ngat ket noi");
                    d.getHandler().close();
                }
            }
        } catch (RuntimeException e) {
            // Nếu không bắt, một lỗi sẽ làm luồng kiểm tra dừng hẳn
            System.out.println("[HUB] Loi khi kiem tra timeout: " + e.getMessage());
        }
    }
}