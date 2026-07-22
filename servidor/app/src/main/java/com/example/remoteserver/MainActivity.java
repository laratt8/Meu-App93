package com.example.remoteserver;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
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
        statusText.setText("Servidor Parado\nAguardando início...");
        statusText.setTextSize(18);
        
        Button btn = new Button(this);
        btn.setText("INICIAR SERVIDOR");
        btn.setOnClickListener(v -> startScreenCapture());
        
        layout.addView(statusText);
        layout.addView(btn);
        setContentView(layout);

        projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
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
}
