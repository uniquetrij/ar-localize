package com.infy.estquido;

import androidx.annotation.NonNull;

import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Vector3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WayPoint {
    private int id;
    private String wayPointName = null;
    private boolean isCheckpoint = false;
    private Map<String, List<String>> checkpointsPath;
    private Node node;
    private Vector3 position;
    private Set<WayPoint> connections;
    private boolean isSelected;
    private String type;

    public WayPoint(int id, Vector3 position, String wayPointName, boolean isCheckpoint) {
        this.id = id;
        this.wayPointName = wayPointName;
        this.isCheckpoint = isCheckpoint;
        this.position = position;
        this.node = new Node();
        this.connections = new HashSet<>();
    }

    public Integer getId() {
        return id;
    }

    public Node getNode() {
        return node;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public Set<WayPoint> getConnections() {
        return connections;
    }

    public Vector3 getPosition() {
        return position;
    }

    public boolean getIsCheckpoint() {
        return this.isCheckpoint;
    }

    public String getWayPointName() {
        return wayPointName;
    }

    public void setIsCheckpoint(boolean isCheckpoint) {
        this.isCheckpoint = isCheckpoint;
    }

    public void setWayPointName(String waypointName) {
        this.wayPointName = waypointName;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("wayPointName", wayPointName);
        map.put("isCheckpoint", isCheckpoint);
        map.put("x", position.x);
        map.put("y", position.y);
        map.put("z", position.z);
        map.put("type", type);

        List<String> list = new ArrayList<>();
        connections.stream().forEachOrdered(x -> list.add(x.wayPointName));
        map.put("connections", list);

        map.put("routes", checkpointsPath);

        return map;
    }

    public Map<String, List<String>> getCheckpointsPath() {
        return checkpointsPath;
    }

    public void setCheckpointsPath(Map<String, List<String>> checkpointsPath) {
        this.checkpointsPath = checkpointsPath;
    }


}
