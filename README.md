# Hệ thống Quản lý Thiết bị Smart Home qua TCP (Hub-and-Spoke)

Đồ án cuối kỳ học phần **Lập trình mạng máy tính** – Nhóm 7 – Lớp 23DTHA7.

Một **Hub Server** trung tâm quản lý nhiều thiết bị mô phỏng (đèn, cảm biến nhiệt độ, khóa cửa, camera) kết nối đồng thời qua **TCP Socket**. Người dùng giám sát và điều khiển thiết bị qua **Dashboard**, nhận cảnh báo khi cảm biến vượt ngưỡng. Mọi thông điệp dùng một **giao thức JSON tự định nghĩa** (mỗi message là một dòng JSON).

## Thành viên

| MSSV | Họ và tên | Tài khoản GitHub | Vai trò |
| --- | --- | --- | --- |
| 2380600407 | Bùi Tiến Đạt | `buitiendat55555-bot` | Nhóm trưởng – Hub Server |
| 2380600767 | Võ Minh Huân | `huanti502` | Client thiết bị |
| 2380600998 | Nguyễn Nhật Minh Khang | `mk1231321` | Giao thức dùng chung, Dashboard |

## Kiến trúc

```
              ┌──────────────────────────────┐
              │          HUB SERVER          │
              │  ServerSocket :5000          │
              │  Thread pool (ClientHandler) │
              │  DeviceRegistry, AlertService│
              │  ActivityLogger -> hub.log   │
              └──────┬───────────────┬───────┘
         TCP + JSON  │               │  TCP + JSON
       ┌─────────────┼───────┐       │
       │             │       │       │
   ┌───┴───┐ ┌───────┴┐ ┌────┴───┐ ┌─┴─────────┐
   │  Đèn  │ │Cảm biến│ │Khóa cửa│ │ Dashboard │
   │light1 │ │ temp1  │ │ door1  │ │dashboard1 │
   └───────┘ └────────┘ └────────┘ └───────────┘
           (Camera cam1 kết nối tương tự)
```

Thiết bị và Dashboard **không nói chuyện trực tiếp** với nhau; mọi lệnh và trạng thái đều đi qua Hub.

## Cấu trúc project (Maven, 4 module)

| Module | Package | Nội dung |
| --- | --- | --- |
| `common` | `vn.nhom7.common` | `Protocol` (hằng số), `MessageType`, `Message`, `JsonUtil` |
| `hub` | `vn.nhom7.hub` | `HubServer`, `ClientHandler`, `DeviceRegistry`, `DeviceInfo`, `AlertService`, `ActivityLogger` |
| `device` | `vn.nhom7.device` | `DeviceClient` (lớp cơ sở), Đèn, Cảm biến nhiệt, Khóa cửa, Camera, `DeviceLauncher` |
| `dashboard` | `vn.nhom7.dashboard` | `HubConnection`, `DeviceTableModel`, `DashboardFrame`, `HistoryDialog`, `DashboardLog` |

Đặc tả giao thức đầy đủ: [`docs/PROTOCOL.md`](docs/PROTOCOL.md).

## Chức năng

**Hub Server**
- Lắng nghe cổng 5000, mỗi kết nối chạy trên một luồng của thread pool.
- Bảng thiết bị dùng `ConcurrentHashMap`, chặn trùng ID.
- Chuyển `COMMAND` từ Dashboard tới đúng thiết bị, chuyển `ACK` về đúng Dashboard đã ra lệnh.
- Đẩy `DEVICE_LIST`, `DEVICE_UPDATE`, `DEVICE_OFFLINE` cho Dashboard.
- Ngắt thiết bị quá 15 giây không gửi `STATUS`.
- Cảnh báo (`ALERT`) khi nhiệt độ vượt 40 °C, khi trở lại bình thường, khi camera phát hiện chuyển động lúc cửa đang khóa.
- Ghi log hoạt động ra `logs/hub.log`.

**Thiết bị**
- Kết nối, gửi `REGISTER`, gửi `STATUS` mỗi 5 giây, nhận lệnh và trả `ACK`.
- Tự kết nối lại Hub mỗi 3 giây khi Hub bị tắt.
- Giao diện Swing riêng cho từng loại thiết bị; trình khởi chạy mở nhiều thiết bị cùng lúc.

**Dashboard**
- Bảng thiết bị cập nhật realtime (thiết bị mất kết nối hiện màu xám).
- Khu vực điều khiển: nút lệnh hiện theo loại thiết bị đang chọn.
- Tab Cảnh báo (kêu bíp, đếm cảnh báo chưa xem), nhật ký sự kiện, lịch sử log lưu ra file và lọc theo từ khóa.

## Cách chạy

**Yêu cầu:** JDK 17 trở lên, Apache NetBeans (hoặc Maven 3.8+).

**Bằng NetBeans**
1. `File → Open Project` → chọn thư mục repo.
2. Chuột phải `smarthome-hub` → **Clean and Build**.
3. Chạy theo thứ tự:
   1. `hub` → `Hub.java` → **Run File**.
   2. `dashboard` → `Dashboard.java` → **Run File**.
   3. Chuột phải module `device` → **Run** → bấm **"Mở bộ demo"** (mở `light1`, `temp1`, `door1`, `cam1`).

**Bằng dòng lệnh** (mỗi lệnh `exec` mở trong một cửa sổ riêng)

```
mvn clean install
mvn -pl hub exec:java
mvn -pl dashboard exec:java
mvn -pl device exec:java
```

## Kịch bản kiểm tra nhanh

| Thao tác | Kết quả mong đợi |
| --- | --- |
| Dashboard chọn `light1` → **Bật đèn** | Đèn sáng, bảng ghi `power = ON`, nhật ký "Thành công lệnh c-1" |
| Kéo cảm biến lên 45 °C | Dashboard kêu bíp, tab Cảnh báo có dòng **WARNING** |
| Kéo cảm biến xuống 30 °C | Cảnh báo **INFO** "đã trở lại bình thường" |
| Cửa đang khóa, camera bấm **Mô phỏng chuyển động** | Cảnh báo **INFO** |
| Đóng cửa sổ một thiết bị | Dòng thiết bị trên Dashboard chuyển xám "Mất kết nối" |
| Tắt Hub rồi chạy lại | Các thiết bị tự kết nối lại sau khoảng 3 giây |
| Dashboard → **Lịch sử log**, lọc `temp1` | Chỉ còn các dòng liên quan `temp1` |

## Quy trình làm việc nhóm

- Mỗi việc là một **Issue** gán đúng 1 người, có Label và Milestone theo ngày.
- Nhánh `main` được bảo vệ: chỉ merge qua Pull Request, cần 1 Approve của thành viên khác.
- Tên nhánh: `feature/<MSSV>-issue-<số>-<tên-ngắn>`; commit theo Conventional Commits (`feat`, `fix`, `refactor`, `docs`).
- Review chéo: Đạt review Huân, Huân review Khang, Khang review Đạt.

## Bảng đóng góp

| MSSV | Họ và tên | Công việc đã thực hiện | Issue | Pull Requests |
| --- | --- | --- | --- | --- |
| 2380600407 | Bùi Tiến Đạt | Khung project Maven và `.gitignore`; Hub Server đa luồng; `DeviceRegistry`; định tuyến `COMMAND`/`ACK`, đẩy trạng thái cho Dashboard, phát hiện thiết bị mất kết nối; cảnh báo vượt ngưỡng và ghi log hoạt động; README | #1, #4, #6, #8, #12, #16 | #17, #19, #23, #24, #31, #32 |
| 2380600767 | Võ Minh Huân | Lớp `DeviceClient` cơ sở; Đèn thông minh; Cảm biến nhiệt độ và Khóa cửa; Camera mô phỏng và tự kết nối lại; trình khởi chạy nhiều thiết bị | #5, #7, #10, #13, #15 | #20, #22, #27, #28, #29 |
| 2380600998 | Nguyễn Nhật Minh Khang | Đặc tả giao thức JSON; lớp `Message`, `MessageType`, `JsonUtil`; Dashboard kết nối Hub và bảng thiết bị realtime; nút điều khiển; khu vực cảnh báo và lịch sử log | #2, #3, #9, #11, #14 | #18, #21, #25, #26, #30 |
