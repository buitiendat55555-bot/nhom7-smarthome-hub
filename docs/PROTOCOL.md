# Đặc tả giao thức Smart Home Hub

## 1. Quy ước chung

- Giao thức tầng vận chuyển: TCP, Hub lắng nghe cổng **5000**.
- Mỗi message là **một object JSON nằm trên một dòng**, kết thúc bằng ký tự xuống dòng `\n`, mã hóa UTF-8.
- Có 2 loại client kết nối tới Hub: **thiết bị** (DEVICE) và **bảng điều khiển** (DASHBOARD).
- Thiết bị và Dashboard không nói chuyện trực tiếp với nhau; mọi message đều đi qua Hub (mô hình Hub-and-Spoke).

## 2. Cấu trúc message

| Trường | Kiểu | Bắt buộc | Ý nghĩa |
| --- | --- | --- | --- |
| `type` | String | Có | Loại message (xem mục 3) |
| `from` | String | Có | ID bên gửi, ví dụ `light1`, `dashboard1`, `hub` |
| `to` | String | Không | ID bên nhận; bỏ trống khi gửi cho Hub |
| `timestamp` | Long | Có | Thời điểm gửi, tính bằng mili giây (epoch) |
| `payload` | Object | Có | Dữ liệu riêng của từng loại message |

Ví dụ:

```json
{"type":"STATUS","from":"light1","timestamp":1760670000000,"payload":{"power":"ON"}}
```

## 3. Các loại message

| type | Chiều gửi | Khi nào gửi | payload |
| --- | --- | --- | --- |
| `REGISTER` | Client → Hub | Ngay sau khi kết nối | `role`, `deviceType`, `name` |
| `ACK` | Hub → Client, Thiết bị → Hub → Dashboard | Xác nhận đăng ký hoặc kết quả thực thi lệnh | `ok`, `message`, `commandId` (nếu có) |
| `STATUS` | Thiết bị → Hub | Định kỳ 5 giây và mỗi khi trạng thái đổi | Trạng thái thiết bị (mục 4) |
| `COMMAND` | Dashboard → Hub → Thiết bị | Người dùng bấm nút điều khiển | `commandId`, `action` |
| `DEVICE_LIST` | Hub → Dashboard | Khi Dashboard vừa đăng ký | `devices`: danh sách thiết bị và trạng thái |
| `DEVICE_UPDATE` | Hub → Dashboard | Khi nhận `STATUS` mới từ thiết bị | `deviceId`, `state` |
| `DEVICE_OFFLINE` | Hub → Dashboard | Thiết bị ngắt kết nối hoặc 15 giây không gửi gì | `deviceId` |
| `ALERT` | Hub → Dashboard | Dữ liệu cảm biến vượt ngưỡng | `deviceId`, `level`, `message` |
| `ERROR` | Hub → Client | Message sai định dạng hoặc gửi tới thiết bị không tồn tại | `message` |

## 4. Loại thiết bị, trạng thái và lệnh

| deviceType | Trạng thái gửi trong STATUS | Lệnh (action) nhận được |
| --- | --- | --- |
| `LIGHT` | `power`: `ON` / `OFF` | `ON`, `OFF` |
| `TEMP_SENSOR` | `temperature`: số thực (°C) | Không có |
| `DOOR_LOCK` | `locked`: `true` / `false` | `LOCK`, `UNLOCK` |
| `CAMERA` | `recording`: `true` / `false`, `motion`: `true` / `false` | `START_RECORDING`, `STOP_RECORDING` |

**Ngưỡng cảnh báo:** `TEMP_SENSOR` có `temperature` > 40 °C → Hub gửi `ALERT` mức `WARNING`. `CAMERA` có `motion` = `true` khi đang khóa cửa → Hub gửi `ALERT` mức `INFO`.