package vn.nhom7.common;

/** 9 loại message theo mục 3 của docs/PROTOCOL.md. */
public enum MessageType {
    REGISTER,       // Client -> Hub: đăng ký khi vừa kết nối
    ACK,            // Xác nhận đăng ký hoặc kết quả thực thi lệnh
    STATUS,         // Thiết bị -> Hub: trạng thái định kỳ
    COMMAND,        // Dashboard -> Hub -> Thiết bị: lệnh điều khiển
    DEVICE_LIST,    // Hub -> Dashboard: danh sách thiết bị
    DEVICE_UPDATE,  // Hub -> Dashboard: trạng thái mới của một thiết bị
    DEVICE_OFFLINE, // Hub -> Dashboard: thiết bị mất kết nối
    ALERT,          // Hub -> Dashboard: cảnh báo vượt ngưỡng
    ERROR           // Hub -> Client: message sai hoặc không xử lý được
}