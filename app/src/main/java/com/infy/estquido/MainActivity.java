package com.infy.estquido;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.ar.core.Anchor;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.util.concurrent.atomic.AtomicReference;

public class MainActivity extends AppCompatActivity {


    private static final String TAG = MainActivity.class.getName();
    private ArFragment mArFragment;
    private ImageView iv;

    private Canvas mCanvas;

    private Vector3 mLocalPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mArFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.main_fragment);

        iv = this.findViewById(R.id.iv);
        Bitmap bitmap = Bitmap.createBitmap((int) getWindowManager()
                .getDefaultDisplay().getWidth(), (int) getWindowManager()
                .getDefaultDisplay().getHeight() / 2, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(bitmap);
        iv.setImageBitmap(bitmap);

        // Line
        Paint paint = new Paint();
        paint.setColor(Color.RED);


        mArFragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> {
            Camera camera = mArFragment.getArSceneView().getScene().getCamera();
            mLocalPosition = camera.getWorldPosition();

            float x = mLocalPosition.x;
            float y = mLocalPosition.z;

            runOnUiThread(() -> {

                if (x == 0 && y == 0)
                    return;
                mCanvas.drawCircle(mCanvas.getWidth() / 2 + x * 10, mCanvas.getHeight() / 2 + y * 10, 2f, paint);
                iv.invalidate();
            });

        });

        final ModelRenderable[] redSphereRenderable = new ModelRenderable[1];
        MaterialFactory.makeOpaqueWithColor(this, new com.google.ar.sceneform.rendering.Color(Color.BLUE))
                .thenAccept(
                        material -> {
                            redSphereRenderable[0] = ShapeFactory.makeSphere(0.1f, new Vector3(0.0f, 0.0f, 0.0f), material);
                        });

        mArFragment.setOnTapArPlaneListener((HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
            // Create the Anchor.
            Anchor anchor = hitResult.createAnchor();
            AnchorNode anchorNode = new AnchorNode(anchor);
            anchorNode.setParent(mArFragment.getArSceneView().getScene());

            // Create the transformable andy and add it to the anchor.
            TransformableNode andy = new TransformableNode(mArFragment.getTransformationSystem());
            andy.setParent(anchorNode);
            andy.setRenderable(redSphereRenderable[0]);
            andy.select();
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
    }
}
