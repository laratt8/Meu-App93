package com.example.remoteserver;

import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.text.format.Formatter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.net.wifi.WifiManager;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_SCREEN_CAPTURE = 1;
    private MediaProjectionManager projectionManager;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50);
        
        statusText = new TextView(this);
        statusText.setTextSize(18);
        
        Button btn = new Button(this);
        btn.setText("INICIAR SERVIDOR");
        btn.setOnClickListener(v -> startScreenCapture());
        
        layout.addView(statusText);
        layout.addView(btn);
        setContentView(layout);

        projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);

        // Mostrar IP local (se disponível)
        String ip = getLocalIpAddress();
        if (ip != null) {
            statusText.setText("Servidor Parado\nIP: " + ip + "\nAguardando início...");
        } else {
            statusText.setText("Servidor Parado\nAguardando início...");
        }
    }

    private void startScreenCapture() {
        Intent intent = projectionManager.createScreenCaptureIntent();
        startActivityForResult(intent, REQUEST_SCREEN_CAPTURE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SCREEN_CAPTURE && resultCode == RESULT_OK) {
            Intent serviceIntent = new Intent(this, ScreenCaptureService.class);
            serviceIntent.putExtra("resultCode", resultCode);
            serviceIntent.putExtra("data", data);
            startForegroundService(serviceIntent);
            
            statusText.setText("✅ SERVIDOR RODANDO\nPorta: 8080\nAguardando conexões...");
            Toast.makeText(this, "Servidor iniciado!", Toast.LENGTH_SHORT).show();
        }
    }

    private String getLocalIpAddress() {
        try {
            WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifi == null) return null;
            int ipAddress = wifi.getConnectionInfo().getIpAddress();
            if (ipAddress == 0) return null;
            return Formatter.formatIpAddress(ipAddress);
        } catch (Exception e) {
            return null;
        }
    }
}
