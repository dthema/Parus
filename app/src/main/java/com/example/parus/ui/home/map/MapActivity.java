package com.example.parus.ui.home.map;


import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.example.parus.R;
import com.example.parus.databinding.ActivityMapBinding;
import com.example.parus.viewmodels.MapViewModel;
import com.example.parus.viewmodels.UserViewModel;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.location.LocationEngineRequest;
import com.mapbox.android.core.location.LocationEngineResult;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.plugins.annotation.Symbol;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.layers.PropertyValue;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textField;

public class MapActivity extends AppCompatActivity implements
        OnMapReadyCallback, PermissionsListener {

    private static final long DEFAULT_INTERVAL_IN_MILLISECONDS = 1000L;
    private static final long DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_IN_MILLISECONDS * 5;
    private static final String TAG = "MapActivityDebug";
    private MapboxMap mapboxMap;
    private PermissionsManager permissionsManager;
    private LocationEngine locationEngine;
    private LocationChangeListeningActivityLocationCallback callback =
            new LocationChangeListeningActivityLocationCallback(this);
    private Location userLocation;
    private Location linkUserLocation;
    private SymbolManager symbolManager;
    private UserViewModel userViewModel;
    private MapViewModel mapViewModel;
    private ActivityMapBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, "pk.eyJ1IjoiZHRoZW1hIiwiYSI6ImNrYW1kcGphMjEzMDQydHA2aDdxbGg1MTcifQ.zZZ1fAHOUWJ9OCuz6fVBZg");
        binding = DataBindingUtil.setContentView(this, R.layout.activity_map);
        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        mapViewModel = new ViewModelProvider(this).get(MapViewModel.class);
        binding.myLocation.setOnClickListener(l -> {
            if (PermissionsManager.areLocationPermissionsGranted(this) && userLocation != null) {
                LatLng latLng = new LatLng(userLocation);
                mapboxMap.setCameraPosition(new CameraPosition.Builder()
                        .target(latLng)
                        .zoom(15)
                        .build()
                );
            } else {
                permissionsManager = new PermissionsManager(this);
                permissionsManager.requestLocationPermissions(this);
            }
        });
        binding.myLocation.setVisibility(View.GONE);
        binding.linkLocation.setVisibility(View.GONE);
        binding.mapView.onCreate(savedInstanceState);
        binding.mapView.getMapAsync(this);
    }

    private static final String ID_ICON_1 = "com.mapbox.annotationplugin.icon.1";

    private Bitmap generateBitmap() {
        Drawable drawable = getResources().getDrawable(R.drawable.ic_adjust_black_24dp);
        return getBitmapFromDrawable(drawable);
    }

    static Bitmap getBitmapFromDrawable(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        } else {
            // width and height are equal for all assets since they are ovals.
            Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                    drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
            return bitmap;
        }
    }

    @Override
    public void onMapReady(@NonNull final MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;
        mapboxMap.setStyle(new Style.Builder()
                        // привязывание своего стиля для карты
                        .fromUri("mapbox://styles/dthema/ckan01is85ape1iqt89ohvcek")
                        // установка картинки для метки на местоположение связанного пользователя
                        .withImage(ID_ICON_1, generateBitmap(), true)
                , style -> {
                    binding.myLocation.setVisibility(View.VISIBLE);
                    binding.linkLocation.setVisibility(View.VISIBLE);
                    enableLocationComponent(style);
                    symbolManager = new SymbolManager(binding.mapView, mapboxMap, style);
                    PropertyValue<String> prop_ru = textField("{name_ru}");
                    List<String> idArr = getParamsList();
                    // перевод всех заголовков на русский язык
                    for (String id : idArr) {
                        Layer settlementLabelLayer = style.getLayer(id);
                        if (settlementLabelLayer != null)
                            settlementLabelLayer.setProperties(prop_ru);
                    }
                    userViewModel.getShortUserData().observe(this, pair -> {
                        if (pair.first == null)
                            return;
                        String userId = pair.first.first;
                        String linkUserId = pair.first.second;
                        Boolean isSupport = pair.second;
                        if (userId == null || linkUserId == null || isSupport == null)
                            return;
                        if (userId.equals(linkUserId)) {
                            if (linkUserLocation != null){
                                symbolManager.deleteAll();
                                linkUserLocation = null;
                            }
                            if (isSupport)
                                binding.linkLocation.setOnClickListener(c ->
                                        Toast.makeText(MapActivity.this, R.string.no_support_link, Toast.LENGTH_LONG).show());
                            else
                                binding.linkLocation.setOnClickListener(c ->
                                        Toast.makeText(MapActivity.this, R.string.no_disabled_link, Toast.LENGTH_LONG).show());
                        } else {
                            userViewModel.getSingleLinkUserData().observe(this, user -> {
                                if (!user.isCheckGeoPosition())
                                    if (isSupport)
                                        Toast.makeText(MapActivity.this, R.string.off_disabled_geoposition, Toast.LENGTH_LONG).show();
                                    else
                                        Toast.makeText(MapActivity.this, R.string.off_support_geoposition, Toast.LENGTH_LONG).show();
                            });
                            binding.linkLocation.setOnClickListener(c -> {
                                if (linkUserLocation != null) {
                                    LatLng latLng = new LatLng(linkUserLocation);
                                    mapboxMap.setCameraPosition(new CameraPosition.Builder()
                                            .target(latLng)
                                            .zoom(15)
                                            .build());
                                } else {
                                    if (isSupport)
                                        Toast.makeText(MapActivity.this, R.string.desabled_not_find, Toast.LENGTH_LONG).show();
                                    else
                                        Toast.makeText(MapActivity.this, R.string.support_not_find, Toast.LENGTH_LONG).show();
                                }
                            });
                            mapViewModel.getLocationData(linkUserId).observe(this, location -> {
                                if (location == null)
                                    return;
                                Double latitude = location.first;
                                Double longitude = location.second;
                                if (latitude == null || longitude == null)
                                    return;
                                if (linkUserLocation == null)
                                    linkUserLocation = new Location("");
                                linkUserLocation.setLongitude(longitude);
                                linkUserLocation.setLatitude(latitude);
                                // добавление метки на местоположение связанного пользователя
                                if (symbolManager != null) {
                                    symbolManager.deleteAll();
                                    symbolManager.setIconAllowOverlap(true);
                                    symbolManager.setTextAllowOverlap(true);
                                    SymbolOptions SymbolOptions = new SymbolOptions()
                                            .withLatLng(new LatLng(latitude, longitude))
                                            .withIconImage(ID_ICON_1);
                                    Symbol symbol = symbolManager.create(SymbolOptions);
                                }
                            });
                        }
                    });
                });
    }

    private List<String> getParamsList() {
        return Arrays.asList("country-label", "state-label", "settlement-major-label", "settlement-minor-label", "settlement-subdivision-label", "airport-label", "natural-point-label", "natural-line-label", "waterway-label", "path-pedestrian-label",
                "tunnel-oneway-arrows-blue-minor",
                "tunnel-oneway-arrows-blue-major",
                "tunnel-oneway-arrows-white",
                "tunnel-oneway-arrows-white",
                "turning-features-outline",
                "road-oneway-arrows-blue-minor",
                "road-oneway-arrows-blue-major",
                "level-crossings",
                "road-oneway-arrows-white",
                "turning-features",
                "bridge-oneway-arrows-blue-minor",
                "bridge-oneway-arrows-blue-major",
                "bridge-oneway-arrows-white",
                "road-label-small",
                "road-label-medium",
                "road-label-large",
                "road-shields-black",
                "road-shields-white",
                "motorway-junction",
                "waterway-label",
                "rail-label",
                "water-label-sm",
                "place-residential",
                "airport-label",
                "place-islet-archipelago-aboriginal",
                "place-neighbourhood",
                "place-suburb",
                "place-hamlet",
                "place-village",
                "place-town",
                "place-island",
                "place-city-sm",
                "place-city-md-s",
                "place-city-md-n",
                "place-city-lg-s",
                "place-city-lg-n",
                "marine-label-sm-ln",
                "marine-label-sm-pt",
                "marine-label-md-ln",
                "marine-label-md-pt",
                "marine-label-lg-ln",
                "marine-label-lg-pt",
                "state-label-sm",
                "state-label-md",
                "state-label-lg",
                "country-label-sm",
                "country-label-md",
                "country-label-lg");
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    private void enableLocationComponent(@NonNull Style loadedMapStyle) {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {

            // Get an instance of the component
            LocationComponent locationComponent = mapboxMap.getLocationComponent();

            // Set the LocationComponent activation options
            LocationComponentActivationOptions locationComponentActivationOptions =
                    LocationComponentActivationOptions.builder(this, loadedMapStyle)
                            .useDefaultLocationEngine(false)
                            .build();

            // Activate with the LocationComponentActivationOptions object
            locationComponent.activateLocationComponent(locationComponentActivationOptions);

            // Enable to make component visible
            locationComponent.setLocationComponentEnabled(true);

            // Set the component's camera mode
            locationComponent.setCameraMode(CameraMode.TRACKING);

            // Set the component's render mode
            locationComponent.setRenderMode(RenderMode.COMPASS);
            initLocationEngine();
        }
    }

    /**
     * Set up the LocationEngine and the parameters for querying the device's location
     */
    @SuppressLint("MissingPermission")
    private void initLocationEngine() {
        locationEngine = LocationEngineProvider.getBestLocationEngine(this);

        LocationEngineRequest request = new LocationEngineRequest.Builder(DEFAULT_INTERVAL_IN_MILLISECONDS)
                .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                .setMaxWaitTime(DEFAULT_MAX_WAIT_TIME).build();
        locationEngine.requestLocationUpdates(request, callback, getMainLooper());
        locationEngine.getLastLocation(callback);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(this, "Требуются права для обработки ваших геоданных",
                Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            mapboxMap.getStyle(this::enableLocationComponent);
        } else {
            Toast.makeText(this, "Не получилось вас обнаружить", Toast.LENGTH_LONG).show();
        }
    }

    private static class LocationChangeListeningActivityLocationCallback
            implements LocationEngineCallback<LocationEngineResult> {

        private final WeakReference<MapActivity> activityWeakReference;

        LocationChangeListeningActivityLocationCallback(MapActivity activity) {
            this.activityWeakReference = new WeakReference<>(activity);
        }

        /**
         * The LocationEngineCallback interface's method which fires when the device's location has changed.
         *
         * @param result the LocationEngineResult object which has the last known location within it.
         */
        @Override
        public void onSuccess(LocationEngineResult result) {
            MapActivity activity = activityWeakReference.get();
            if (activity != null) {
                Location location = result.getLastLocation();
                if (location == null) {
                    return;
                }
                if (activity.mapboxMap != null && result.getLastLocation() != null) {
                    activity.mapboxMap.getLocationComponent().forceLocationUpdate(result.getLastLocation());
                    activity.userLocation = location;
                }
            }
        }

        /**
         * The LocationEngineCallback interface's method which fires when the device's location can't be captured
         *
         * @param exception the exception message
         */
        @Override
        public void onFailure(@NonNull Exception exception) {
            MapActivity activity = activityWeakReference.get();
            if (activity != null) {
                Toast.makeText(activity, exception.getLocalizedMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        binding.mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        binding.mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        binding.mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        binding.mapView.onStop();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        binding.mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationEngine != null) {
            locationEngine.removeLocationUpdates(callback);
        }
        binding.mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        binding.mapView.onLowMemory();
    }
}