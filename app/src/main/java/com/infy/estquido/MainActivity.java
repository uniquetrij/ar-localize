package com.infy.estquido;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.ux.ArFragment;

public class MainActivity extends AppCompatActivity {

    private ArFragment mArFragment;
    private ImageView drawingImageView;

    private Canvas mCanvas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mArFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.main_fragment);

        drawingImageView = this.findViewById(R.id.iv);
        Bitmap bitmap = Bitmap.createBitmap((int) getWindowManager()
                .getDefaultDisplay().getWidth(), (int) getWindowManager()
                .getDefaultDisplay().getHeight() / 2, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(bitmap);
        drawingImageView.setImageBitmap(bitmap);

        // Line
        Paint paint = new Paint();
        paint.setColor(Color.RED);


        mArFragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> {
//            mArFragment.onUpdate(frameTime);
            Camera camera = mArFragment.getArSceneView().getScene().getCamera();

            Vector3 localPosition = camera.getLocalPosition();
            Vector3 worldPosition = camera.getWorldPosition();


            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (localPosition.x == 0 && localPosition.z == 0)
                        return;
                    mCanvas.drawCircle(mCanvas.getWidth() / 2 + localPosition.x * 10, mCanvas.getHeight() / 2 + localPosition.z * 10, 2f, paint);
                    drawingImageView.invalidate();
//                    Toast.makeText(getApplicationContext(), localPosition.toString(), Toast.LENGTH_LONG).show();
                }
            });

        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
    }
}
