package vn.nhom7.hub;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/** Xử lý một kết nối (thiết bị hoặc Dashboard) trên một luồng riêng. */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private PrintWriter out;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        String address = socket.getRemoteSocketAddress().toString();
        try (BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter writer = new PrintWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {

            this.out = writer;
            String line;
            // Mỗi message là một dòng JSON (theo docs/PROTOCOL.md)
            while ((line = in.readLine()) != null) {
                System.out.println("[HUB] Nhan tu " + address + ": " + line);
                send("{\"type\":\"ACK\",\"from\":\"hub\",\"payload\":{\"ok\":true,\"message\":\"Da nhan\"}}");
            }
        } catch (IOException e) {
            System.out.println("[HUB] Loi ket noi " + address + ": " + e.getMessage());
        } finally {
            System.out.println("[HUB] Ngat ket noi " + address);
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    /** synchronized để nhiều luồng không ghi chồng lên nhau. */
    public synchronized void send(String line) {
        if (out != null) {
            out.println(line);
        }
    }
}