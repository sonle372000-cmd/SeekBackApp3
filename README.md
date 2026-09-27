# SeekBack

App Android (Kotlin) hỗ trợ **Android 6.0 (API 23)** trở lên.

## Chức năng
Khi bấm nút **"TUA TỚI RỒI QUAY LẠI"**, app sẽ:
1. Tua bài hát đang phát (ở bất kỳ app nhạc nào đang mở: Spotify, YouTube Music, Zing MP3, NhacCuaTui...) tiến lên một số giây bạn nhập (mặc định 10 giây).
2. Sau một khoảng thời gian chờ bạn nhập (mặc định 5 giây), app tự động tua **quay lại đúng vị trí ban đầu**.

## Vì sao cần cấp quyền "Truy cập thông báo"?
Android không cho phép 1 app điều khiển trực tiếp app nhạc khác trừ khi nó đăng ký làm **Notification Listener** — đây là cách chuẩn để lấy `MediaSession` (phiên phát nhạc) đang hoạt động trên máy. App **không đọc nội dung thông báo**, chỉ dùng quyền này để lấy quyền điều khiển phát nhạc (`play/pause/seekTo`).

Cách cấp quyền:
1. Mở app SeekBack, bấm nút **"Mở cài đặt cấp quyền thông báo"**.
2. Tìm "SeekBack" trong danh sách và bật lên.
3. Quay lại app, mở một bài hát ở app nhạc bất kỳ rồi bấm nút tua.

## Cách mở project bằng Android Studio
1. Cài Android Studio (bản mới nhất).
2. `File > Open` và chọn thư mục `SeekBackApp` này.
3. Android Studio sẽ tự tải Gradle wrapper và đồng bộ project (cần internet lần đầu).
4. Cắm điện thoại (bật USB debugging) hoặc dùng máy ảo Android 6.0+ rồi bấm Run ▶.

## Cách lấy file APK
GitHub **không tự build APK khi bạn chỉ push code**. Có 2 cách lấy APK:

1. **Build local (nhanh):** mở project bằng Android Studio → menu `Build` → `Build Bundle(s) / APK(s)` → `Build APK(s)`. File nằm ở `app/build/outputs/apk/debug/app-debug.apk`.
2. **Để GitHub tự build (dùng GitHub Actions):** project này đã có sẵn file `.github/workflows/build-apk.yml`. Chỉ cần push code lên GitHub là nó tự chạy:
   - Vào tab **Actions** trên repo GitHub của bạn.
   - Chờ job "Build APK" chạy xong (vài phút).
   - Bấm vào job đó → kéo xuống mục **Artifacts** → tải file `SeekBack-debug-apk.zip` (bên trong là file `.apk`).

## Cách đưa lên GitHub
```bash
cd SeekBackApp
git init
git add .
git commit -m "Initial commit: SeekBack app"
git branch -M main
git remote add origin https://github.com/<ten-user>/<ten-repo>.git
git push -u origin main
```

## Cấu trúc project
```
SeekBackApp/
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/example/seekback/
│       │   ├── MainActivity.kt          # Giao diện + logic tua tới/tua lui
│       │   └── NotifListenerService.kt  # Service để lấy quyền điều khiển media session
│       └── res/
│           ├── layout/activity_main.xml
│           ├── values/ (strings, colors, styles)
│           └── mipmap-*/ (icon app)
├── build.gradle
├── settings.gradle
└── .gitignore
```

## Lưu ý
- minSdkVersion = 23 (Android 6.0 Marshmallow) như yêu cầu.
- Không cần quyền Internet hay quyền nguy hiểm (dangerous permission) nào khác ngoài quyền Notification Access (cấp thủ công trong Settings).
- Nếu ứng dụng nhạc bạn dùng không public `MediaSession` (hiếm gặp), app sẽ báo "Không tìm thấy bài hát nào đang phát".
