package com.kviation.android.sample.location;

import android.app.ListActivity;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Date;

public class LocationTest extends ListActivity {

  private static final String EXTRA_LOCATIONS = "com.kviation.android.sample.location.LOCATIONS";

  /**
   * Handles serializing/deserializing the location readings.
   */
  private static class LocationUpdate implements Parcelable {
    @SuppressWarnings("unused")
    public static final Parcelable.Creator<LocationUpdate> CREATOR =
        new Parcelable.Creator<LocationUpdate>() {
          public LocationUpdate createFromParcel(Parcel in) {
            return new LocationUpdate(in);
          }

          public LocationUpdate[] newArray(int size) {
            return new LocationUpdate[size];
          }
        };

    public String coords;
    public String details;

    public LocationUpdate(String coords, String details) {
      this.coords = coords;
      this.details = details;
    }

    private LocationUpdate(Parcel in) {
      coords = in.readString();
      details = in.readString();
    }

    @Override
    public int describeContents() {
      return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
      out.writeString(coords);
      out.writeString(details);
    }
  }

  /**
   * Receives all location updates.
   */
  private class MyLocationListener implements LocationListener {
    @Override
    public void onLocationChanged(Location location) {
      addLocation(location, false);
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }
  }

  private static String formatTimestamp(long timestamp) {
    return String.format("%1$tH:%1$tM:%1$tS.%1$tL", new Date(timestamp));
  }

  private static boolean isAccurate(Location location) {
    if (location == null) {
      return false;
    }

    // Accuracy must be 500 meters or better
    if (location.hasAccuracy() && location.getAccuracy() > 500) {
      return false;
    }

    return true;
  }

  private static boolean isFresh(Location location) {
    if (location == null) {
      return false;
    }

    // Age must be no more than 1 minute
    long age = System.currentTimeMillis() - location.getTime();
    if (age > 60000) {
      return false;
    }

    return true;
  }

  private CheckBox cachedButton;
  private CheckBox gpsButton;
  private LocationListener gpsLocationListener;
  private ArrayAdapter<LocationUpdate> listAdapter;
  private LocationManager locationManager;
  private EditText minDistance;
  private EditText minTime;
  private CheckBox networkButton;
  private LocationListener networkLocationListener;
  private Button startButton;
  private Button stopButton;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

    final LayoutInflater layoutInflater = LayoutInflater.from(this);
    // Location renderer
    listAdapter = new ArrayAdapter<LocationUpdate>(this,
        R.layout.location_update) {
      @Override
      public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
          convertView = layoutInflater.inflate(R.layout.location_update, null);
        }

        LocationUpdate locationUpdate = getItem(position);
        ((TextView) convertView.findViewById(R.id.coords))
            .setText(locationUpdate.coords);
        ((TextView) convertView.findViewById(R.id.details))
            .setText(locationUpdate.details);

        return convertView;
      }
    };
    setListAdapter(listAdapter);
    setContentView(R.layout.location_test);

    gpsButton = (CheckBox) findViewById(R.id.gps);
    networkButton = (CheckBox) findViewById(R.id.network);
    cachedButton = (CheckBox) findViewById(R.id.cached);
    minTime = (EditText) findViewById(R.id.minTime);
    minDistance = (EditText) findViewById(R.id.minDistance);
    startButton = (Button) findViewById(R.id.start);
    stopButton = (Button) findViewById(R.id.stop);

    gpsLocationListener = new MyLocationListener();
    networkLocationListener = new MyLocationListener();
    locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

    startButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        startListening();
      }
    });

    stopButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        stopListening();
      }
    });

    setDefaults();
  }

  @Override
  protected void onPause() {
    super.onPause();
    stopListening();
  }

  @Override
  protected void onRestoreInstanceState(Bundle state) {
    super.onRestoreInstanceState(state);

    ArrayList<Parcelable> locations = state.getParcelableArrayList(EXTRA_LOCATIONS);
    listAdapter.clear();
    for (Parcelable location : locations) {
      listAdapter.add((LocationUpdate) location);
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    ArrayList<LocationUpdate> locations = new ArrayList<LocationUpdate>();
    for (int i = 0, size = listAdapter.getCount(); i < size; i++) {
      locations.add(listAdapter.getItem(i));
    }
    outState.putParcelableArrayList(EXTRA_LOCATIONS, locations);
  }

  private void addLocation(Location location, boolean isCached) {
    if (location == null) {
      return;
    }

    boolean isAccurate = isAccurate(location);
    boolean isFresh = isFresh(location);

    String coords = String.format("%f, %f, %.1f", location.getLatitude(),
        location.getLongitude(), location.getAltitude());
    String details = String.format("%s, %.0f m, %s", location
        .getProvider().toUpperCase(), location.getAccuracy(), formatTimestamp(location.getTime()));

    if (isCached) {
      details += (", " + getString(R.string.location_cached));
    }

    if (!isAccurate) {
      details += (", " + getString(R.string.location_inaccurate));
    }

    if (!isFresh) {
      details += (", " + getString(R.string.location_old));
    }

    LocationUpdate locationUpdate = new LocationUpdate(coords, details);
    listAdapter.add(locationUpdate);
    listAdapter.notifyDataSetChanged();
  }

  private float getMinDistance() {
    try {
      return Float.parseFloat(minDistance.getText().toString());
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  private long getMinTime() {
    try {
      return Long.parseLong(minTime.getText().toString());
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  private void setDefaults() {
    gpsButton.setChecked(true);
    networkButton.setChecked(true);
    stopButton.setEnabled(false);
  }

  private void startListening() {
    gpsButton.setEnabled(false);
    networkButton.setEnabled(false);
    cachedButton.setEnabled(false);
    startButton.setEnabled(false);
    stopButton.setEnabled(true);

    listAdapter.clear();
    listAdapter.notifyDataSetChanged();
    setProgressBarIndeterminateVisibility(true);

    LocationUpdate locationUpdate =
        new LocationUpdate("Started listening", formatTimestamp(System.currentTimeMillis()));
    listAdapter.add(locationUpdate);

    if (cachedButton.isChecked()) {
      addLocation(locationManager
          .getLastKnownLocation(LocationManager.NETWORK_PROVIDER), true);
      addLocation(locationManager
          .getLastKnownLocation(LocationManager.GPS_PROVIDER), true);
    }

    if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        && gpsButton.isChecked()) {
      locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
          getMinTime(), getMinDistance(), gpsLocationListener);
    }

    if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        && networkButton.isChecked()) {
      locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
          getMinTime(), getMinDistance(), networkLocationListener);
    }
  }

  private void stopListening() {
    gpsButton.setEnabled(true);
    networkButton.setEnabled(true);
    cachedButton.setEnabled(true);
    startButton.setEnabled(true);
    stopButton.setEnabled(false);
    setProgressBarIndeterminateVisibility(false);

    locationManager.removeUpdates(gpsLocationListener);
    locationManager.removeUpdates(networkLocationListener);
  }
}
