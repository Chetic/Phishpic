package com.phishcave.phishpic;

import android.content.Intent;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.graphics.BitmapFactory;

import java.io.ByteArrayInputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ConfirmPhoto extends Activity {
    public void acceptPhoto(View v) {
        EditText photo_name_edit_text = (EditText)findViewById(R.id.photoName);
        Intent intent = new Intent();
        intent.putExtra("name", photo_name_edit_text.getText().toString());
        setResult(Phishpic.RESULT_CONFIRM, intent);
        finish();
    }

    public void rejectPhoto(View v) {
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_confirm_photo);

        ImageView image_viewer = (ImageView)findViewById(R.id.imageView);
        EditText photo_name_edit_text = (EditText)findViewById(R.id.photoName);

        Intent intent = getIntent();
        byte[] imageData = intent.getByteArrayExtra("imageData");

        ByteArrayInputStream bis = new ByteArrayInputStream(imageData);
        Bitmap bmp = BitmapFactory.decodeStream(bis);
        image_viewer.setImageBitmap(bmp);
        image_viewer.setScaleType(ImageView.ScaleType.FIT_XY);

        photo_name_edit_text.setText(defaultPhotoName());
    }

    /* Attempt to free bitmap early */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        ImageView imageView = (ImageView)findViewById(R.id.imageView);
        Drawable drawable = imageView.getDrawable();

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            Bitmap bitmap = bitmapDrawable.getBitmap();
            bitmap.recycle();
        }
    }

    private String defaultPhotoName() {
        return "phishpic_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
    }
}
