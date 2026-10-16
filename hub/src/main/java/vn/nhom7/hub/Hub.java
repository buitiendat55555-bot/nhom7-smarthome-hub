package vn.nhom7.hub;

public class Hub {

    public static void main(String[] args) throws Exception {
        new HubServer(5000).start();
    }
}