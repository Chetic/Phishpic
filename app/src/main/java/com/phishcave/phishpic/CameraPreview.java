package com.phishcave.phishpic;

/**
 * Created by freveny on 2015-03-10.
 */

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;


import java.io.IOException;
import java.util.List;

@SuppressWarnings("deprecation")

/** A basic Camera preview class */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private int mCameraId;
    private int previewHeight;
    private int previewWidth;

    public CameraPreview(Context context, int cameraId, Camera camera, int width, int height) {
        super(context);
        mCameraId = cameraId;
        mCamera = camera;

        previewHeight = height;
        previewWidth = width;

        //boolean did_disable_shutter_sound_work = mCamera.enableShutterSound(false);

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    private Size getOptimalSize(List<Size> sizes,int w,int h){
        final double ASPECT_TOLERANCE=0.0;
        double targetRatio=(double)w / h;
        if (sizes == null)   return null;
        Size optimalSize=null;
        double minDiff=Double.MAX_VALUE;
        int targetHeight=Math.min(h,w);
        for (  Size size : sizes) {
            double ratio=(double)size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)     continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize=size;
                minDiff=Math.abs(size.height - targetHeight);
            }
        }
        if (optimalSize == null) {
            minDiff=Double.MAX_VALUE;
            for (    Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize=size;
                    minDiff=Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            mCamera.setPreviewDisplay(holder);
            Camera.Parameters params = mCamera.getParameters();
            List<Camera.Size> sizes = params.getSupportedPictureSizes();
            Camera.Size optimalSize = getOptimalSize(sizes, previewWidth, previewHeight);

            Log.d("Phishpic", "Setting picture size to: " + optimalSize.width + "x" + optimalSize.height);

            params.setPictureSize(optimalSize.width, optimalSize.height);
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            params.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
            params.setPictureFormat(ImageFormat.JPEG);
            params.setJpegQuality(100);

            mCamera.setParameters(params);

            mCamera.startPreview();
        } catch (IOException e) {
            Log.d("Phishpic", "Error setting camera preview: " + e.getMessage());
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        close();
    }

    public void close() {
        if ( mCamera != null ) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;

            mHolder.removeCallback(this);
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (mHolder.getSurface() == null){
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e){
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here
        setCameraDisplayOrientation();

        // start preview with new settings
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
        } catch (Exception e){
            Log.d("Phishpic", "Error starting camera preview: " + e.getMessage());
        }
    }

    public void setCameraDisplayOrientation() {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(mCameraId, info);
        int rotation = Phishpic.activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        mCamera.setDisplayOrientation(result);

        Camera.Parameters params = mCamera.getParameters();
        params.setRotation(result);
        mCamera.setParameters(params);
    }
}
