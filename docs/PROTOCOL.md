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

## 5. Ví dụ luồng bật đèn

1. Đèn kết nối và đăng ký:

```json
{"type":"REGISTER","from":"light1","timestamp":1760670000000,"payload":{"role":"DEVICE","deviceType":"LIGHT","name":"Đèn phòng khách"}}
```

2. Hub xác nhận:

```json
{"type":"ACK","from":"hub","to":"light1","timestamp":1760670000050,"payload":{"ok":true,"message":"Đăng ký thành công"}}
```

3. Dashboard gửi lệnh bật đèn:

```json
{"type":"COMMAND","from":"dashboard1","to":"light1","timestamp":1760670010000,"payload":{"commandId":"c-001","action":"ON"}}
```

4. Đèn thực thi, trả kết quả (Hub chuyển tiếp cho Dashboard):

```json
{"type":"ACK","from":"light1","to":"dashboard1","timestamp":1760670010100,"payload":{"commandId":"c-001","ok":true,"message":"Đã bật đèn"}}
```

5. Đèn gửi trạng thái mới, Hub đẩy cho Dashboard:

```json
{"type":"DEVICE_UPDATE","from":"hub","to":"dashboard1","timestamp":1760670010200,"payload":{"deviceId":"light1","state":{"power":"ON"}}}
```

## 6. Ví dụ cảnh báo nhiệt độ

```json
{"type":"ALERT","from":"hub","to":"dashboard1","timestamp":1760670020000,"payload":{"deviceId":"temp1","level":"WARNING","message":"Nhiệt độ 45.2°C vượt ngưỡng 40°C"}}
```