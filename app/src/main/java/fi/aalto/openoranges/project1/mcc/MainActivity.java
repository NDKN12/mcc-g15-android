package fi.aalto.openoranges.project1.mcc;

import android.os.AsyncTask;
import android.os.Bundle;

import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.support.v4.app.ActivityCompat;
import android.location.Location;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private String mToken;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;

    private static final String LOG_TAG = "MainActivity";
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    OkHttpClient client = new OkHttpClient();
    private ApplicationList mAppList = null;


    private TextView mTextView;

    private String mAppsListTest = "void";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        mToken = getIntent().getStringExtra("token");
        // Toast.makeText(MainActivity.this, mToken, Toast.LENGTH_SHORT).show();

        mTextView = (TextView) findViewById(R.id.textView);
        // mListTextView = (TextView) findViewById(R.id.Liste);


        mAppList = new ApplicationList("test", "test");
        //mListTextView.setText(mListeTest);
        mAppList.execute((Void) null);

    }

    public Response getList(String url) throws IOException {
        return get(url);
    }

    public Response get(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", mToken)
                .build();
        return client.newCall(request).execute();
    }


    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class ApplicationList extends AsyncTask<Void, Void, Boolean> {

        private final String mLatitude;
        private final String mLongitude;

        ApplicationList(String latitude, String longitude) {
            mLatitude = latitude;
            mLongitude = longitude;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            String lat = "12.124124";
            String lng = "12.344345";
            String l = "?lat=" + lat + "&lng=" + lng;

            try {
                // Simulate network access.
                Response response = getList("https://mccg15.herokuapp.com/application" + l);
                int code = response.code();
                if (code == 200) {
                    mAppsListTest = response.body().string().toString();
                    Toast.makeText(MainActivity.this, mAppsListTest, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MainActivity.this, "Code: 401", Toast.LENGTH_LONG).show();
                    return false;
                }
            } catch (Exception i) {
                //Toast.makeText(MainActivity.this, "FAILURE", Toast.LENGTH_LONG).show();
                i.printStackTrace();
                Toast.makeText(MainActivity.this, "hallo" + i.getMessage(), Toast.LENGTH_LONG).show();
                return false;
            }

            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAppList = null;
            ArrayList<JSONObject> arrays = null;
            if (success) {

                mAppsListTest = "{'apps': " + mAppsListTest + "}";
                try {
                    JSONObject myjson = new JSONObject(mAppsListTest);
                    JSONArray the_json_array = myjson.getJSONArray("apps");
                    int size = the_json_array.length();
                    arrays = new ArrayList<JSONObject>();
                    for (int i = 0; i < size; i++) {
                        JSONObject another_json_object = the_json_array.getJSONObject(i);

                        arrays.add(another_json_object);
                    }

                    JSONObject[] jsons = new JSONObject[arrays.size()];
                    arrays.toArray(jsons);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                assert arrays != null;
                try {
                    Toast.makeText(MainActivity.this, arrays.get(0).getString("icon_url").toString(), Toast.LENGTH_LONG).show();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(MainActivity.this, "ApplicationList Failure!", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onCancelled() {
            mAppList = null;
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        //Connect the client
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        //Disconnect the client
        mGoogleApiClient.disconnect();
        super.onStop();
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        //Identifying location update parameters
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(1000); // Update location every second

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(LOG_TAG, "GoogleApiClient has been suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(LOG_TAG, "GoogleApiClient has been suspended");
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.i(LOG_TAG, location.toString());
        mTextView.setText(location.toString());
    }

}
