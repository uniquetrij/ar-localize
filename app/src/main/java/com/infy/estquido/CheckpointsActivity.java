package com.infy.estquido;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.couchbase.lite.Database;
import com.couchbase.lite.MutableDocument;
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

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class CheckpointsActivity extends AppCompatActivity {


    private static final String TAG = CheckpointsActivity.class.getName();
    private static final String DB_NAME = "CBDB";

    private ArFragment mArFragment;
    private Vector3 position;
    private Vector3 mCalibPose = new Vector3(0, 0, 0);
    private Database database;
    private MutableDocument mutableDoc;
    private int counter;

    private Map<Node, WayPoint> nodes = new LinkedHashMap<>();

    private Node selected;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkpoints);
        mArFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.checkpoints_fragment);

        mArFragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> {
            Camera mARCamera = mArFragment.getArSceneView().getScene().getCamera();
            position = mARCamera.getWorldPosition();
        });
    }

    public void addWayPoint(View view) {
        MaterialFactory.makeOpaqueWithColor(this, new com.google.ar.sceneform.rendering.Color(Color.parseColor("#FFBF00")))
                .thenAccept(material -> {
                    ModelRenderable modelRenderable = ShapeFactory.makeSphere(0.1f, new Vector3(0.0f, 0.0f, 0.0f), material);
                    Anchor anchor = mArFragment.getArSceneView().getSession().createAnchor(new Pose(new float[]{position.x, position.y, position.z}, new float[]{0, 0, 0, 0}));
                    AnchorNode anchorNode = new AnchorNode(anchor);
                    anchorNode.setParent(mArFragment.getArSceneView().getScene());

                    Node node = new Node();
                    node.setParent(anchorNode);
                    node.setRenderable(modelRenderable);

                    nodes.put(node, new WayPoint(++counter + "", new Vector3(position.x, position.y, position.z)));

                    node.setOnTapListener(new Node.OnTapListener() {
                        @Override
                        public void onTap(HitTestResult hitTestResult, MotionEvent motionEvent) {
                            WayPoint wayPoint = nodes.get(node);
                            if (!wayPoint.isSelected()) {
                                if (selected == null) {
                                    wayPoint.setSelected(true);
                                    node.getRenderable().getMaterial().setFloat3(MaterialFactory.MATERIAL_COLOR, new com.google.ar.sceneform.rendering.Color(Color.BLUE));
                                    selected = node;
                                } else {
                                    nodes.get(selected).setSelected(false);
                                    if (!nodes.get(selected).getConnected().contains(nodes.get(node))) {
                                        connectNodes(selected, node);
                                    }

                                    selected.getRenderable().getMaterial().setFloat3(MaterialFactory.MATERIAL_COLOR, new com.google.ar.sceneform.rendering.Color(Color.parseColor("#FFBF00")));
                                    nodes.get(selected).getConnected().add(nodes.get(node));
                                    nodes.get(node).getConnected().add(nodes.get(selected));
                                    selected = null;

                                }
                            } else {
                                wayPoint.setSelected(false);
                                node.getRenderable().getMaterial().setFloat3(MaterialFactory.MATERIAL_COLOR, new com.google.ar.sceneform.rendering.Color(Color.parseColor("#FFBF00")));
                                selected = null;
                            }
                        }
                    });
                });
    }

    private void connectNodes(Node from, Node to) {
        AnchorNode node1 = (AnchorNode) from.getParent();
        AnchorNode node2 = (AnchorNode) to.getParent();
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
                                    nodes.get(from).getConnected().remove(nodes.get(node));
                                    nodes.get(to).getConnected().remove(nodes.get(from));
                                }
                            });
                        }
                );
    }


    public void syncOrigin(View view) {

        Map<Node, WayPoint> tempNodes = new LinkedHashMap<>(this.nodes);
        Map<WayPoint, Node> tempWP = new LinkedHashMap<>();
        Set<WayPoint> visited = new HashSet<>();
        nodes.clear();
        selected = null;
        mArFragment.getArSceneView().getSession().getAllAnchors().forEach(x -> x.detach());

        tempNodes.entrySet().forEach(entry -> {
            Vector3 position = entry.getValue().getPosition();
            MaterialFactory.makeOpaqueWithColor(this, new com.google.ar.sceneform.rendering.Color(Color.parseColor("#FFBF00")))
                    .thenAccept(material -> {
                        ModelRenderable modelRenderable = ShapeFactory.makeSphere(0.1f, new Vector3(0.0f, 0.0f, 0.0f), material);
                        Anchor anchor = mArFragment.getArSceneView().getSession().createAnchor(new Pose(new float[]{position.x, position.y, position.z}, new float[]{0, 0, 0, 0}));
                        AnchorNode anchorNode = new AnchorNode(anchor);
                        anchorNode.setParent(mArFragment.getArSceneView().getScene());

                        Node node = new Node();
                        node.setParent(anchorNode);
                        node.setRenderable(modelRenderable);

                        nodes.put(node, entry.getValue());
                        tempWP.put(entry.getValue(), node);


                        node.setOnTapListener(new Node.OnTapListener() {
                            @Override
                            public void onTap(HitTestResult hitTestResult, MotionEvent motionEvent) {
                                WayPoint wayPoint = nodes.get(node);
                                if (!wayPoint.isSelected()) {
                                    if (selected == null) {
                                        wayPoint.setSelected(true);
                                        node.getRenderable().getMaterial().setFloat3(MaterialFactory.MATERIAL_COLOR, new com.google.ar.sceneform.rendering.Color(Color.BLUE));
                                        selected = node;
                                    } else {
                                        nodes.get(selected).setSelected(false);
                                        if (!nodes.get(selected).getConnected().contains(nodes.get(node))) {
                                            connectNodes(selected, node);
                                        }
                                        selected.getRenderable().getMaterial().setFloat3(MaterialFactory.MATERIAL_COLOR, new com.google.ar.sceneform.rendering.Color(Color.parseColor("#FFBF00")));
                                        nodes.get(selected).getConnected().add(nodes.get(node));
                                        nodes.get(node).getConnected().add(nodes.get(selected));
                                        selected = null;

                                    }
                                } else {
                                    wayPoint.setSelected(false);
                                    node.getRenderable().getMaterial().setFloat3(MaterialFactory.MATERIAL_COLOR, new com.google.ar.sceneform.rendering.Color(Color.parseColor("#FFBF00")));
                                    selected = null;
                                }
                            }
                        });
                    });


        });

        Log.d("ENTRY", nodes.values().toString());

        for (Map.Entry<Node, WayPoint> entry : nodes.entrySet()) {
            visited.add(entry.getValue());
            for (WayPoint wayPoint : entry.getValue().getConnected()) {
                if (visited.contains(wayPoint)) {
                    continue;
                }
                connectNodes(entry.getKey(), tempWP.get(wayPoint));
            }

        }


    }
}
