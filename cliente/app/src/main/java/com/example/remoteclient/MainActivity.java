package com.example.remoteclient;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

public class MainActivity extends AppCompatActivity {
    private ImageView screenView;
    private WebSocketClient client;
    private Handler handler = new Handler(Looper.getMainLooper());
    private String serverIp = "192.168.1.100";
    private TextView statusText;
    private boolean isConnected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 20, 20, 20);
        
        statusText = new TextView(this);
        statusText.setText("Desconectado");
        
        EditText ipInput = new EditText(this);
        ipInput.setHint("IP do servidor (ex: 192.168.1.100)");
        ipInput.setText(serverIp);
        
        Button btnConnect = new Button(this);
        btnConnect.setText("CONECTAR");
        btnConnect.setOnClickListener(v -> {
            serverIp = ipInput.getText().toString();
            connectToServer();
        });
        
        screenView = new ImageView(this);
        screenView.setMinimumHeight(800);
        
        layout.addView(statusText);
        layout.addView(ipInput);
        layout.addView(btnConnect);
        layout.addView(screenView);
        setContentView(layout);
        
        setupTouchControl();
    }

    private void connectToServer() {
        try {
            URI uri = new URI("ws://" + serverIp + ":8080");
            
            client = new WebSocketClient(uri) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    isConnected = true;
                    handler.post(() -> statusText.setText("✅ CONECTADO!"));
                }

                @Override
                public void onMessage(String message) {}

                @Override
                public void onMessage(java.nio.ByteBuffer bytes) {
                    byte[] data = new byte[bytes.remaining()];
                    bytes.get(data);
                    
                    Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                    handler.post(() -> screenView.setImageBitmap(bitmap));
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    isConnected = false;
                    handler.post(() -> statusText.setText("Desconectado"));
                }

                @Override
                public void onError(Exception ex) {
                    handler.post(() -> statusText.setText("Erro: " + ex.getMessage()));
                }
            };
            
            client.connect();
        } catch (Exception e) {
            statusText.setText("Erro: " + e.getMessage());
        }
    }

    private void setupTouchControl() {
        screenView.setOnTouchListener((v, event) -> {
            if (!isConnected || client == null) return false;
            
            float x = event.getX() / v.getWidth();
            float y = event.getY() / v.getHeight();
            
            String command = String.format("%s:%.3f:%.3f", 
                event.getAction() == MotionEvent.ACTION_DOWN ? "DOWN" : 
                event.getAction() == MotionEvent.ACTION_MOVE ? "MOVE" : "UP", x, y);
            
            client.send(command);
            return true;
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (client != null) client.close();
    }
}
