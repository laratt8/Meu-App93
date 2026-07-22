package com.example.remoteserver;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Collections;

public class ScreenCaptureService extends Service {
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private WebSocketServer server;
    private Handler handler = new Handler(Looper.getMainLooper());
    private int width, height, density;
    private static final int SERVER_PORT = 8080;
    private static final int FPS = 10;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metrics);
        
        width = metrics.widthPixels;
        height = metrics.heightPixels;
        density = metrics.densityDpi;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification = new Notification.Builder(this, "screen_channel")
            .setContentTitle("Remote Server")
            .setContentText("Servidor ativo - Porta 8080")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .build();
        
        startForeground(1, notification);

        int resultCode = intent.getIntExtra("resultCode", -1);
        Intent data = intent.getParcelableExtra("data");
        
        MediaProjectionManager mgr = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        mediaProjection = mgr.getMediaProjection(resultCode, data);
        
        imageReader = ImageReader.newInstance(width / 2, height / 2, PixelFormat.RGBA_8888, 2);
        virtualDisplay = mediaProjection.createVirtualDisplay(
            "ScreenCapture",
            width / 2, height / 2, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.getSurface(), null, handler
        );

        startServer();
        startCaptureLoop();

        return START_STICKY;
    }

    private void startServer() {
        try {
            server = new WebSocketServer(new InetSocketAddress(SERVER_PORT)) {
                @Override
                public void onOpen(WebSocket conn, ClientHandshake handshake) {
                    handler.post(() -> updateNotification("Cliente conectado: " + conn.getRemoteSocketAddress()));
                }

                @Override
                public void onClose(WebSocket conn, int code, String reason, boolean remote) {
                    handler.post(() -> updateNotification("Aguardando conexão..."));
                }

                @Override
                public void onMessage(WebSocket conn, String message) {}

                @Override
                public void onMessage(WebSocket conn, ByteBuffer message) {}

                @Override
                public void onError(WebSocket conn, Exception ex) {}

                @Override
                public void onStart() {}
            };
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startCaptureLoop() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                captureAndSend();
                handler.postDelayed(this, 1000 / FPS);
            }
        }, 1000 / FPS);
    }

    private void captureAndSend() {
        Image image = null;
        try {
            image = imageReader.acquireLatestImage();
            if (image == null) return;

            Image.Plane[] planes = image.getPlanes();
            ByteBuffer buffer = planes[0].getBuffer();
            int pixelStride = planes[0].getPixelStride();
            int rowStride = planes[0].getRowStride();
            int rowPadding = rowStride - pixelStride * (width / 2);

            Bitmap bitmap = Bitmap.createBitmap(
                (width / 2) + rowPadding / pixelStride, height / 2, Bitmap.Config.ARGB_8888);
            bitmap.copyPixelsFromBuffer(buffer);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
            byte[] jpegData = baos.toByteArray();

            for (WebSocket client : Collections.list(server.getConnections())) {
                if (client.isOpen()) {
                    client.send(jpegData);
                }
            }

            bitmap.recycle();
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (image != null) image.close();
        }
    }

    private void updateNotification(String text) {
        NotificationManager mgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification notification = new Notification.Builder(this, "screen_channel")
            .setContentTitle("Remote Server")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .build();
        mgr.notify(1, notification);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                "screen_channel", "Screen Capture", NotificationManager.IMPORTANCE_LOW);
            NotificationManager mgr = getSystemService(NotificationManager.class);
            mgr.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (virtualDisplay != null) virtualDisplay.release();
        if (mediaProjection != null) mediaProjection.stop();
        if (imageReader != null) imageReader.close();
        try {
            if (server != null) server.stop();
        } catch (Exception e) {}
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
