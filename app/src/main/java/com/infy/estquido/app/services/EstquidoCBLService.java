package com.infy.estquido.app.services;

import android.location.Location;
import android.util.Log;

import com.couchbase.lite.AbstractReplicator;
import com.couchbase.lite.BasicAuthenticator;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.DatabaseConfiguration;
import com.couchbase.lite.MutableDocument;
import com.couchbase.lite.Replicator;
import com.couchbase.lite.ReplicatorConfiguration;
import com.couchbase.lite.URLEndpoint;
import com.infy.estquido.app.This;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;


public class EstquidoCBLService {


    private interface OnFetchCompletedCallback {
        void onFetchCompleted(Map<String, Map<String, Object>> documents);
    }

    public interface OnCenterInferredCallback {
        void onCenterInferred(String center);
    }

    public interface OnSpotsFetchedCallback {
        void onSpotsFetched(Map<String, Object> map);
    }

    public interface OnBuildingsFetchedCallback {
        void onBuildingFetched(Map<String, Object> map);
    }


    private static final String TAG = EstquidoCBLService.class.getName();

    private static DatabaseConfiguration dbConfig;
    private static Database database;
    private static URLEndpoint endpoint;
    private static ReplicatorConfiguration reConfig;

    static {
        dbConfig = new DatabaseConfiguration(This.CONTEXT.get());
        try {
            database = new Database(This.Static.COUCHBASE_DB, dbConfig);
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
        try {
            endpoint = new URLEndpoint(new URI(This.Static.COUCHBASE_URL));
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        reConfig = new ReplicatorConfiguration(database, endpoint);
        reConfig.setAuthenticator(new BasicAuthenticator(This.Static.COUCHBASE_USER, This.Static.COUCHBASE_PASS));
    }

    public void fetch(OnFetchCompletedCallback callback, String... documents) {
        Map<String, Map<String, Object>> map = new HashMap<>();
        reConfig.setReplicatorType(ReplicatorConfiguration.ReplicatorType.PULL);
        reConfig.setDocumentIDs(Arrays.asList(documents));
        Replicator replicator = new Replicator(reConfig);
        replicator.addChangeListener(change -> {
            if (change.getStatus().getError() != null) {
                Log.e(TAG, "FETCH_ERROR " + change.getStatus().getError());
            }
            if (change.getStatus().getActivityLevel().equals(AbstractReplicator.ActivityLevel.STOPPED)) {
                Stream.of(documents).forEach(d -> {
                    try {
                        map.put(d, database.getDocument(d).toMap());
                    } catch (Exception e) {

                    }
                });
                Log.i(TAG, "FETCH_COMPLETED " + map.toString());
                callback.onFetchCompleted(map);
            }
        });
        replicator.start();
    }

    public void update(OnFetchCompletedCallback callback, String... documents) {
        Map<String, Map<String, Object>> map = new HashMap<>();
        reConfig.setReplicatorType(ReplicatorConfiguration.ReplicatorType.PUSH);
        reConfig.setDocumentIDs(Arrays.asList(documents));
        Replicator replicator = new Replicator(reConfig);
        replicator.addChangeListener(change -> {
            if (change.getStatus().getError() != null) {
                Log.e(TAG, "FETCH_ERROR " + change.getStatus().getError());
            }
            if (change.getStatus().getActivityLevel().equals(AbstractReplicator.ActivityLevel.STOPPED)) {
                Stream.of(documents).forEach(d -> {
                    try {
                        map.put(d, database.getDocument(d).toMap());
                    } catch (Exception e) {

                    }
                });
                Log.i(TAG, "FETCH_COMPLETED " + map.toString());
                callback.onFetchCompleted(map);
            }
        });
        replicator.start();
    }

    public static Database getDatabase() {
        return database;
    }

    public static void inferCenter(Location location, OnCenterInferredCallback callback) {
        new EstquidoCBLService().fetch(documents -> {
            Map<String, Object> map = documents.get("centers");
            Log.i(TAG, "CENTERS: " + map.toString());
            Optional<Map.Entry<String, Object>> min = map.entrySet().stream().min((Map.Entry<String, Object> e1, Map.Entry<String, Object> e2) -> {
                HashMap<String, Object> map1 = (HashMap<String, Object>) e1.getValue();
                HashMap<String, Object> map2 = (HashMap<String, Object>) e2.getValue();
                ArrayList<Double> location1 = (ArrayList<Double>) map1.get("location");
                ArrayList<Double> location2 = (ArrayList<Double>) map2.get("location");

                Location l1 = new Location("A");
                l1.setLatitude(location1.get(0));
                l1.setLongitude(location1.get(1));

                Location l2 = new Location("B");
                l2.setLatitude(location2.get(0));
                l2.setLongitude(location2.get(1));

                return Float.compare(location.distanceTo(l1), location.distanceTo(l2));
            });
            String center = min.get().getKey();
            Log.i(TAG, "CENTER: " + center);
            callback.onCenterInferred(center);

        }, "centers");
    }

    public static void fetchSpots(String center, OnSpotsFetchedCallback callback) {
        new EstquidoCBLService().fetch(documents -> {
            Map<String, Object> map = documents.get("spots_" + center);
            if (map == null) {
                MutableDocument document = new MutableDocument("spots_" + center);
                try {
                    database.save(document);
                } catch (CouchbaseLiteException e) {
                    e.printStackTrace();
                }
            }
            Log.i(TAG, "SPOTS: " + map.toString());
            callback.onSpotsFetched(map);
        }, "spots_" + center);
    }

    public static void fetchBuilding(String center, String building, OnBuildingsFetchedCallback callback) {
        new EstquidoCBLService().fetch(documents -> {
            Map<String, Object> map = documents.get("building_" + center + "_" + building);
            Log.i(TAG, "BUILDING: " + map);
            callback.onBuildingFetched(map);
        }, "building_" + center + "_" + building);
    }

    public static void updateBuilding(String center, String building, OnBuildingsFetchedCallback callback) {
        new EstquidoCBLService().update(documents -> {
            Map<String, Object> map = documents.get("building_" + center + "_" + building);
            Log.i(TAG, "BUILDING: " + map);
            callback.onBuildingFetched(map);
        }, "building_" + center + "_" + building);
    }
}
