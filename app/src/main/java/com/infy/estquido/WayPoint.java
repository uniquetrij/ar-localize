package com.infy.estquido;

import com.google.ar.sceneform.math.Vector3;

import java.util.List;

public class WayPoint {



    private String id;
    private Vector3 position;
    private List<WayPoint> connected;

    private boolean isSelected;

    public WayPoint(String id, Vector3 position) {
        this.id = id;
        this.position = position;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}
