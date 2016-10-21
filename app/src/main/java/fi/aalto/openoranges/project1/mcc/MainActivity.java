package fi.aalto.openoranges.project1.mcc;

import android.Manifest;
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
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.view.SimpleDraweeView;
import com.google.android.gms.appindexing.AppIndex;
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
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    //Initialization for location service
    private int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private View mNoLocationPermission;
    private double mLatitude;
    private double mLongitude;

    //Initialization for Application List
    private static final String LOG_TAG = "MainActivity";
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    OkHttpClient client = new OkHttpClient();
    private ApplicationList mAppList = null;
    private getApplicationTask mGetAppTask = null;
    private TextView mTextView;
    private String mAppsListTest = "void";
    private ArrayList<JSONObject> arrays = null;
    private List<Application> myApps = new ArrayList<>();

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLogoutTask mAuthTask = null;
    private View mProgressView;
    private View mListView;
    private int mResumeTest = 0;
    private Button mLogoutButton;
    private ImageButton mRefreshButton;
    private TimeoutOperation mSleeper = null;

    private String mToken;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //initialize fresco for downloading icon images
        Fresco.initialize(this);

        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mListView = findViewById(R.id.oo_AppsListView);
        mProgressView = findViewById(R.id.logout_progress);
        mNoLocationPermission = findViewById(R.id.noLocationPermission);

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            // ATTENTION: This "addApi(AppIndex.API)"was auto-generated to implement the App Indexing API.
            // See https://g.co/AppIndexing/AndroidStudio for more information.
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .addApi(AppIndex.API).build();
        }

        mToken = getIntent().getStringExtra("token");

        mTextView = (TextView) findViewById(R.id.textView);

        mRefreshButton = (ImageButton) findViewById(R.id.refresh);
        mRefreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSleeper = new TimeoutOperation();
                mSleeper.execute((Void) null);
                showProgress(true);
                mAppList = new ApplicationList();
                mAppList.execute((Void) null);
            }
        });

        mLogoutButton = (Button) findViewById(R.id.logout);
        mLogoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogout();
            }
        });

        registerClickCallback();
    }

    @Override
    protected void onStart() {
        super.onStart();
        showProgress(true);
        mResumeTest = 1;
        mLogoutButton.setEnabled(true);
        mRefreshButton.setEnabled(true);
        //Connect the client
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        //Disconnect the client
        mGoogleApiClient.disconnect();
        super.onStop();
    }


    //adding the applications in the list to the ListView
    private void addingAppsToListView() {
        ArrayAdapter<Application> adapter = new ListAdapter();
        ListView list = (ListView) findViewById(R.id.oo_AppsListView);
        list.setAdapter(adapter);
    }

    /**
     * class to add applications to the list
     */
    private class ListAdapter extends ArrayAdapter<Application> {
        public ListAdapter() {
            super(MainActivity.this, R.layout.item_view, myApps);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            //Ensure to have a view which is not null
            View itemView = convertView;
            if (itemView == null)
                itemView = getLayoutInflater().inflate(R.layout.item_view, parent, false);
            //finding app in the list on position
            Application currentApp = myApps.get(position);

            //Fill the view with the apps using inflater
            Uri uri = Uri.parse(currentApp.getIcon_url());
            SimpleDraweeView view = (SimpleDraweeView) itemView.findViewById(R.id.imageView);
            try {
                view.setImageURI(uri);
            } catch (Exception i) {
                i.printStackTrace();
            }


            //Fill the textview with the name of the app
            TextView nameText = (TextView) itemView.findViewById(R.id.nameText);
            nameText.setText(currentApp.getName());

            return itemView;
        }
    }

    //fill arraylist with the apps, received from the server
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
        addingAppsToListView();
    }

    /**
     * Represents an asynchronous task used to get a list of applications from the server
     */
    public class ApplicationList extends AsyncTask<Void, Void, ArrayList<JSONObject>> {
        private String mLatitudeText;
        private String mLongitudeText;

        @Override
        protected ArrayList<JSONObject> doInBackground(Void... params) {

            mLatitudeText = String.valueOf(mLatitude);
            mLongitudeText = String.valueOf(mLongitude);
            String l = "?lat=" + mLatitudeText + "&lng=" + mLongitudeText;

            try {
                String server_url = getString(R.string.server);
                Response response = getList(server_url + "application" + l);
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
                        JSONObject[] json = new JSONObject[arrays.size()];
                        arrays.toArray(json);
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
            //checks if received list is empty or not
            if (list == null) {
                showProgress(false);
                Toast.makeText(MainActivity.this, "List could not be refreshed due to missing internet connection!", Toast.LENGTH_LONG).show();
            } else {
                mAppList = null;
                arrays = list;
                populateAppList();
            }
        }

        @Override
        protected void onCancelled() {
            mAppList = null;

        }
    }


    //Method for starting application and register which app is clicked
    private void registerClickCallback() {
        ListView list = (ListView) findViewById(R.id.oo_AppsListView);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View viewClicked, int position, long id) {
                Application clickedApp = myApps.get(position);
                mGoogleApiClient.disconnect();
                showProgress(true);
                mGetAppTask = new getApplicationTask(clickedApp.getId(), clickedApp.getName());
                mGetAppTask.execute((Void) null);
            }
        });
    }

    public Response getList(String url) throws IOException {
        return get(url);
    }

    //Sends a json request to the server and returns the response
    //receives as parameters a string which represents the url of the server
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
        private final String mName;
        private String mVmUrl;
        private boolean isInternetAvailable = true;

        public getApplicationTask(String id, String name) {
            mId = id;
            mName = name;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            int counter = 0;
            try {
                String server_url = getString(R.string.server);
                Response response = MainActivity.this.get(server_url + "application/" + mId);
                int code = response.code();

                //if the code is 200 than everything is okay and vnc viewer can start
                //if the code is 202 wait for the VM to get started
                if (code == 200) {
                    JSONObject myjson = new JSONObject(response.body().string().toString());
                    mVmUrl = myjson.getString("vm_url");
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(MainActivity.this, "Launching " + mName + " wait for the VM to start!", Toast.LENGTH_LONG).show();
                        }
                    });
                    return true;
                } else if (code == 202) {
                    while (counter < 20) {
                        Thread.sleep(8000);
                        response = MainActivity.this.get(server_url + "application/" + mId);
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
                    //
                    if (counter >= 20) {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(MainActivity.this, "Time exceeded, we are sorry!", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    return false;
                } else if (code == 500) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(MainActivity.this, "VM of " + mName + " still disconnecting. Please wait!", Toast.LENGTH_LONG).show();
                        }
                    });
                    return false;
                } else {
                    return false;
                }
            } catch (Exception i) {
                isInternetAvailable = false;
                i.printStackTrace();
                return false;
            }
        }


        protected void onPostExecute(Boolean success) {
            mGetAppTask = null;
            showProgress(false);

            if (success) {
                mGoogleApiClient.connect();
                String port = mVmUrl.substring(mVmUrl.length() - 4);
                mVmUrl = mVmUrl.substring(0, mVmUrl.length() - 5);

                ConnectionBean selected = new ConnectionBean();
                selected.setAddress(mVmUrl);
                selected.setPassword("12345678");
                selected.setForceFull(1L);

                try {
                    selected.setPort(Integer.parseInt(port));
                } catch (NumberFormatException nfe) {
                    selected.setPort(5901);
                    Log.d("NumberFormatException", nfe.toString());
                }

                Intent intent = new Intent(MainActivity.this, VncCanvasActivity.class);
                intent.putExtra(VncConstants.CONNECTION, selected.Gen_getValues());
                intent.putExtra("token", mToken);
                intent.putExtra("id", mId);
                intent.putExtra("name", mName);


                try {
                    startActivity(intent);
                } catch (Exception i) {
                    i.printStackTrace();
                }
                finish();
            } else {
                if (isInternetAvailable == false) {
                    Toast.makeText(MainActivity.this, "Application can not be opened due to missing internet connection!", Toast.LENGTH_LONG).show();
                    mLogoutButton.setEnabled(true);
                    mRefreshButton.setEnabled(true);
                } else {
                }
                //Toast.makeText(MainActivity.this, "Failure", Toast.LENGTH_LONG).show();
            }
        }

        @Override
        protected void onCancelled() {
            mGetAppTask = null;
            showProgress(false);
        }
    }


    //Location services
    @Override
    public void onConnected(@Nullable Bundle bundle) {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // No explanation needed, we can request the permission.
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            return;
        }

        mNoLocationPermission.setVisibility(View.INVISIBLE);

        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(1000); // Update location every second

        if (mLocationRequest != null) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
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
        if (mResumeTest != 0) {
            mAppList = new ApplicationList();
            mAppList.execute((Void) null);
            mResumeTest = 0;
        }
    }

    //Permission handling
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION) {
            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                int grantResult = grantResults[i];

                if (permission.equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        //Identifying location update parameters
                        mGoogleApiClient.disconnect();
                        this.onStart();

                        //mAppList = new ApplicationList();
                        //mAppList.execute((Void) null);
                    } else {
                        //default location
                        mLatitude = 0.0;
                        mLongitude = 0.0;
                        mAppList = new ApplicationList();
                        mAppList.execute((Void) null);
                        mNoLocationPermission.setVisibility(View.VISIBLE);
                        //ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
                    }
                }
            }
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
        RequestBody body = RequestBody.create(JSON, "");
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", mToken)
                .post(body)
                .build();
        return client.newCall(request).execute();
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLogoutTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {

            try {
                String server_url = getString(R.string.server);
                Response response = post(server_url + "users/logout");
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
                finish();
            } else {
                Toast.makeText(MainActivity.this, "Server connection failed!", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }


    /**
     * Shows the progress bar
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

    //Sleeper for refreshing function
    private class TimeoutOperation extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {

            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            //Update your layout here
        }
    }

}
