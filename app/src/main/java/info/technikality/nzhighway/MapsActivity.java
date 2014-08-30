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
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
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
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
                Elements labels = results[0];
                Elements values = results[1];

                // remove entry that does not have 3 points to trilaterate
                while ((i + 1) < entryList.size() && (labels.size() < 16
                        || labels.get(8).childNodeSize() != 0
                        || labels.get(9).childNodeSize() != 0
                        /*|| !value.get(12).text().equalsIgnoreCase("Active")*/)) {
                   results = dataQuery(i++);
                   labels = results[0];
                   values = results[1];
                }

                // check for last entry
                if (i >= entryList.size() - 1) {
                    // would skip the last entry
                    // TODO
                    return treis;
                }

                // TODO
                // have to fix this to reduce the need to store redundant data
                // proceed to the trilateration with this data immediately
                // need not store

                // 1 is LRMS code
                // 3 & 2 for title
                // 4 for What description
                // 0 for Where description
                // 6 for Until description
                // 5 for Detour description
                // 12 for active
                // 7,8,9 for the 3 points
                ArrayList<Object[]> store = new ArrayList<Object[]>();

                for (int j = 7; j <= 9; j++) {
                    Object[] objects = null;
                    while (objects == null) {
                        objects = retrieveCoordinates(values.get(j).text().split(" of "));
                    }
                    store.add(objects);
                }

                LatLng coord = trilaterate(store);

                MarkerOptions mo = new MarkerOptions().position(coord).title(values.get(3).text() + " - "
                        + values.get(2).text()).snippet("What: " + values.get(4).text() + "\n\n"
                        + "Where: " + values.get(0).text() + "\n");

                if (markerList == null) {
                    markerList = new ArrayList<MarkerOptions>();
                }
                markerList.add(mo);

                    /*for (int j = 0; j < label.size(); j++) {
                        if (j == 7 || j == 8 || j == 9) {
                            // Direct Line Distance Parameters
                            treisEntry.put(label.get(7).text().replace(":", " " + (j - 6) + ":"), value.get(j).text());
                        } else {
                            treisEntry.put(label.get(j).text(), value.get(j).text());
                        }
                    }*/
                //treis.add(treisEntry);
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

        private LatLng trilaterate (ArrayList<Object[]> store) {
            // refactored trilateration algorithm based on
            // http://gis.stackexchange.com/questions/66/trilateration-using-3-latitude-and-longitude-points-and-3-distances
            int earthR = 6371;
            double[] eX = new double[3];
            double[] eY = new double[3];
            double[] eZ = new double[3];

            double p1x = ((LatLng)store.get(0)[2]).latitude;
            double p2x = ((LatLng)store.get(1)[2]).latitude;
            double p3x = ((LatLng)store.get(2)[2]).latitude;

            double p1y = ((LatLng)store.get(0)[2]).longitude;
            double p2y = ((LatLng)store.get(1)[2]).longitude;
            double p3y = ((LatLng)store.get(2)[2]).longitude;

            double r1 = ( Double.parseDouble((String) store.get(0)[0]) + 0.50 );
            double r2 = ( Double.parseDouble((String) store.get(1)[0]) + 0.50 );
            double r3 = ( Double.parseDouble((String) store.get(2)[0]) + 0.50 );

            double xA = earthR *(Math.cos(Math.toRadians(p1x)) * Math.cos(Math.toRadians(p1y)));
            double yA = earthR *(Math.cos(Math.toRadians(p1x)) * Math.sin(Math.toRadians(p1y)));
            double zA = earthR *(Math.sin(Math.toRadians(p1x)));

            double xB = earthR *(Math.cos(Math.toRadians(p2x)) * Math.cos(Math.toRadians(p2y)));
            double yB = earthR *(Math.cos(Math.toRadians(p2x)) * Math.sin(Math.toRadians(p2y)));
            double zB = earthR *(Math.sin(Math.toRadians(p2x)));

            double xC = earthR *(Math.cos(Math.toRadians(p3x)) * Math.cos(Math.toRadians(p3y)));
            double yC = earthR *(Math.cos(Math.toRadians(p3x)) * Math.sin(Math.toRadians(p3y)));
            double zC = earthR *(Math.sin(Math.toRadians(p3x)));

            // A to B
            double deltaX = (xB - xA);
            double deltaY = (yB - yA);
            double deltaZ = (zB - zA);
            double abNorm = Math.pow(Math.pow(Math.abs(deltaX), 2) + Math.pow(Math.abs(deltaY), 2)
                    + Math.pow(Math.abs(deltaZ), 2),0.5);
            double d = abNorm;
            eX[0] = deltaX / abNorm;
            eX[1] = deltaY / abNorm;
            eX[2] = deltaZ / abNorm;

            // A to C
            deltaX = (xC - xA);
            deltaY = (yC - yA);
            deltaZ = (zC - zA);
            // dot product of eX with AC
            double i = (eX[0] * deltaX) + (eX[1] * deltaY) + (eX[2] * deltaZ);
            double acNorm = Math.pow(Math.pow(Math.abs(deltaX - (eX[0] * i)), 2)
                    + Math.pow(Math.abs(deltaY - (eX[1] * i)), 2)
                    + Math.pow(Math.abs(deltaZ - (eX[2] * i)), 2),0.5);
            eY[0] = (deltaX - (eX[0] * i)) / acNorm;
            eY[1] = (deltaY - (eX[1] * i)) / acNorm;
            eY[2] = (deltaZ - (eX[2] * i)) / acNorm;

            eZ[0] = (eX[1] * eY[2]) - (eX[2] * eY[1]);
            eZ[1] = (eX[2] * eY[0]) - (eX[0] * eY[2]);
            eZ[2] = (eX[0] * eY[1]) - (eX[1] * eY[0]);

            double j = (eY[0] * deltaX) + (eY[1] * deltaY) + (eY[2] * deltaZ);

            double x = (r1 * r1 - r2 * r2 + d * d) / (2 * d);
            double y = (((r1 * r1) - (r3 * r3) + (i * i) + (j * j)) / (2 * j)) - ((i / j) * x);
            double z = Double.parseDouble(sqrtImaginaryNum((r1 * r1) - (x * x) - (y * y)));

            //triPt is an array with ECEF x,y,z of trilateration point
            double[] triPt = new double[3];
            triPt[0] = xA + x*eX[0] + y*eY[0] + z*eZ[0];
            triPt[1] = yA + x*eX[1] + y*eY[1] + z*eZ[1];
            triPt[2] = zA + x*eX[2] + y*eY[2] + z*eZ[2];

            //convert back to lat/long from ECEF
            //convert to degrees
            double lat = Math.toDegrees(Math.asin(triPt[2] / earthR));
            double lon = Math.toDegrees(Math.atan2(triPt[1],triPt[0]));

            Location loc = new Location("");
            loc.setLatitude(lat);
            loc.setLongitude(lon);

            return new LatLng(loc.getLatitude(), loc.getLongitude());
        }

        private String sqrtImaginaryNum(double i) {
            String str = "";
            double sqrtI = Math.sqrt(Math.abs(i));
            if (i < 0) {

                return str += "-" + sqrtI;
            }
            return str += sqrtI;
        }
    }

/*    *//**
     * Implementation of AsyncTask used to process Marker
     *
     * This includes Geocoding the longitude and latitude of the 3 points
     * and trilateration of those 3 points to give us the exact position
     * to place the marker on the site.
     *//*
    private class ProcessMarkerTask extends AsyncTask<HashMap<String, String>, Void, MarkerOptions> {

        //private ProgressDialog dialog;
        private Context context;
        private MarkerOptions markerOptions;

        public ProcessMarkerTask(Context context) {
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
            *//*dialog = new ProgressDialog(context);
            dialog.setMessage("Processing Marker ");
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog.setProgress(0);
            dialog.setMax(100);
            dialog.show();*//*
        }

        @Override
        protected void onPostExecute(MarkerOptions markerOptions) {
            *//*if (dialog.isShowing()) {
                dialog.dismiss();
            }*//*

            int count = 0;
            while (this.markerOptions == null && count < 5) {
                try {
                    Thread.sleep(1000);
                    count++;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (this.markerOptions != null) {

                if (markerList == null) {
                    markerList = new ArrayList<MarkerOptions>();
                }

                markerList.add(this.markerOptions);
            }

        }

        @Override
        protected MarkerOptions doInBackground(HashMap<String, String>... hm) {
            ArrayList<Object[]> store = new ArrayList<Object[]>();
            for (int i = 1; i < 4; i++) {
                // object array contain
                // 1 - distance
                // 2 - compass direction
                // 3 - location (longitude,latitude)
                Object[] objects = new Object[3];
                try {
                    objects = retrieveCoordinates(hm[0].get("Direct Line Distances " + i + ":").split(" of "));
                    store.add(objects);
                } catch (NullPointerException e) {
                    Log.e("nzhighway","Direct Line Distance Error: " + (i - 1) + " number of parameter. ");
                    return null;
                }
            }


            if (store.size() >= 3 && store.get(0) != null && store.get(1) != null && store.get(2) != null) {
                // trilateration
                double[] eX = new double[2]; // unit vector in direction from P1 to P2
                double[] eY = new double[2]; // unit vector in direction from P1 to P2

                double p1x = ((Location) store.get(0)[2]).getLatitude();
                double p2x = ((Location) store.get(1)[2]).getLatitude();
                double p3x = ((Location) store.get(2)[2]).getLatitude();

                double p1y = ((Location) store.get(0)[2]).getLongitude();
                double p2y = ((Location) store.get(1)[2]).getLongitude();
                double p3y = ((Location) store.get(2)[2]).getLongitude();

                double r1 = Double.parseDouble((String) store.get(0)[0]) * 1000;
                double r2 = Double.parseDouble((String) store.get(1)[0]) * 1000;
                double r3 = Double.parseDouble((String) store.get(2)[0]) * 1000;

	    	*//* Calculating i, the signed magnitude of the x component from P1 to P3 *//*
                double deltaX = (p2x - p1x);
                double deltaY = (p2y - p1y);
                double norm = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
                double d = norm;

                eX[0] = deltaX / norm;
                eX[1] = deltaY / norm;

                deltaX = (p3x - p1x);
                deltaY = (p3y - p1y);
                double i = eX[0] * deltaX + eX[1] * deltaY;

    		*//* Calculating j, the signed magnitude of the y component from P1 to P3 *//*
                deltaX = (p3x - p1x - i * eX[0]);
                deltaY = (p3y - p1y - i * eX[1]);
                norm = Math.sqrt(deltaX * deltaX + deltaY * deltaY);

                eY[0] = deltaX / norm;
                eY[1] = deltaY / norm;

                deltaX = (p3x - p1x);
                deltaY = (p3y - p1y);
                double j = eY[0] * deltaX + eY[1] * deltaY;

		    *//* Calculate x *//*
                double x = (r1 * r1 - r2 * r2 + d * d) / (2 * d);
                double x2 = x;

		    *//* Calculate y *//*
                double y = (((r1 * r1) - (r3 * r3) + (i * i) + (j * j)) / (2 * j)) - ((i / j) * x);

                x = p1x + x * eX[0] + y * eY[0];
                y = p1y + x2 * eX[1] + y * eY[1];

                this.markerOptions = new MarkerOptions().position(new LatLng(x, y))
                        .title(hm[0].get("Event Type:")).snippet(hm[0].get("Location Area:"));

                return this.markerOptions;
            }
            return null;
        }

        private Object[] retrieveCoordinates(String[] coord) {
            // geocoding + address=santa+cruz&components=country:ES&key=
            try {
                URL url = new URL(geocoding + "address=" + coord[1] +
                        "&components=country:NZ&key=" +
                        "AIzaSyBH8z5LRQXBw3figE1D3XgnNEwR-52Ed-8");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(20000 *//* milliseconds *//*);
                conn.setConnectTimeout(30000 *//* milliseconds *//*);
                conn.setRequestMethod("GET");
                conn.setDoInput(true);
                // Starts the query
                conn.connect();
                JSONObject jObject = new JSONObject(getStringFromInputStream(conn.getInputStream()));
                if (jObject.getString("status").equalsIgnoreCase("ok")) {
                    jObject = jObject.getJSONArray("results").getJSONObject(0);
                    JSONObject geometry = jObject.getJSONObject("geometry");
                    JSONObject location = geometry.getJSONObject("location");
                    Double lng = location.getDouble("lng");
                    Double lat = location.getDouble("lat");
                    Location l = new Location("");
                    l.setLongitude(lng);
                    l.setLatitude(lat);
                    String[] vectors = coord[0].split(" km ");
                    return new Object[]{vectors[0], vectors[1], l};
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
    }*/

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

        /*while (treis.size() <= entryList.size() - 5) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // run the entryList from treis and process add the respective marker at the respective coordinates
        for (HashMap<String, String> hm : treis) {
            // google geocoding for coordinates
            // then trilaterate to place markers
            markerProcess = new ProcessMarkerTask(this).execute(hm);
        }*/
        //mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));
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
}
