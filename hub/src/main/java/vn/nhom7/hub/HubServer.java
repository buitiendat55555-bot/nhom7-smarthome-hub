package vn.nhom7.hub;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Hub trung tâm: lắng nghe cổng, mỗi kết nối giao cho một luồng trong thread pool. */
public class HubServer {

    private final int port;
    private final ExecutorService pool = Executors.newFixedThreadPool(20);
    private volatile boolean running;

    public HubServer(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        running = true;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[HUB] Dang lang nghe cong " + port);
            while (running) {
                Socket socket = serverSocket.accept(); // chờ thiết bị kết nối
                System.out.println("[HUB] Ket noi moi tu " + socket.getRemoteSocketAddress());
                pool.execute(new ClientHandler(socket));
            }
        } finally {
            pool.shutdown();
        }
    }
}