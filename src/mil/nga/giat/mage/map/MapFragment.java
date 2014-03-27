package mil.nga.giat.mage.map;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import mil.nga.giat.mage.MAGE;
import mil.nga.giat.mage.R;
import mil.nga.giat.mage.map.GoogleMapWrapper.OnMapPanListener;
import mil.nga.giat.mage.observation.ObservationEditActivity;
import mil.nga.giat.mage.sdk.location.LocationService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.CancelableCallback;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.TileProvider;

/**
 * TODO : What does this do?
 * 
 * @author newmanw
 * 
 */

public class MapFragment extends Fragment implements 
    OnMapLongClickListener, 
    OnMapPanListener,
    OnMyLocationButtonClickListener,
    OnClickListener, 
    LocationSource, 
    LocationListener {

    private GoogleMap map;
    private int mapType = 1;
    private Location location;
    private boolean followMe = false;
    private GoogleMapWrapper mapWrapper;
    private OnLocationChangedListener locationChangedListener;
    private Map<String, TileOverlay> tileOverlays = new HashMap<String, TileOverlay>();
        
    private LocationService locationService;
    
    SharedPreferences preferences;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);
        
        mapWrapper = new GoogleMapWrapper(getActivity());
        mapWrapper.addView(view);
        
        preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        map = ((SupportMapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();

        mapType = Integer.parseInt(preferences.getString("baseLayer", "1"));
        map.setMapType(mapType);

        map.setOnMapLongClickListener(this);
        map.setOnMyLocationButtonClickListener(this);

        ImageButton mapSettings = (ImageButton) view.findViewById(R.id.map_settings);
        mapSettings.setOnClickListener(this);
        
        locationService = ((MAGE) getActivity().getApplication()).getLocationService();

        return mapWrapper;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Check if any map preferences changed that I care about        
        boolean locationServiceEnabled = preferences.getBoolean("locationServiceEnabled", false);
        map.setMyLocationEnabled(locationServiceEnabled);

        if (locationServiceEnabled) {
            map.setLocationSource(this);
            locationService.registerOnLocationListener(this);
        }

        updateMapType();
        updateMapOverlays();
    }
    
    @Override
    public void onPause() {
        super.onPause();

        boolean locationServiceEnabled = Integer.parseInt(preferences.getString("userReportingFrequency", "0")) > 0;
        if (locationServiceEnabled) {
            map.setLocationSource(null);
            locationService.unregisterOnLocationListener(this);
        }
    }
    
    @Override
    public void onMapLongClick(LatLng point) {
        // TODO Auto-generated method stub
        Intent intent = new Intent(getActivity(), ObservationEditActivity.class);
        intent.putExtra("latitude", point.latitude);
        intent.putExtra("longitude", point.longitude);
        startActivity(intent);        
    }
    
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.map_settings: {
                Intent i = new Intent(getActivity(), MapPreferencesActivity.class);
                startActivity(i);
                break;
            }
        }
    }
    
    @Override
    public boolean onMyLocationButtonClick() {
        
        if (location != null) {
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            float zoom = map.getCameraPosition().zoom < 15 ? 15 : map.getCameraPosition().zoom;
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom), new CancelableCallback() {

                @Override
                public void onCancel() {
                    mapWrapper.setOnMapPanListener(MapFragment.this);
                    followMe = true;
                }

                @Override
                public void onFinish() {
                    mapWrapper.setOnMapPanListener(MapFragment.this);
                    followMe = true;
                }
            });
        }
                
        return true;
    }

    @Override
    public void activate(OnLocationChangedListener listener) {
        locationChangedListener = listener;
    }

    @Override
    public void deactivate() {
        locationChangedListener = null;        
    }
    
    @Override
    public void onMapPan() {
        mapWrapper.setOnMapPanListener(null);
        followMe = false;
    }
    
    @Override
    public void onLocationChanged(Location location) {
        this.location = location;
        if (locationChangedListener != null) {
            locationChangedListener.onLocationChanged(location);
        }    
        
        if (followMe) {
            LatLngBounds bounds = map.getProjection().getVisibleRegion().latLngBounds;
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            if (!bounds.contains(latLng)) {
                // Move the camera to the user's location once it's available!
                map.animateCamera(CameraUpdateFactory.newLatLng(latLng));
            }
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onProviderDisabled(String provider) {}
    
    private void updateMapType() {
        int mapType = Integer.parseInt(preferences.getString("mapBaseLayer", "1"));
        if (mapType != this.mapType) {
            this.mapType = mapType;
            map.setMapType(this.mapType);
        }
    }

    private void updateMapOverlays() {
        Set<String> overlays = preferences.getStringSet("mapTileOverlays", Collections.<String> emptySet());

        // Add all overlays that are in the preferences
        // For now there is no ordering in how tile overlays are stacked

        Set<String> removedOverlays = new HashSet<String>(tileOverlays.keySet());
        for (String overlay : overlays) {
            if (!tileOverlays.keySet().contains(overlay)) {
                TileProvider tileProvider = new FileSystemTileProvider(256, 256, overlay);
                TileOverlay tileOverlay = map.addTileOverlay(new TileOverlayOptions().tileProvider(tileProvider));
                tileOverlays.put(overlay, tileOverlay);
            }

            removedOverlays.remove(overlay);
        }

        // Remove any overlays that are on the map but no longer in the
        // preferences
        for (String overlay : removedOverlays) {
            tileOverlays.remove(overlay).remove();
        }
    }
}