package mil.nga.giat.mage.map.marker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import mil.nga.giat.mage.filter.Filter;
import mil.nga.giat.mage.observation.ObservationViewActivity;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.MarkerManager;
import com.vividsolutions.jts.geom.Point;

public class ObservationMarkerCollection implements ObservationCollection, OnMarkerClickListener {

    private GoogleMap map;
    private Context context;
    private Collection<Filter<Observation>> filters = new ArrayList<Filter<Observation>>();

    private boolean collectionVisible = true;

    private Map<Long, Marker> observationIdToMarker = new ConcurrentHashMap<Long, Marker>();
    private Map<String, Observation> markerIdToObservation = new ConcurrentHashMap<String, Observation>();

    private MarkerManager.Collection markerCollection;

    public ObservationMarkerCollection(Context context, GoogleMap map) {
        this.map = map;
        this.context = context;

        MarkerManager markerManager = new MarkerManager(map);
        markerCollection = markerManager.newCollection();
        map.setOnMarkerClickListener(this);
    }

    @Override
    public void add(Observation o) {
        Point point = (Point) o.getObservationGeometry().getGeometry();
        MarkerOptions options = new MarkerOptions()
            .position(new LatLng(point.getY(), point.getX()))
            .icon(ObservationBitmapFactory.bitmapDescriptor(context, o))
            .visible(isObservationVisible(o));

        Marker marker = markerCollection.addMarker(options);

        observationIdToMarker.put(o.getId(), marker);
        markerIdToObservation.put(marker.getId(), o);
    }

    @Override
    public void addAll(Collection<Observation> observations) {
        for (Observation o : observations) {
            add(o);
        }
    }

    @Override
    public Collection<Observation> getObservations() {
        return markerIdToObservation.values();
    }

    @Override
    public void setVisible(boolean collectionVisible) {
        if (this.collectionVisible == collectionVisible)
            return;
        
        this.collectionVisible = collectionVisible;
        for (Marker m : observationIdToMarker.values()) {
            Observation o = markerIdToObservation.get(m.getId());
            m.setVisible(isObservationVisible(o));
        }
    }

    @Override
    public void setObservationVisibility(Observation o, boolean visible) {        
        observationIdToMarker.get(o.getId()).setVisible(this.collectionVisible && visible);
    }

    @Override
    public void remove(Observation o) {
        Marker marker = observationIdToMarker.remove(o.getId());
        if (marker != null) {
            markerIdToObservation.remove(marker.getId());
            markerCollection.remove(marker);
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        Observation o = markerIdToObservation.get(marker.getId());
        
        if (o == null) return false;  // Not an observation let someone else handle it

        Intent intent = new Intent(context, ObservationViewActivity.class);
        intent.putExtra(ObservationViewActivity.OBSERVATION_ID, o.getId());
        intent.putExtra(ObservationViewActivity.INITIAL_LOCATION, map.getCameraPosition().target);
        intent.putExtra(ObservationViewActivity.INITIAL_ZOOM, map.getCameraPosition().zoom);
        context.startActivity(intent);

        return true;
    }

    @Override
    public void clear() {
        observationIdToMarker.clear();
        markerIdToObservation.clear();
        markerCollection.clear();
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        // do nothing I don't care
    }

    @Override
    public void setFilters(Collection<Filter<Observation>> filters) {
        this.filters = filters;

        // re-filter based on new filter
        new FilterObservationsTask().execute();
    }

    private boolean isObservationVisible(Observation o) {
        boolean isVisible = collectionVisible;

        // Only check filter if the collection is visible
        if (isVisible) {
            for (Filter<Observation> filter : filters) {
                if (!filter.passesFilter(o)) {
                    isVisible = false;
                    break;
                }
            }
        }

        return isVisible;
    }

    private class FilterObservationsTask extends AsyncTask<Void, Map<Observation, Boolean>, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            for (Observation o : markerIdToObservation.values()) {
                publishProgress(Collections.<Observation, Boolean> singletonMap(o, isObservationVisible(o)));
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Map<Observation, Boolean>... observations) {
            for (Map.Entry<Observation, Boolean> entry : observations[0].entrySet()) {
                ObservationMarkerCollection.this.setObservationVisibility(entry.getKey(), entry.getValue());
            }            
        }
    }
}