package vn.nhom7.hub;

import vn.nhom7.common.Protocol;

public class Hub {

    public static void main(String[] args) throws Exception {
        new HubServer(Protocol.PORT, new DeviceRegistry()).start();
    }
}