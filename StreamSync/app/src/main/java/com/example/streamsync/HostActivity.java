package com.example.streamsync;

import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.SurfaceHolder;
import androidx.appcompat.app.AppCompatActivity;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class HostActivity extends AppCompatActivity {

    private static final int PORT = 8080;
    private static final String TAG = "HostActivity";
    private ServerSocket serverSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_host);

        startServer();
    }

    private void startServer() {
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                Log.d(TAG, "Server started on port: " + PORT);

                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    Log.d(TAG, "Client connected: " + clientSocket.getInetAddress());
                    // Handle incoming video streams (future implementation)
                }
            } catch (IOException e) {
                Log.e(TAG, "Server error: " + e.getMessage());
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Error closing server: " + e.getMessage());
        }
    }
}
