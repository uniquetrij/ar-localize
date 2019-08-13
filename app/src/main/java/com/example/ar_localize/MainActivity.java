package com.example.ar_localize;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.DatabaseConfiguration;

import com.couchbase.lite.MutableDocument;
import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.ux.ArFragment;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "couchbase";
    private static final String DB_NAME = "ar-localize";
    private ArFragment mArFragment;
    private ImageView drawingImageView;

    private Canvas mCanvas;
    private float cx,cy;
    private Database database;
    private MutableDocument mutableDoc;
    private int counter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mArFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.main_fragment);

        DatabaseConfiguration config = new DatabaseConfiguration(getApplicationContext());
        try {
             database = new Database(DB_NAME, config);
             mutableDoc = new MutableDocument();
             mutableDoc.setValue("checkpoints", new ArrayList<Checkpoint>());
//             mutableDoc.setData(new HashMap<String, Object>());
             database.save(mutableDoc);
        } catch (CouchbaseLiteException e) {
            Log.e(TAG,e.toString());
            e.printStackTrace();
        }

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
                    cx=localPosition.x;
                    cy=localPosition.z;

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

    public void recordCheckpoint(View view) {
        Map<String,Float> map = new HashMap<>();
        map.put("x", cx);
        map.put("y", cy);
        String id=String.valueOf(++counter);
        mutableDoc.setValue(id, map);
        try {
            database.save(mutableDoc);
        } catch (CouchbaseLiteException e) {
            Log.e(TAG,e.toString());
            e.printStackTrace();
        }
        mutableDoc = database.getDocument(mutableDoc.getId()).toMutable();
        Log.d(TAG,mutableDoc.getValue(id).toString());
        Toast.makeText(getApplicationContext(),mutableDoc.getDictionary(id).getFloat("x")+"",Toast.LENGTH_SHORT).show();


    }
}
