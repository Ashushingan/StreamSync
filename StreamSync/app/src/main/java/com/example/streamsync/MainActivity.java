package com.example.streamsync;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class MainActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener {

    private Button startHost, joinClient;
    private TextView statusText;
    private TextureView cameraPreview;

    private ServerSocket serverSocket;
    private Socket clientSocket;
    private DataOutputStream outputStream;
    private Camera camera;

    private static final int PORT = 8080;
    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final String TAG = "SocketStream";

    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        surfaceView = findViewById(R.id.cameraPreview);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                startCamera();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                if (camera != null) {
                    camera.stopPreview();
                    camera.release();
                    camera = null;
                }
            }
        });
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, 100);
        }

    }

    private void startServer() {
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                runOnUiThread(() -> statusText.setText("Waiting for client..."));
                Log.d(TAG, "Server started on port: " + PORT);

                clientSocket = serverSocket.accept();
                runOnUiThread(() -> statusText.setText("Client connected: " + clientSocket.getInetAddress()));
                Log.d(TAG, "Client connected: " + clientSocket.getInetAddress());

                outputStream = new DataOutputStream(clientSocket.getOutputStream());
            } catch (IOException e) {
                Log.e(TAG, "Server error: " + e.getMessage());
            }
        }).start();
    }

    private void startClient() {
        new Thread(() -> {
            try {
                String hostIp = "192.168.43.1";  // Ensure this is the host's actual IP
                clientSocket = new Socket(hostIp, PORT);
                runOnUiThread(() -> statusText.setText("Connected to Host"));
                Log.d(TAG, "Connected to server");

                outputStream = new DataOutputStream(clientSocket.getOutputStream());

                startCamera();
            } catch (IOException e) {
                Log.e(TAG, "Client error: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Connection failed", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

        private void startCamera() {
            if (camera != null) {
                camera.release(); // Release any previously held camera
            }

            try {
                camera = Camera.open();
                camera.setPreviewDisplay(surfaceHolder);
                camera.startPreview();
            } catch (Exception e) {
                Log.e("CameraError", "Failed to start camera", e);
            }
        }


    private void streamVideo() {
        new Thread(() -> {
            try {
                byte[] buffer = new byte[1024];
                while (true) {
                    outputStream.write(buffer);
                    outputStream.flush();
                }
            } catch (IOException e) {
                Log.e(TAG, "Streaming error: " + e.getMessage());
            }
        }).start();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (camera == null) {
            startCamera();
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {}

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {}

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (serverSocket != null) serverSocket.close();
            if (clientSocket != null) clientSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Cleanup error: " + e.getMessage());
        }
    }
}
