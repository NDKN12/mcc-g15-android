package fi.aalto.openoranges.project1.mcc;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.view.SimpleDraweeView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import androidVNC.ConnectionBean;
import androidVNC.VncCanvasActivity;
import androidVNC.VncConstants;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.pow;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private String mToken;
    private int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;

    private static final String LOG_TAG = "MainActivity";
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    OkHttpClient client = new OkHttpClient();
    private ApplicationList mAppList = null;
    private getApplicationTask mGetAppTask = null;

    private TextView mTextView;
    private TextView mTextViewTest;

    private String mAppsListTest = "void";
    private ArrayList<JSONObject> arrays = null;
    private List<Application> myApps = new ArrayList<Application>();

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLogoutTask mAuthTask = null;
    private View mProgressView;
    private View mListView;
    private double mLatitude;
    private double mLongitude;
    private double mUsedLatitude;
    private double mUsedLongitude;
    private int mResumeCounter = 0;
    private int mResumeTest;
    private double mDistance;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fresco.initialize(this);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mListView = findViewById(R.id.oo_AppsListView);
        mProgressView = findViewById(R.id.logout_progress);

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        mToken = getIntent().getStringExtra("token");

        mTextView = (TextView) findViewById(R.id.textView);
        mTextViewTest = (TextView) findViewById(R.id.textViewTest);
        // mListTextView = (TextView) findViewById(R.id.Liste);


        Button logoutButton = (Button) findViewById(R.id.logout);
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogout();
            }
        });

        registerClickCallback();
    }

    private void registerClickCallback() {
        ListView list = (ListView) findViewById(R.id.oo_AppsListView);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View viewClicked, int position, long id) {
                Application clickedApp = myApps.get(position);
                String message = "Launching " + clickedApp.getName() + " wait for the VM to start!";

                mGoogleApiClient.disconnect();
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                showProgress(true);
                mGetAppTask = new getApplicationTask(clickedApp.getId());
                mGetAppTask.execute((Void) null);
            }
        });
    }

    private void populateListView() {
        ArrayAdapter<Application> adapter = new MyListAdapter();
        ListView list = (ListView) findViewById(R.id.oo_AppsListView);
        list.setAdapter(adapter);
    }

    private class MyListAdapter extends ArrayAdapter<Application> {
        public MyListAdapter() {
            super(MainActivity.this, R.layout.item_view, myApps);
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            //Make sure to have a view to work with
            View itemView = convertView;
            if (itemView == null)
                itemView = getLayoutInflater().inflate(R.layout.item_view, parent, false);
            //find app to work with
            Application currentApp = myApps.get(position);

            //Fill the view
            Uri uri = Uri.parse(currentApp.getIcon_url());
            SimpleDraweeView draweeView = (SimpleDraweeView) itemView.findViewById(R.id.imageView);
            try {
                draweeView.setImageURI(uri);
            } catch (Exception i) {
                i.printStackTrace();
            }


            //Fill the textview with the name of the app
            TextView nameText = (TextView) itemView.findViewById(R.id.nameText);
            nameText.setText(currentApp.getName());

            //Fill the textview with the description of the app
            //TextView descriptionText = (TextView) itemView.findViewById(R.id.nameText);
            //descriptionText.setText(currentApp.getDescription());
            return itemView;
        }
    }


    private void populateAppList() {
        myApps = new ArrayList<>();
        for (int j = 0; j < arrays.size(); j++) {
            String name = null;
            String id = null;
            String icon_url = null;
            String description = null;
            try {
                name = arrays.get(j).getString("name");
                id = arrays.get(j).getString("id");
                icon_url = arrays.get(j).getString("icon_url");
                description = arrays.get(j).getString("description");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            myApps.add(new Application(name, id, icon_url, description));
        }
        showProgress(false);
        populateListView();
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
     * Represents an asynchronous task used to get the address of the chosen application
     */
    public class getApplicationTask extends AsyncTask<Void, Void, Boolean> {
        private final String mId;
        private String mVmUrl;

        public getApplicationTask(String id) {
            mId = id;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            int counter = 0;
            try {
                // Simulate network access.
                Response response = MainActivity.this.get("https://mccg15.herokuapp.com/application/" + mId);
                int code = response.code();

                if (code == 200) {
                    JSONObject myjson = new JSONObject(response.body().string().toString());
                    mVmUrl = myjson.getString("vm_url");
                    return true;
                } else if (code == 202) {
                    while (counter < 20) {
                        Thread.sleep(10000);
                        response = MainActivity.this.get("https://mccg15.herokuapp.com/application/" + mId);
                        if (response.code() == 200) {
                            JSONObject myjson = new JSONObject(response.body().string().toString());
                            mVmUrl = myjson.getString("vm_url");
                            return true;
                        } else if (response.code() == 202) {
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    Toast.makeText(MainActivity.this, "Still waiting for the VM to start!", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else if (response.code() == 401) {
                            return false;
                        }
                        counter++;
                    }
                    if (counter >= 20) {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(MainActivity.this, "Time exceeded, we are sorry!", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    return false;
                } else {
                    return false;
                }
            } catch (Exception i) {
                i.printStackTrace();
                return false;
            }
        }


        protected void onPostExecute(Boolean success) {
            mGetAppTask = null;
            showProgress(false);
            mGoogleApiClient.connect();
            String port = mVmUrl.substring(mVmUrl.length() - 4);
            mVmUrl = mVmUrl.substring(0, mVmUrl.length() - 5);

            if (success) {
                Toast.makeText(MainActivity.this, "SUCCESS :" + mVmUrl, Toast.LENGTH_LONG).show();

                ConnectionBean selected = new ConnectionBean();
                selected.setAddress(mVmUrl);
                selected.setPassword("12345678");
                try
                {
                    selected.setPort(Integer.parseInt(port));
                }
                catch (NumberFormatException nfe)
                {
                    selected.setPort(5901);
                    Log.d("NumberFormatException", nfe.toString());
                }


                Intent intent = new Intent(MainActivity.this, VncCanvasActivity.class);
                intent.putExtra(VncConstants.CONNECTION, selected.Gen_getValues());

                try {
                    startActivity(intent);
                } catch (Exception i) {
                    i.printStackTrace();
                }
            } else {
                Toast.makeText(MainActivity.this, "FAILURE", Toast.LENGTH_LONG).show();
            }
        }

        @Override
        protected void onCancelled() {
            mGetAppTask = null;

        }
    }


    /**
     * Represents an asynchronous task used to get a list of applications from the server
     */
    public class ApplicationList extends AsyncTask<Void, Void, ArrayList<JSONObject>> {
        private String mLatitudeText;
        private String mLongitudeText;

        @Override
        protected ArrayList<JSONObject> doInBackground(Void... params) {

            // String lat = "12.124124";
            // String lng = "12.344345";

            mUsedLatitude = mLatitude;
            mUsedLongitude = mLongitude;

            mLatitudeText = String.valueOf(mLatitude);
            mLongitudeText = String.valueOf(mLongitude);
            String l = "?lat=" + mLatitudeText + "&lng=" + mLongitudeText;

            try {
                // Simulate network access.
                Response response = getList("https://mccg15.herokuapp.com/application" + l);
                int code = response.code();
                if (code == 200) {
                    mAppsListTest = response.body().string().toString();
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
                        return arrays;
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {

                    return arrays;
                }
            } catch (Exception i) {
                i.printStackTrace();
                return null;
            }
            return arrays;

        }


        protected void onPostExecute(ArrayList<JSONObject> list) {
            mAppList = null;
            arrays = list;

            populateAppList();

        }

        @Override
        protected void onCancelled() {
            mAppList = null;

        }
    }


    //Location services
    @Override
    protected void onStart() {
        super.onStart();
        showProgress(true);
        mResumeTest = mResumeCounter + 1;
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

            // No explanation needed, we can request the permission.
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            return;
        }

        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(1000); // Update location every second

        if (mLocationRequest != null) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION) {
            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                int grantResult = grantResults[i];

                if (permission.equals(android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        //Identifying location update parameters

                    } else {
                        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
                    }
                }
            }
        }
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
        mLatitude = location.getLatitude();
        mLongitude = location.getLongitude();
        mTextViewTest.setText(String.valueOf(mDistance));
        if (checkDistance() || mResumeTest != mResumeCounter) {
            mAppList = new ApplicationList();
            mAppList.execute((Void) null);
            mResumeCounter = mResumeTest;
            Toast.makeText(MainActivity.this, "Updating Location!", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean checkDistance() {
        double d2r = (3.14159265359 / 180);
        double dlat = (mLatitude - mUsedLatitude) * d2r;
        double dlong = (mLongitude - mUsedLongitude) * d2r;
        double a = pow(sin(dlat / 2.0), 2) + cos(mUsedLatitude * d2r) * cos(mLatitude * d2r) * pow(sin(dlong / 2.0), 2);
        double c = 2 * atan2(sqrt(a), sqrt(1 - a));
        double d = 6367000 * c;  //meters
        mDistance = d;

        if (d > 20) {
            return true;
        } else {
            return false;
        }
    }


    //Logout activity
    private void attemptLogout() {
        if (mAuthTask != null) {
            return;
        }


        boolean cancel = false;
        View focusView = null;

        showProgress(true);
        mAuthTask = new UserLogoutTask();
        mAuthTask.execute((Void) null);
    }

    public Response post(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", mToken)
                .build();
        return client.newCall(request).execute();
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mListView.setVisibility(show ? View.GONE : View.VISIBLE);
            mListView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mListView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mListView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLogoutTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {

            try {
                // Simulate network access.
                Response response = post("https://mccg15.herokuapp.com/users/logout");
                int code = response.code();
                if (code == 200) {
                    return true;
                } else {
                    return false;
                }
            } catch (Exception i) {
                i.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            showProgress(false);

            if (success) {
                Toast.makeText(MainActivity.this, "Logout successfully!", Toast.LENGTH_SHORT).show();
                Intent i = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(i);
            } else {
                Toast.makeText(MainActivity.this, "Server connection failed!", Toast.LENGTH_SHORT).show();

                //TESTING
                Intent i = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(i);
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }
}
