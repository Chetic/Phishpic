package com.phishcave.phishpic;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.GooglePlayServicesUtil;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class Phishpic extends Activity {
    private int mCameraId = 0;
    private Camera mCamera;
    private CameraPreview mCameraPreview;
    SharedPreferences mSettings;
    protected static Activity activity;
    static final int REQUEST_CODE_PICK_ACCOUNT = 1000;
    String mEmail; // Received from newChooseAccountIntent(); passed to getToken()
    String mAuthToken = "";
    static final int REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR = 1001;
    static final int REQUEST_CODE_RECOVER_FROM_AUTH_ERROR = 1002;
    static final int REQUEST_CONFIRM_PHOTO = 1;
    static final int RESULT_CONFIRM = 1;
    private FrameLayout camera_preview;
    private final String upload_url = "http://phishcave.com/api/upload";

    private String imageName;
    private static final String SCOPE =
            "oauth2:https://www.googleapis.com/auth/userinfo.email";

    public void storeFile(String filename, byte[] data) {
        try {
            FileOutputStream fo = openFileOutput(filename, Context.MODE_PRIVATE);
            fo.write(data);
            fo.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void takePhoto(View v) {
        mCamera.takePicture(null, null, null,
                new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] data, Camera camera) {
                        Log.d("Phishpic", "onPictureTaken");

                        if ( data != null ) {
                            storeFile("jpeg", data);

                            Intent confirmIntent = new Intent(getApplicationContext(), ConfirmPhoto.class);
                            startActivityForResult(confirmIntent, REQUEST_CONFIRM_PHOTO);
                        } else {
                            Toast.makeText(getApplicationContext(), "Failed to get image", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phishpic);

        camera_preview = (FrameLayout)findViewById(R.id.app_frame);
        Button uploadButton = (Button)findViewById(R.id.uploadButton);
        uploadButton.bringToFront();

        activity = this;

        mSettings = getPreferences(0);
        mEmail = mSettings.getString("email", "");

        getUsername();
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);

            mCamera = Camera.open(mCameraId);
            mCameraPreview = new CameraPreview(this, mCameraId, mCamera, metrics.widthPixels, metrics.heightPixels);
            camera_preview.addView(mCameraPreview);
        }
        else {
            Toast.makeText(getApplicationContext(), "Error: No camera found", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("Phishpic", "Received intent response with request code: " +
                String.valueOf(requestCode) + ", result code: " + String.valueOf(resultCode));
        if (requestCode == REQUEST_CODE_PICK_ACCOUNT) {
            // Receiving a result from the AccountPicker
            if (resultCode == RESULT_OK) {
                mEmail = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                SharedPreferences.Editor settingsEditor = mSettings.edit();
                settingsEditor.putString("email", mEmail);
                settingsEditor.commit();
                // With the account name acquired, go get the auth token
                getUsername();
            } else if (resultCode == RESULT_CANCELED) {
                // The account picker dialog closed without selecting an account.
                // Notify users that they must pick an account to proceed.
                Toast.makeText(this, "Uploading anonymously", Toast.LENGTH_SHORT).show();
            }
        } else if ((requestCode == REQUEST_CODE_RECOVER_FROM_AUTH_ERROR ||
                requestCode == REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR)
                && resultCode == RESULT_OK) {
            // Receiving a result that follows a GoogleAuthException, try auth again
            getUsername();
        } else if ((requestCode == REQUEST_CONFIRM_PHOTO)) {
            if (resultCode == RESULT_CONFIRM) {
                imageName = data.getStringExtra("name");
                new UploadPhotoTasAsyncTask().execute();
            }
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        if (mCameraPreview != null) {
            mCameraPreview.close();
            mCameraPreview = null;
        }
    }

    public void setAuthToken(String token) {
        this.mAuthToken = token;
    }

    /**
     * This method is a hook for background threads and async tasks that need to
     * provide the user a response UI when an exception occurs.
     */
    public void handleException(final Exception e) {
        // Because this call comes from the AsyncTask, we must ensure that the following
        // code instead executes on the UI thread.
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (e instanceof GooglePlayServicesAvailabilityException) {
                    // The Google Play services APK is old, disabled, or not present.
                    // Show a dialog created by Google Play services that allows
                    // the user to update the APK
                    int statusCode = ((GooglePlayServicesAvailabilityException) e)
                            .getConnectionStatusCode();
                    Dialog dialog = GooglePlayServicesUtil.getErrorDialog(statusCode,
                            Phishpic.this,
                            REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR);
                    dialog.show();
                } else if (e instanceof UserRecoverableAuthException) {
                    // Unable to authenticate, such as when the user has not yet granted
                    // the app access to the account, but the user can fix this.
                    // Forward the user to an activity in Google Play services.
                    Intent intent = ((UserRecoverableAuthException) e).getIntent();
                    startActivityForResult(intent,
                            REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR);
                }
            }
        });
    }

    /**
     * Attempts to retrieve the username.
     * If the account is not yet known, invoke the picker. Once the account is known,
     * start an instance of the AsyncTask to get the auth token and do work with it.
     */
    private void getUsername() {
        if (mEmail == "") {
            pickUserAccount();
        } else {
            new GetUsernameTask(Phishpic.this, mEmail, SCOPE).execute();
        }
    }

    private void pickUserAccount() {
        String[] accountTypes = new String[]{"com.google"};
        Intent intent = AccountPicker.newChooseAccountIntent(null, null,
                accountTypes, false, null, null, null, null);
        startActivityForResult(intent, REQUEST_CODE_PICK_ACCOUNT);
    }

    private class UploadPhotoTasAsyncTask extends AsyncTask<byte[], Void, HttpResponse> {

        private InputStream loadPhotoData() {
            FileInputStream imageData = null;
            try {
                imageData = openFileInput("jpeg");
            }
            catch(FileNotFoundException e) {
                Log.d("Phishpic", "Error loading bitmap: " + e.getMessage());
            }

            return imageData;
        }

        @Override
        protected HttpResponse doInBackground(byte[]... params) {
            InputStream imageData = loadPhotoData();
            if ( imageData != null ) {
                return upload(imageData);
            } else {
                return null;
            }

        }

        protected void onPostExecute(HttpResponse response) {
            if (response == null) {
                return;
            }
            int status = response.getStatusLine().getStatusCode();

            Context c = getApplicationContext();

            if ( status == 200) {
                Toast.makeText(c, "Success", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(c, "Failed", Toast.LENGTH_SHORT).show();
            }
        }

        private HttpResponse upload(InputStream imageData) {
            // Create a new HttpClient and Post Header
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost(upload_url);
            MultipartEntityBuilder meb = MultipartEntityBuilder.create();

            meb.addBinaryBody("upload[file]", imageData, ContentType.create("image/jpeg"), imageName);
            meb.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);

            HttpEntity multipartEntity = meb.build();
            httppost.setEntity(multipartEntity);

            if (mAuthToken != "") {
                httppost.addHeader("AuthToken", mAuthToken);
                httppost.addHeader("AuthMethod", "Google");
            }
            try {
                HttpResponse response = httpclient.execute(httppost);
                HttpEntity responseEntity = response.getEntity();
                Log.d("Phishpic", EntityUtils.toString(responseEntity));
                return response;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

}
