package fi.aalto.openoranges.project1.mcc;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Toast;

import java.io.IOException;

import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.internal.framed.Header;

public class MainActivity extends AppCompatActivity {

    private String mToken;
    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");
    OkHttpClient client = new OkHttpClient();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });


        mToken = getIntent().getStringExtra("token");
        Toast.makeText(MainActivity.this, mToken, Toast.LENGTH_SHORT).show();


            String l = "?lat=12.124124&lng=12.344345";
        try {
            // Simulate network access.
            Response response = get("https://mccg15.herokuapp.com/application"+l);
            int code = response.code();
            if (code == 200) {
                Toast.makeText(MainActivity.this, "success", Toast.LENGTH_LONG).show();
            } else if (code == 401) {
                Toast.makeText(MainActivity.this, "Code: 401", Toast.LENGTH_LONG).show();
            }
        } catch (Exception i) {
            Toast.makeText(MainActivity.this, "FAILURE", Toast.LENGTH_LONG).show();
        }

    }


    Response get(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "\""+mToken+"\"")
                .build();
        Response response = client.newCall(request).execute();
        return response;
    }


}
