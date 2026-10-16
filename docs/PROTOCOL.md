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