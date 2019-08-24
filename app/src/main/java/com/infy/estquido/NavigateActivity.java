package com.infy.estquido;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.GenericLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.DatabaseConfiguration;
import com.couchbase.lite.Document;
import com.couchbase.lite.MutableArray;
import com.couchbase.lite.MutableDocument;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.ar.core.Anchor;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.HitTestResult;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.ux.ArFragment;

import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;

public class NavigateActivity extends AppCompatActivity {


    private static final String TAG = CheckpointsActivity.class.getName();
    private static final String DB_NAME = "Estquido";

    private ArFragment mArFragment;
    private Vector3 mCamPosition;
    private Vector3 calibPosition = Vector3.zero();


    private Set<WayPoint> mWayPoints = Collections.synchronizedSet(new LinkedHashSet<>());
    private WayPoint selectedWayPoint;

    private int wayPointCounter = 0;
    private Quaternion mRotation ;
    private Vector3 mPosition ;
    private Database database;
    private MutableDocument document;
    private Quaternion mCamRotation;
    private Anchor mAnchor = null;
    private Vector3 prevCamPosition = null;
    private Quaternion prevCamRotation = null;
    private String wayPointName = "wayPoint_";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkpoints);
        initialiseDB();

        mArFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.checkpoints_fragment);


        mArFragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> {
            Camera mARCamera = mArFragment.getArSceneView().getScene().getCamera();
            mCamPosition = mARCamera.getLocalPosition();
            mCamRotation = mARCamera.getLocalRotation();


            if (prevCamPosition == null & prevCamRotation == null) {
                prevCamPosition = mCamPosition;
                prevCamRotation = mCamRotation;
            }

            if (mAnchor == null & !prevCamRotation.equals(mCamRotation) && !prevCamPosition.equals(mCamPosition)) {
                createAnchor(mCamPosition,mCamRotation);

            }
            if (mAnchor == null) {
                prevCamPosition = mCamPosition;
                prevCamRotation = mCamRotation;
            }


        });
    }

    private void initialiseDB() {
        DatabaseConfiguration config = new DatabaseConfiguration(getApplicationContext());
        try {
            database = new Database(DB_NAME, config);
            Document doc = database.getDocument("B32F0");

            if (doc == null) {
                document = new MutableDocument("B32F0");
                document.setValue("WayPoints", new ArrayList<Map<String, Object>>());
                document.setValue("WayPointIDs", new ArrayList<Integer>(Arrays.asList(0)));
                document.setValue("CheckPoints", new ArrayList<Map<String, Integer>>());
                database.save(document);
            } else {
                document = doc.toMutable();
                Map<String, WayPoint> newWayPoints = Collections.synchronizedMap(new LinkedHashMap<>());
                ((MutableArray) document.getValue("WayPoints")).toList().stream().forEachOrdered(m -> {
                    Map<String, Map<String, Object>> map = (Map<String, Map<String, Object>>) m;
                    Map<String, Object> wpMap = map.values().stream().findFirst().get();
                    WayPoint wayPoint = new WayPoint(((Long) wpMap.get("id")).intValue(), new Vector3((float) wpMap.get("x"), (float) wpMap.get("y"), (float) wpMap.get("z")), (String) wpMap.get("wayPointName"), (boolean) wpMap.get("isCheckpoint"));
                    wayPoint.setCheckpointsPath((Map<String, List<String>>) wpMap.get("routes"));
                    mWayPoints.add(wayPoint);
                    newWayPoints.put(wayPoint.getWayPointName(), wayPoint);
                });
                wayPointCounter = ((MutableArray) document.getValue("WayPointIDs")).toList().stream().mapToInt(value -> ((Long) value).intValue()).max().getAsInt();

                ((MutableArray) document.getValue("WayPoints")).toList().stream().forEachOrdered(m -> {
                    Log.d("m", m.toString());
                    Map<String, Map<String, Object>> map = (Map<String, Map<String, Object>>) m;
                    Map<String, Object> wpMap = map.values().stream().findFirst().get();
                    List<String> connections = (List<String>) wpMap.get("connections");
                    connections.stream().forEachOrdered(wayPointName -> {
                        newWayPoints.get((wpMap.get("wayPointName"))).getConnections().add(newWayPoints.get((wayPointName)));
                        newWayPoints.get((wayPointName)).getConnections().add(newWayPoints.get((wpMap.get("wayPointName"))));
                    });

                });

            }
        } catch (CouchbaseLiteException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }
        Log.d("DATABASE", wayPointCounter + "");
        Log.d("DATABASE", ((MutableArray) document.getValue("WayPoints")).toList().toString());
        Log.d("DATABASE", mWayPoints.toString());
    }



    private WayPoint addWayPoint(Integer id, Vector3 position, String wayPointName, boolean isCheckpoint, boolean dispalyNode, boolean isDestination) {

        WayPoint wayPoint = new WayPoint(id, position, wayPointName, isCheckpoint);
        if(dispalyNode) {
            MaterialFactory.makeOpaqueWithColor(this, new com.google.ar.sceneform.rendering.Color(Color.parseColor("#FFBF00")))
                    .thenAccept(material -> {

                        AtomicReference<ModelRenderable> modelRenderable = new AtomicReference<>();

                        if(!isDestination) {
                            ModelRenderable.builder()
                                    .setSource(this, Uri.parse("arrow.sfb"))
                                    .build()
                                    .thenAccept(renderable -> modelRenderable.set(renderable))
                                    .exceptionally(throwable -> {
                                        return null;
                                    });

                        }
                        else{
                            ModelRenderable.builder()
                                    .setSource(this, Uri.parse("destination.sfb"))
                                    .build()
                                    .thenAccept(renderable -> modelRenderable.set(renderable))
                                    .exceptionally(throwable -> {
                                        return null;
                                    });
                        }

                        new Timer().schedule(new TimerTask() {
                            @Override
                            public void run() {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        AnchorNode anchorNode = new AnchorNode();
                                        anchorNode.setAnchor(mAnchor);
                                        anchorNode.setParent(mArFragment.getArSceneView().getScene());


                                        wayPoint.getNode().setParent(anchorNode);
                                        if(isDestination) {
                                            wayPoint.getNode().setLocalScale(new Vector3(15, 15, 20));
                                        }
                                        else{
                                            wayPoint.getNode().setLocalScale(new Vector3(5, 15, 4));
                                        }
                                        wayPoint.getNode().setRenderable(modelRenderable.get());
                                        wayPoint.getNode().setLocalPosition(position);
                                    }
                                });
                            }
                        }, 300);

                        AnchorNode anchorNode = new AnchorNode();
                        anchorNode.setAnchor(mAnchor);
                        anchorNode.setParent(mArFragment.getArSceneView().getScene());

                        wayPoint.getNode().setParent(anchorNode);
                        wayPoint.getNode().setRenderable(modelRenderable.get());
                        wayPoint.getNode().setLocalPosition(position);

                    });
        }
        return wayPoint;
    }


    private void connectWayPoints(WayPoint from, WayPoint to) {
        if (from.getConnections().contains(to) && to.getConnections().contains(from))
            return;

        from.getConnections().add(to);
        to.getConnections().add(from);

        AnchorNode node1 = (AnchorNode) from.getNode().getParent();
        AnchorNode node2 = (AnchorNode) to.getNode().getParent();

        Vector3 point1, point2;
        point1 = from.getPosition();
        point2 = to.getPosition();


        final Vector3 difference = Vector3.subtract(point1, point2);
        final Vector3 directionFromTopToBottom = difference.normalized();
        final Quaternion rotationFromAToB = Quaternion.lookRotation(directionFromTopToBottom, Vector3.up());

        from.getNode().setWorldRotation(rotationFromAToB);


    }

    public void createAnchor(Vector3 position, Quaternion rotation) {
        mRotation=rotation;
        mAnchor = mArFragment.getArSceneView().getSession().createAnchor(new Pose(new float[]{position.x, position.y, position.z}, new float[]{0, 0, 0, -rotation.w}));

    }


    public void syncPositions(View view) {
        mArFragment.getArSceneView().getSession().getAllAnchors().forEach(anchor -> anchor.detach());
        mAnchor=null;
        mPosition = mCamPosition;
        mRotation = mCamRotation;

        reset(null);
        view.setEnabled(false);
        if(mAnchor == null){
            createAnchor(mPosition,mRotation);
        }


        syncPositions("c3", "c1");
        view.setEnabled(true);


//        new Timer().schedule(new TimerTask() {
//            @Override
//            public void run() {
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        reset(null);
//                        view.setEnabled(false);
//                        if(mAnchor == null){
//                            createAnchor(mCamPosition,mCamRotation);
//                        }
//
//
//                        syncPositions("c2", "c1");
//                        view.setEnabled(true);
//                    }
//                });
//            }
//        }, 5000);
//
//
//        new Timer().schedule(new TimerTask() {
//            @Override
//            public void run() {
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        reset(null);
//                        view.setEnabled(false);
//                        if(mAnchor == null){
//                            createAnchor(mCamPosition,mCamRotation);
//                        }
//
//
//                        syncPositions("c2", "c3");
//                        view.setEnabled(true);
//                    }
//                });
//            }
//        }, 10000);
    }

    public void syncPositions(String src, String dst) {

        List<WayPoint> wpList = new ArrayList<WayPoint>(mWayPoints);
        mWayPoints.clear();


        boolean isPathExists = true;

        for( WayPoint wp : wpList){
            if(wp.getWayPointName().equalsIgnoreCase(src)){
                if(wp.getCheckpointsPath().keySet().isEmpty()){
                    isPathExists = false;
                }
                else{
                    if(!wp.getCheckpointsPath().keySet().contains(dst)){
                        isPathExists = false;
                    }

                }

            }
        }

        if(!isPathExists){
            DijikstrasShortestPath dj = new DijikstrasShortestPath();
            wpList = dj.getShortestPath(wpList,src,dst);
        }
        else{
        }

        Set<WayPoint> oldWP = Collections.synchronizedSet(new LinkedHashSet<>(wpList));
        Map<Integer, WayPoint> newWayPoints = Collections.synchronizedMap(new LinkedHashMap<>());



        Map<String, List<String>> pathpoints = new HashMap<>();
        for (WayPoint wp : wpList) {
            if (wp.getWayPointName().equalsIgnoreCase(src)) {
                pathpoints = wp.getCheckpointsPath();
            }
        }

        List<String> path = new ArrayList<>();
        for (String name : pathpoints.keySet()) {

            if (name.equalsIgnoreCase(dst)) {
                path = pathpoints.get(name);
            }
        }


        List<WayPoint> pathWayPoints = new ArrayList<>();

        for (String name : path) {
            pathWayPoints.add(wpList.get(IterableUtils.indexOf(wpList, object -> object.getWayPointName().equals(name))));
        }
        oldWP.stream().forEachOrdered(wpnt -> {
            WayPoint wpoint;
            if(pathWayPoints.contains(wpnt)) {
                if(wpnt.getWayPointName().equalsIgnoreCase(dst)) {
                    wpoint = addWayPoint(wpnt.getId(), wpnt.getPosition(), wpnt.getWayPointName(), wpnt.getIsCheckpoint(), true,true);
                    wpoint.setCheckpointsPath(wpnt.getCheckpointsPath());
                    if(wpoint.getConnections().isEmpty()){
                        wpoint.setConnections(wpnt.getConnections());
                    }
                }
                else{
                    wpoint = addWayPoint(wpnt.getId(), wpnt.getPosition(), wpnt.getWayPointName(), wpnt.getIsCheckpoint(), true, false);
                    wpoint.setCheckpointsPath(wpnt.getCheckpointsPath());
                    if(wpoint.getConnections().isEmpty()){
                        wpoint.setConnections(wpnt.getConnections());
                    }
                }

            }
            else {
                wpoint = addWayPoint(wpnt.getId(), wpnt.getPosition(), wpnt.getWayPointName(), wpnt.getIsCheckpoint(), false, false);
                wpoint.setCheckpointsPath(wpnt.getCheckpointsPath());
                if(wpoint.getConnections().isEmpty()){
                    wpoint.setConnections(wpnt.getConnections());
                }
            }
            newWayPoints.put(wpnt.getId(), wpoint);
        });


        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Set<WayPoint> visited = new HashSet<>();

                        for (int i = 0; i < pathWayPoints.size() - 1; i++) {

                            connectWayPoints(newWayPoints.get(pathWayPoints.get(i).getId()), newWayPoints.get(pathWayPoints.get(i + 1).getId()));
                        }
                    }
                });
            }
        }, 10);

        List<WayPoint> finalWayPoint = new ArrayList<>(newWayPoints.values());

        Collections.sort(finalWayPoint, Comparator.comparingLong(WayPoint::getId));

        mWayPoints.addAll(newWayPoints.values());
        persistWayPoints();

    }

    public void persistWayPoints() {
        if (mWayPoints.isEmpty())
            return;
        List<Map<String, Object>> wpArray = new ArrayList<>();
        List<Integer> idArray = new ArrayList<>();
        Map<String, Integer> checkpointArray = new HashMap<>();


        mWayPoints.stream().forEachOrdered(wayPoint -> {
            if (wayPoint.getIsCheckpoint()) {
                checkpointArray.put(wayPoint.getWayPointName(), wayPoint.getId());
            }
        });

        mWayPoints.stream().forEachOrdered(wayPoint -> {

            Map<String, Object> node = new LinkedHashMap<>();
            Map<String, Object> map = wayPoint.toMap();

            node.put(wayPoint.getWayPointName(), map);
            wpArray.add(node);
            idArray.add(wayPoint.getId());

        });

        document.setValue("WayPoints", wpArray);
        document.setValue("WayPointIDs", idArray);
        document.setValue("CheckPoints", checkpointArray);

        Log.d("path_ db1 WayPoints", wpArray.toString());
        Log.d("path_ db1 WayPointIDs", idArray.toString());
        Log.d("path_ db1 checkpoints", checkpointArray.toString());
        try {
            database.save(document);
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
    }

   
    public void reset(View view) {
        mArFragment.getArSceneView().getSession().getAllAnchors().forEach(anchor -> anchor.detach());
        mAnchor = null;
    }


    
    
}
