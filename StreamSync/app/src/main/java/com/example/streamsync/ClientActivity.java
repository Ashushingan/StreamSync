package com.example.streamsync;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import androidx.appcompat.app.AppCompatActivity;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener {

    private static final int PORT = 8080;
    private static final String TAG = "ClientActivity";
    private Camera camera;
    private Socket clientSocket;
    private DataOutputStream outputStream;
    private String hostIp = "192.168.43.1";  // Ensure correct host IP

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);

        TextureView cameraPreview = findViewById(R.id.cameraPreview);
        cameraPreview.setSurfaceTextureListener(this);

        connectToHost();
    }

    private void connectToHost() {
        new Thread(() -> {
            try {
                clientSocket = new Socket(hostIp, PORT);
                Log.d(TAG, "Connected to host");
                outputStream = new DataOutputStream(clientSocket.getOutputStream());
                startCamera();
            } catch (IOException e) {
                Log.e(TAG, "Client connection failed: " + e.getMessage());
            }
        }).start();
    }

    private void startCamera() {
        if (camera != null) {
            camera.release();
        }
        try {
            camera = Camera.open();
            camera.setPreviewTexture(new SurfaceTexture(0));
            camera.startPreview();
            sendVideoStream();
        } catch (IOException e) {
            Log.e(TAG, "Failed to start camera", e);
        }
    }

    private void sendVideoStream() {
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
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (clientSocket != null) clientSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Error closing client socket: " + e.getMessage());
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        startCamera();
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
}
