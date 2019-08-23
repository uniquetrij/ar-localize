package com.infy.estquido;


import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.ar.sceneform.math.Vector3;

import org.apache.commons.collections4.IterableUtils;


public class DijikstrasShortestPath {

    private static final int NO_PARENT = -1;
    private int nVertices = 0;
    private float[] shortestDistances = new float[nVertices];
    private int[] parents = new int[nVertices];
    private float[][] adjacencyMatrix = {{}};



    private void dijkstra(int startVertex, int endVertex) {
        nVertices = adjacencyMatrix[0].length;
        shortestDistances = new float[nVertices];
        boolean[] added = new boolean[nVertices];


        for (int vertexIndex = 0; vertexIndex < nVertices; vertexIndex++) {
            shortestDistances[vertexIndex] = Integer.MAX_VALUE;
            added[vertexIndex] = false;
        }

        shortestDistances[startVertex] = 0;
        parents = new int[nVertices];
        parents[startVertex] = NO_PARENT;

        for (int i = 1; i < nVertices; i++) {
            int nearestVertex = -1;
            float shortestDistance = Float.MAX_VALUE;
            for (int vertexIndex = 0; vertexIndex < nVertices; vertexIndex++) {
                if (!added[vertexIndex] && shortestDistances[vertexIndex] < shortestDistance) {
                    nearestVertex = vertexIndex;
                    shortestDistance = shortestDistances[vertexIndex];
                }
            }


            added[nearestVertex] = true;
            for (int vertexIndex = 0; vertexIndex < nVertices; vertexIndex++) {
                float edgeDistance = adjacencyMatrix[nearestVertex][vertexIndex];

                if (edgeDistance > 0 && ((shortestDistance + edgeDistance) < shortestDistances[vertexIndex])) {
                    parents[vertexIndex] = nearestVertex;
                    shortestDistances[vertexIndex] = shortestDistance + edgeDistance;
                }
            }
        }
    }

    public List<Integer> getSolution(int endVertex, int[] parents)
    {
        List<Integer> path = new ArrayList<Integer>();
        path = getPath(endVertex, parents, path);

        return path;

    }


    private List<Integer> getPath(int currentVertex, int[] parents, List<Integer> path) {
        if (currentVertex == NO_PARENT) {
            return path;
        }
        getPath(parents[currentVertex], parents, path);
        path.add(currentVertex);
        return path;
    }

    private float getCost(Vector3 A, Vector3 B) {

        float X = Math.abs(A.x - B.x);
        float Y = Math.abs(A.y - B.y);
        float Z = Math.abs(A.z - B.z);

        return (float) Math.sqrt(X * X + Y * Y + Z * Z);

    }

    public void createAdjacencyMatrix(List<WayPoint> wayPoints) {

        float[][] adjmat = new float[wayPoints.size()][wayPoints.size()];

        for (int i = 0; i < wayPoints.size(); i++) {
            List<WayPoint> connections = new ArrayList<>(wayPoints.get(i).getConnections());
            Vector3 a = wayPoints.get(i).getPosition();
            for (int j = 0; j < connections.size(); j++) {

                Vector3 b = connections.get(j).getPosition();
                float cost = getCost(a, b);
                int idx;
                for(int k=0; k<wayPoints.size();k++){
                    if(connections.get(j).getWayPointName().equals(wayPoints.get(k).getWayPointName())){
                        idx = k;
                        adjmat[i][idx] = cost;
                        adjmat[idx][i] = cost;
                    }
                }





            }
        }

        this.adjacencyMatrix = adjmat;

    }

    public float[][] getAdjacencyMatrix() {
        return this.adjacencyMatrix;
    }




    private List<WayPoint> getPaths(List<WayPoint> wayPoints, List<WayPoint> checkpoints, List<Integer> checkPointIds) {

        List<Integer> tempids = new ArrayList<Integer>();

        for (int i = 0; i < checkPointIds.size(); i++) {
            if (!tempids.contains(i)) {


                Map<String, List<String>> ckptconnected = new HashMap<>();
                for (int j = 0; j < checkPointIds.size(); j++) {
                    if (i != j) {

                        int startVertex = checkPointIds.get(i);
                        int endVertex = checkPointIds.get(j);
                        dijkstra(startVertex, endVertex);

                        List<Integer> path = getSolution(endVertex, parents);
                        Map<String, List<String>> map = new HashMap<>();
                        List<String> nodePoints = new ArrayList<>();


                        for (int k = 0; k < path.size(); k++) {

                            List<Integer> finalPath = path;
                            int finalK = k;
                            WayPoint wayPoint = wayPoints.get(IterableUtils.indexOf(wayPoints, object -> object.getId().equals(finalPath.get(finalK))));
                            nodePoints.add(wayPoint.getWayPointName());


                        }


                        ckptconnected.put(checkpoints.get(j).getWayPointName(), nodePoints);

                    }
                }


                for (int j = 0; j < wayPoints.size(); j++) {
                    if (checkpoints.get(i).getWayPointName().equalsIgnoreCase(wayPoints.get(j).getWayPointName())) {
                        wayPoints.get(j).setCheckpointsPath(ckptconnected);
                    }
                }

            }
            tempids.add(i);
        }

        return wayPoints;
    }


    public List<WayPoint> getAllShortestPaths(List<WayPoint> wayPoints, Map<String, Integer> checkPointsList) {

        createAdjacencyMatrix(wayPoints);


        List<WayPoint> checkpoints = new ArrayList<WayPoint>();
        List<Integer> checkpointIds = new ArrayList<Integer>();
        for (String name : checkPointsList.keySet()) {
            WayPoint wayPoint = wayPoints.get(IterableUtils.indexOf(wayPoints, object -> object.getId().equals(checkPointsList.get(name))));
            checkpoints.add(wayPoint);
            checkpointIds.add(wayPoint.getId());
        }

        return getPaths(wayPoints, checkpoints, checkpointIds);


    }

    public List<WayPoint> getShortestPath(List<WayPoint> wayPoints, String src, String dst){

        createAdjacencyMatrix(wayPoints);

        WayPoint srcWayPoint = wayPoints.get(IterableUtils.indexOf(wayPoints, object -> object.getWayPointName().equalsIgnoreCase(src)));
        WayPoint dstWayPoint = wayPoints.get(IterableUtils.indexOf(wayPoints, object -> object.getWayPointName().equalsIgnoreCase(dst)));

        dijkstra(srcWayPoint.getId(), dstWayPoint.getId());

        List<Integer> path = getSolution(dstWayPoint.getId(), parents);


        Map<String, List<String>> map_src = new HashMap<>();
        List<String> nodePoints_src = new ArrayList<>();
        Map<String, List<String>> map_dst = new HashMap<>();


        for (int k = 0; k < path.size(); k++) {

            List<Integer> finalPath = path;
            int finalK = k;
            WayPoint wayPoint = wayPoints.get(IterableUtils.indexOf(wayPoints, object -> object.getId().equals(finalPath.get(finalK))));
            nodePoints_src.add(wayPoint.getWayPointName());

        }
        List<String> nodePoints_dst = new ArrayList<String>(nodePoints_src);
        Collections.reverse(nodePoints_dst);

        map_dst.put(srcWayPoint.getWayPointName(),nodePoints_dst);
        map_src.put(dstWayPoint.getWayPointName(),nodePoints_src);

        wayPoints.get(wayPoints.indexOf(srcWayPoint)).setCheckpointsPath(map_src);
        wayPoints.get(wayPoints.indexOf(dstWayPoint)).setCheckpointsPath(map_dst);

        return wayPoints;
    }


}

