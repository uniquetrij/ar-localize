package com.infy.estquido;

import android.graphics.Color;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.ar.core.Anchor;
import com.google.ar.core.Pose;
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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class CheckpointsActivity extends AppCompatActivity {


    private static final String TAG = CheckpointsActivity.class.getName();

    private ArFragment mArFragment;
    private Vector3 mCamPosition;


    private Set<WayPoint> mWayPoints = Collections.synchronizedSet(new LinkedHashSet<>());
    private WayPoint selectedWayPoint;

    private int wayPointID = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkpoints);
        mArFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.checkpoints_fragment);

        mArFragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> {
            Camera mARCamera = mArFragment.getArSceneView().getScene().getCamera();
            mCamPosition = mARCamera.getWorldPosition();
        });
    }

    public void placeWayPoint(View view) {
        mWayPoints.add(addWayPoint(++wayPointID + "", mCamPosition));
    }

    private WayPoint addWayPoint(String id, Vector3 position) {
        WayPoint wayPoint = new WayPoint(id, position);
        MaterialFactory.makeOpaqueWithColor(this, new com.google.ar.sceneform.rendering.Color(Color.parseColor("#FFBF00")))
                .thenAccept(material -> {
                    ModelRenderable modelRenderable = ShapeFactory.makeSphere(0.1f, new Vector3(0.0f, 0.0f, 0.0f), material);
                    Anchor anchor = mArFragment.getArSceneView().getSession().createAnchor(new Pose(new float[]{position.x, position.y, position.z}, new float[]{0, 0, 0, 0}));
                    AnchorNode anchorNode = new AnchorNode(anchor);
                    anchorNode.setParent(mArFragment.getArSceneView().getScene());

                    wayPoint.getNode().setParent(anchorNode);
                    wayPoint.getNode().setRenderable(modelRenderable);
                    wayPoint.getNode().setOnTapListener(new Node.OnTapListener() {
                        @Override
                        public void onTap(HitTestResult hitTestResult, MotionEvent motionEvent) {
                            if (!wayPoint.isSelected()) {
                                if (selectedWayPoint == null) {
                                    wayPoint.setSelected(true);
                                    wayPoint.getNode().getRenderable().getMaterial().setFloat3(MaterialFactory.MATERIAL_COLOR, new com.google.ar.sceneform.rendering.Color(Color.BLUE));
                                    selectedWayPoint = wayPoint;
                                } else {
                                    selectedWayPoint.setSelected(false);
                                    connectWayPoints(selectedWayPoint, wayPoint);
                                    selectedWayPoint.getNode().getRenderable().getMaterial().setFloat3(MaterialFactory.MATERIAL_COLOR, new com.google.ar.sceneform.rendering.Color(Color.parseColor("#FFBF00")));
                                    selectedWayPoint = null;

                                }
                            } else {
                                wayPoint.setSelected(false);
                                wayPoint.getNode().getRenderable().getMaterial().setFloat3(MaterialFactory.MATERIAL_COLOR, new com.google.ar.sceneform.rendering.Color(Color.parseColor("#FFBF00")));
                                selectedWayPoint = null;
                            }
                        }
                    });
                });
        return wayPoint;
    }

    private void connectWayPoints(WayPoint from, WayPoint to) {
        if (from.getConnections().contains(to))
            return;

        from.getConnections().add(to);
        to.getConnections().add(from);

        AnchorNode node1 = (AnchorNode) from.getNode().getParent();
        AnchorNode node2 = (AnchorNode) to.getNode().getParent();
        Vector3 point1, point2;
        point1 = node1.getWorldPosition();
        point2 = node2.getWorldPosition();

        final Vector3 difference = Vector3.subtract(point1, point2);
        final Vector3 directionFromTopToBottom = difference.normalized();
        final Quaternion rotationFromAToB = Quaternion.lookRotation(directionFromTopToBottom, Vector3.up());
        MaterialFactory.makeOpaqueWithColor(getApplicationContext(), new com.google.ar.sceneform.rendering.Color(Color.RED))
                .thenAccept(material -> {
                            ModelRenderable model = ShapeFactory.makeCube(new Vector3(0.025f, 0.025f, difference.length()), Vector3.zero(), material);
                            Anchor anchor = node2.getAnchor();
                            Node node = new Node();
                            node.setParent(node1);
                            node.setRenderable(model);
                            node.setWorldPosition(Vector3.add(point1, point2).scaled(.5f));
                            node.setWorldRotation(rotationFromAToB);

                            node.setOnTapListener(new Node.OnTapListener() {
                                @Override
                                public void onTap(HitTestResult hitTestResult, MotionEvent motionEvent) {
                                    node.setParent(null);
                                    from.getConnections().remove(to);
                                    to.getConnections().remove(from);
                                }
                            });
                        }
                );
    }


    public void syncPositions(View view) {

        mArFragment.getArSceneView().getSession().getAllAnchors().forEach(anchor -> anchor.detach());
        Set<WayPoint> oldWP = Collections.synchronizedSet(new LinkedHashSet<>(mWayPoints));
        Map<String, WayPoint> newWayPoints = new LinkedHashMap<>();
        mWayPoints.clear();

        oldWP.forEach(wayPoint -> {
            newWayPoints.put(wayPoint.getId(), addWayPoint(wayPoint.getId(), wayPoint.getPosition()));
        });

        Set<WayPoint> visited = new HashSet<>();

        oldWP.stream().sequential().forEach(wayPoint -> {
            visited.add(wayPoint);
            wayPoint.getConnections().forEach(wayPoint1 -> {
                if (!visited.contains(wayPoint1)) {
                    connectWayPoints(newWayPoints.get(wayPoint.getId()), newWayPoints.get(wayPoint1.getId()));
                }
            });
        });

        mWayPoints.addAll(newWayPoints.values());


    }
}
