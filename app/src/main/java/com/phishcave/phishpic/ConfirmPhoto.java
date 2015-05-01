package com.phishcave.phishpic;

import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.graphics.BitmapFactory;

import java.io.ByteArrayInputStream;
import java.text.SimpleDateFormat;
import java.util.Date;


public class ConfirmPhoto extends ActionBarActivity {

    private ImageView image_viewer;
    private EditText photo_name_edit_text;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_confirm_photo);

        image_viewer = (ImageView)findViewById(R.id.imageView);
        photo_name_edit_text = (EditText)findViewById(R.id.photoName);

        Intent intent = getIntent();
        byte[] imageData = intent.getByteArrayExtra("imageData");

        ByteArrayInputStream bis = new ByteArrayInputStream(imageData);
        Bitmap bp=BitmapFactory.decodeStream(bis);
        image_viewer.setImageBitmap(bp);
        image_viewer.setScaleType(ImageView.ScaleType.FIT_XY);

        photo_name_edit_text.setText(defaultPhotoName());
    }

    private String defaultPhotoName() {
        return "phishpic_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_confirm_photo, menu);
        return true;
    }

    public void redoPhoto(View v) {
        finish();
    }

    public void uploadPhoto(View v) {
        Intent intent = new Intent();
        intent.putExtra("name", photo_name_edit_text.getText().toString());
        setResult(Phishpic.RESULT_CONFIRM, intent);
        finish();
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
