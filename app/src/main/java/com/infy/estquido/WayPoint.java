package com.infy.estquido;

import com.google.ar.sceneform.math.Vector3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WayPoint {
    private String id;
    private Vector3 position;
    private Set<WayPoint> connected;

    private boolean isSelected;

    public WayPoint(String id, Vector3 position) {
        this.id = id;
        this.position = position;
        this.connected = new HashSet<>();
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public Vector3 getPosition() {
        return position;
    }

    public String getId() {
        return id;
    }

    public Set<WayPoint> getConnected() {
        return connected;
    }
}
