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
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.DatabaseConfiguration;
import com.couchbase.lite.Document;
import com.couchbase.lite.MutableArray;
import com.couchbase.lite.MutableDocument;
import com.google.ar.core.Anchor;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.util.ArrayList;
import java.util.HashMap;

public class CheckpointsActivity extends AppCompatActivity {


    private static final String TAG = CheckpointsActivity.class.getName();
    private static final String DB_NAME = "CBDB";

    private ArFragment mArFragment;
    private Vector3 mLocalPosition;
    private Vector3 mCalibPose = new Vector3(0, 0, 0);
    private Database database;
    private MutableDocument mutableDoc;
    private int counter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkpoints);

        mArFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.checkpoints_fragment);

        mArFragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> {
            Camera mARCamera = mArFragment.getArSceneView().getScene().getCamera();
            mLocalPosition = mARCamera.getLocalPosition();
        });

        DatabaseConfiguration config = new DatabaseConfiguration(getApplicationContext());
        try {
            database = new Database(DB_NAME, config);
            Document docoment = database.getDocument("b32");

            if (docoment == null) {
                mutableDoc = new MutableDocument("b32");
                mutableDoc.setValue("checkpoints", new MutableArray());
                database.save(mutableDoc);
            } else {
                mutableDoc = docoment.toMutable();
            }
        } catch (CouchbaseLiteException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }
        Log.d("reading database on create", ((MutableArray) mutableDoc.getValue("checkpoints")).toList().toString());
        ////LOG/////////////////
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                MutableArray checkpoints = (MutableArray) mutableDoc.getValue("checkpoints");
//                mutableDoc.getValue("checkpoints");
//                Toast.makeText(getApplicationContext(),mutableDoc.getArray("checkpoints").toString()+"",Toast.LENGTH_LONG).show();
//            }
//        });


        //////////////////////

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    public void addWayPoint(View view) {
        MaterialFactory.makeOpaqueWithColor(this, new com.google.ar.sceneform.rendering.Color(Color.parseColor("#FFBF00")))
                .thenAccept(material -> {
                    ModelRenderable modelRenderable = ShapeFactory.makeSphere(0.1f, new Vector3(0.0f, 0.0f, 0.0f), material);
                    Anchor anchor = mArFragment.getArSceneView().getSession().createAnchor(new Pose(new float[]{mLocalPosition.x, mLocalPosition.y, mLocalPosition.z}, new float[]{0, 0, 0, 0}));
                    AnchorNode anchorNode = new AnchorNode(anchor);
                    anchorNode.setParent(mArFragment.getArSceneView().getScene());

                    // Create the transformable andy and add it to the anchor.
                    TransformableNode andy = new TransformableNode(mArFragment.getTransformationSystem());
                    andy.setParent(anchorNode);
                    andy.setRenderable(modelRenderable);
                    andy.select();

                    HashMap<String, Object> map = new HashMap<>();
                    map.put("x", mLocalPosition.x);
                    map.put("z", mLocalPosition.z);
                    String id = String.valueOf(++counter);

                    MutableArray checkpoints = (MutableArray) mutableDoc.getValue("checkpoints");
                    checkpoints.addValue(map);
                    mutableDoc.setValue("checkpoints", checkpoints);
                    try {
                        database.save(mutableDoc);
                        Log.d(TAG, mutableDoc.getArray("checkpoints").toString() + "");

                    } catch (CouchbaseLiteException e) {
                        Log.e(TAG, e.toString());
                        e.printStackTrace();
                    }
                });

//        Toast.makeText(getApplicationContext(), mLocalPosition.x + ", " + mLocalPosition.y + ", " + mLocalPosition.z, Toast.LENGTH_LONG).show();
//        Log.d(TAG, mLocalPosition.x + ", " + mLocalPosition.y + ", " + mLocalPosition.z);
//        Log.d(TAG, mArFragment.getArSceneView().getSession().getAllAnchors().size() + "");


    }

    public void syncOrigin(View view) {
        mCalibPose.set(mLocalPosition);
    }
}
