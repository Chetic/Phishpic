package com.phishcave.phishpic;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class Phishpic extends Activity {
    private int mCameraId = 0;
    private Camera mCamera;
    private CameraPreview mCameraPreview;
    String mCurrentPhotoPath;
    protected static Activity activity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phishpic);

        activity = this;


        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            mCamera = Camera.open(mCameraId);
            mCameraPreview = new CameraPreview(this, mCameraId, mCamera);
            FrameLayout camera_preview = (FrameLayout)findViewById(R.id.app_frame);
            camera_preview.addView(mCameraPreview);
            Button uploadButton = (Button)findViewById(R.id.uploadButton);
            uploadButton.bringToFront();
        }
        else {
            Toast.makeText(getApplicationContext(), "Error: No camera found", Toast.LENGTH_LONG).show();
        }
    }

    public void uploadPicture(View v) {
        mCamera.takePicture(null, null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] jpegData, Camera camera) {
                Log.d("Phishpic", "onPictureTaken");
                AsyncTask<byte[], Void, Void> task = new AsyncTask<byte[], Void, Void>() {
                    @Override
                    protected Void doInBackground(byte[]... params) {
                        byte[] jpegData = params[0];
                        postData(jpegData, "http://phishcave.com/api/upload");
                        return null;
                    }
                };
                task.execute(jpegData);
            }
        });
    }

    public void postData(byte[] data, String url) {
        // Create a new HttpClient and Post Header
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(url);
        String filename =  new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        MultipartEntityBuilder meb = MultipartEntityBuilder.create();
        meb.addBinaryBody("upload[file]", data, ContentType.create("image/jpeg"), "phishpic" + filename);
        meb.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        HttpEntity multipartEntity = meb.build();
        httppost.setEntity(multipartEntity);
        try {
            HttpResponse response = httpclient.execute(httppost);
            HttpEntity responseEntity = response.getEntity();
            Log.d("Phishpic", EntityUtils.toString(responseEntity));
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCamera.startPreview();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("Phishpic", "Received intent response with request code: " +
                String.valueOf(requestCode) + ", result code: " + String.valueOf(resultCode));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_phishpic, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
