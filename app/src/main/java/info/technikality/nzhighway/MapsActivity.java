package info.technikality.nzhighway;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import info.technikality.nzhighway.LRMS.LQJlrmsPoint;
import info.technikality.nzhighway.LRMS.LQJLrmsPortBinding;

import static info.technikality.nzhighway.XMLAdapter.Entry;

public class MapsActivity extends FragmentActivity {

    public static final String WIFI = "Wi-Fi";
    public static final String ANY = "Any";
    private static final String url = "https://infoconnect1.highwayinfo.govt.nz/ic/jbi/TREIS/RSS/AtomFeed.xml/";
    private static final String geocoding = "https://maps.googleapis.com/maps/api/geocode/json?";

    // Whether there is a Wi-Fi connection.
    private static boolean wifiConnected = false;
    // Whether there is a mobile connection.
    private static boolean mobileConnected = false;
    // Whether the display should be refreshed.
    public static String sPref = null;

    private static List<Entry> entryList = null;
    private static List<HashMap<String, String>> treis = null;
    private static List<MarkerOptions> markerList = null;

    /**
     * Implementation of AsyncTask used to get xml from NZTA
     *
     * This would query would return an xml with a entryList of location
     * that currently has incident along the highway of New Zealand.
     */
    private class DownloadXmlTask extends AsyncTask<String, Void, List> {

        private ProgressDialog dialog;
        private Context context;

        public DownloadXmlTask(Context context) {
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
            dialog = new ProgressDialog(context);
            dialog.setMessage("Loading NZTA Data");
            dialog.show();
        }

        @Override
        protected void onPostExecute(List list) {
            if (entryList == null)
                Log.e("MapsActivity.java", "entryList equals to null.");

            while (entryList == null) {
                try {
                    Thread.sleep(1000);
                    Log.e("MapsActivity.java", "entryList still equals to null.");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            secondStage();
        }


        @Override
        protected List doInBackground(String... urls) {
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(30000 /* milliseconds */);
                conn.setConnectTimeout(60000 /* milliseconds */);
                conn.setRequestMethod("GET");
                conn.setDoInput(true);
                // Starts the query
                conn.connect();
                entryList = new XMLAdapter().parse(conn.getInputStream());
                return entryList;
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (ProtocolException e) {
                e.printStackTrace();
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    /**
     * Implementation of AsyncTask used to process Entry entryList
     *
     * This would query the links from the entryList and extract the data
     * for each marker that we are about to place.
     */
    private class ProcessEntryTask extends AsyncTask<Void, Integer, List> {

        private ProgressDialog dialog;
        private Context context;

        public ProcessEntryTask(Context context) {
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
            dialog = new ProgressDialog(context);
            dialog.setMessage("Processing Entries ");
            dialog.setProgress(0);
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog.show();
        }

        @Override
        protected void onPostExecute(List list) {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            loadMarker();
        }


        @Override
        protected List<HashMap<String, String>> doInBackground(Void... entry) {
            dialog.setMax(entryList.size());
            treis = new ArrayList<HashMap<String, String>>();
            for (int i = 0; i < entryList.size(); i++) {
                publishProgress((int) (i + 1));

                Elements[] results = dataQuery(i);
                Elements values = results[1];

                if (values.size() <= 3)
                    continue;

                // 1 is LRMS code
                // 3 & 2 for title
                // 4 for What description
                // 0 for Where description
                // 6 for Until description
                // 5 for Detour description
                // 12 for active
                // 7,8,9 for the 3 points

                // SOAP request on LRMS
                // LRMS code with a 1 or more RS
                // 0 = SH code
                // 1 = RS code
                // 2 = distance in KM
                // 3 = direction
                String[] strip = values.get(1).text().split("-| |/");

                LatLng coord = locationQuery(strip);

                // check for null coord
                // if null skip and move on
                while(coord == null) {
                    results = dataQuery(i++);
                    values = results[1];
                    strip = values.get(1).text().split("-| |/");
                    coord = locationQuery(strip);
                }

                MarkerOptions mo = new MarkerOptions().position(coord).title(values.get(3).text() + " - "
                        + values.get(2).text()).snippet("What: " + values.get(4).text() + "\n\n"
                        + "Where: " + values.get(0).text() + "\n");

                if (markerList == null) {
                    markerList = new ArrayList<MarkerOptions>();
                }
                markerList.add(mo);

            }
            return treis;
        }

        private Elements[] dataQuery(int i) {
            try {
                Document doc = Jsoup.connect(entryList.get(i).link).get();
                Element valid = doc.getElementById("content");

                Elements label = valid.getElementsByClass("label");
                Elements value = valid.getElementsByClass("value");
                return new Elements[]{label, value};
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }
        
        private LatLng locationQuery(String[] values) {
            try {
                LQJlrmsPoint lm = new LQJLrmsPortBinding().locatePointAlongRs(values[0],
                        Integer.parseInt(values[1]), Double.parseDouble(values[2]) * 1000,
                        (values[3] == null || values[3].length() != 1)? "B":values[3], 4326);
                // lm y = latitude
                // lm x = longitude
                return new LatLng(lm.y,lm.x);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... args) {
            super.onProgressUpdate();
            dialog.setProgress(args[0]);
        }

        private Object[] retrieveCoordinates(String[] coord) {
            // geocoding + address=santa+cruz&components=country:ES&key=
            try {
                URL url = new URL(geocoding + "address=" + URLEncoder.encode(coord[1],"UTF-8") +
                        "&components=&country:NZ&key=" +
                        "AIzaSyBH8z5LRQXBw3figE1D3XgnNEwR-52Ed-8");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(50000 /* milliseconds */);
                conn.setConnectTimeout(100000 /* milliseconds */);
                conn.setRequestMethod("GET");
                //conn.setRequestProperty("User-Agent","Mozilla/5.0 (Linux; Android) ");
                //conn.setRequestProperty("Accept","*/*");
                //conn.setDoInput(true);
                // Starts the query
                conn.connect();
                InputStream is = conn.getInputStream();
                JSONObject jObject = new JSONObject(getStringFromInputStream(is));
                if (jObject.getString("status").equalsIgnoreCase("ok")) {
                    jObject = jObject.getJSONArray("results").getJSONObject(0);
                    JSONObject geometry = jObject.getJSONObject("geometry");
                    JSONObject location = geometry.getJSONObject("location");
                    Double lng = location.getDouble("lng");
                    Double lat = location.getDouble("lat");
                    LatLng loc = new LatLng(lat,lng);
                    String[] vectors = coord[0].split(" km ");
                    return new Object[]{vectors[0], vectors[1], loc};
                } else {
                    // Error
                    return null;
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (ProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        sPref = ANY;
        NetworkInfo[] allNetworkInfo = ((ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE)).getAllNetworkInfo();
        for (NetworkInfo ni : allNetworkInfo) {
            // TYPE_MOBILE, TYPE_WIFI
            if (ni.getTypeName().equalsIgnoreCase("mobile")) {
                mobileConnected = ni.isConnected();
            } else if (ni.getTypeName().equalsIgnoreCase("wifi")) {
                wifiConnected = ni.isConnected();
            }
        }

        loadPage();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPage();
    }

    // Uses AsyncTask to download the XML feed.
    public void loadPage() {

        if ((sPref.equals(ANY)) && (wifiConnected || mobileConnected)) {
            new DownloadXmlTask(this).execute(url);
        } else if ((sPref.equals(WIFI)) && (wifiConnected)) {
            new DownloadXmlTask(this).execute(url);
        } else {
            // show error
            errorMessage("There is no connection available!");
        }
    }

    private void secondStage() {
        new ProcessEntryTask(this).execute();
    }

    private void loadMarker() {
        setUpMapIfNeeded();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        //TODO
        mMap.setMyLocationEnabled(true);
        mMap.setTrafficEnabled(true);
        mMap.setIndoorEnabled(false);
        mMap.setBuildingsEnabled(false);
        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                TextView tv = new TextView(getBaseContext());
                tv.setMinLines(5);
                tv.setText(marker.getTitle() + "\n\n" + marker.getSnippet());
                return tv;
            }
        });
        LatLng welly = new LatLng(-41.2443701,174.7618546);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(welly,9));

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                Toast.makeText(getBaseContext(), marker.getTitle(), Toast.LENGTH_LONG).show();
                return false;
            }
        });

        for (MarkerOptions marker: markerList) {
            mMap.addMarker(marker);
        }
    }

    /**
     * Alert message dialog that will pop up on the screen.
     *
     * @param errorMessage
     */
    private void errorMessage(String errorMessage) {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
        alertBuilder.setTitle("Error");
        alertBuilder.setMessage(errorMessage);
        alertBuilder.setNeutralButton("Exit the app", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                MapsActivity.this.finish();
            }
        });
        AlertDialog alertDialog = alertBuilder.create();
        alertDialog.show();
    }

    // convert InputStream to String
    private static String getStringFromInputStream(InputStream is) {

        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();

        String line;
        try {

            br = new BufferedReader(new InputStreamReader(is));
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return sb.toString();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add("Refresh Map").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                loadPage();
                return false;
            }
        });
        menu.add("Navigate");
        return super.onPrepareOptionsMenu(menu);
    }
}
