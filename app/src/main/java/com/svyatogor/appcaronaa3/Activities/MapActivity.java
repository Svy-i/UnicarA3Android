package com.svyatogor.appcaronaa3.Activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.svyatogor.appcaronaa3.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MapActivity extends AppCompatActivity {

    private MapView map;
    private MyLocationNewOverlay mLocationOverlay;
    private final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private String origem, destino;
    private final String API_KEY = "5b3ce3597851110001cf624859742347a61f4c38a2cf371021231169";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_map);

        map = findViewById(R.id.map_view_dedicated);
        ImageButton ibCloseMap = findViewById(R.id.ib_close_map);

        Intent intent = getIntent();
        origem = intent.getStringExtra("origem");
        destino = intent.getStringExtra("destino");

        requestPermissionsIfNecessary(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        });

        setupMap();

        if (ibCloseMap != null) {
            ibCloseMap.setOnClickListener(v -> finish());
        }

        if (origem != null && destino != null) {
            buscarCoordenadas(origem, destino);
        }
    }

    private void setupMap() {
        if (map != null) {
            map.setTileSource(TileSourceFactory.MAPNIK);
            map.setMultiTouchControls(true);
            IMapController mapController = map.getController();
            mapController.setZoom(14.5);
            mapController.setCenter(new GeoPoint(-19.4600, -44.2486));

            mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), map);
            mLocationOverlay.enableMyLocation();
            map.getOverlays().add(mLocationOverlay);
        }
    }

    private void buscarCoordenadas(String origem, String destino) {
        new Thread(() -> {
            try {
                GeoPoint origemCoords = geocodificar(origem);
                GeoPoint destinoCoords = geocodificar(destino);

                if (origemCoords != null && destinoCoords != null) {
                    desenharRota(origemCoords, destinoCoords);
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(this, "Erro ao obter coordenadas", Toast.LENGTH_SHORT).show()
                    );
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private GeoPoint geocodificar(String endereco) throws IOException {
        OkHttpClient client = new OkHttpClient();
        String url = "https://api.openrouteservice.org/geocode/search?api_key=" + API_KEY + "&text=" + endereco;

        Log.d("GEOCODIFICAR_URL", url);

        Request request = new Request.Builder().url(url).build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                Log.e("GEOCODIFICAR_ERRO", "Erro na resposta: " + response.code());
                return null;
            }
            String responseBody = response.body().string();
            Log.d("GEOCODIFICAR_RESPOSTA", responseBody);

            JSONObject json = new JSONObject(responseBody);
            JSONArray features = json.getJSONArray("features");
            if (features.length() > 0) {
                JSONArray coords = features.getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates");
                double lon = coords.getDouble(0);
                double lat = coords.getDouble(1);
                return new GeoPoint(lat, lon);
            } else {
                Log.w("GEOCODIFICAR", "Nenhuma coordenada encontrada.");
            }
        } catch (JSONException e) {
            Log.e("GEOCODIFICAR_JSON", "Erro no JSON: " + e.getMessage());
            throw new RuntimeException(e);
        }
        return null;
    }




    private void desenharRota(GeoPoint origem, GeoPoint destino) throws IOException, JSONException {
        OkHttpClient client = new OkHttpClient();

        String url = "https://api.openrouteservice.org/v2/directions/driving-car/geojson";


        JSONObject jsonBody = new JSONObject();
        JSONArray coords = new JSONArray();
        JSONArray origemJson = new JSONArray();
        origemJson.put(origem.getLongitude());
        origemJson.put(origem.getLatitude());
        JSONArray destinoJson = new JSONArray();
        destinoJson.put(destino.getLongitude());
        destinoJson.put(destino.getLatitude());
        coords.put(origemJson);
        coords.put(destinoJson);
        jsonBody.put("coordinates", coords);

        RequestBody body = RequestBody.create(
                jsonBody.toString(),
                MediaType.parse("application/json")
        );


        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", API_KEY) // Lembre-se que API_KEY deve ser sua chave
                .header("Content-Type", "application/json")
                .post(body)
                .build();


        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {

                Log.e("ROTA_HTTP_ERRO", "Código de erro: " + response.code());
                return;
            }

            String responseData = response.body().string();
            JSONObject json = new JSONObject(responseData);


            Log.d("ROTA_JSON", json.toString(4)); // Usar json.toString(4) para formatar o JSON no log, fica mais fácil de ler


            if (json.has("error")) {
                String errorMessage = json.getJSONObject("error").getString("message");
                Log.e("ROTA_API_ERRO", "A API retornou um erro: " + errorMessage);


                return;
            }




            JSONArray routeCoords = json.getJSONArray("features")
                    .getJSONObject(0)
                    .getJSONObject("geometry")
                    .getJSONArray("coordinates");

            List<GeoPoint> geoPoints = new ArrayList<>();
            for (int i = 0; i < routeCoords.length(); i++) {
                JSONArray coord = routeCoords.getJSONArray(i);
                double lon = coord.getDouble(0);
                double lat = coord.getDouble(1);
                geoPoints.add(new GeoPoint(lat, lon));
            }

            Log.d("ROTA_PONTOS", "Total de pontos: " + geoPoints.size());


            runOnUiThread(() -> {


                Polyline routeLine = new Polyline();
                routeLine.setPoints(geoPoints);
                routeLine.setColor(Color.BLUE);
                routeLine.setWidth(10f); // Aumentei um pouco a espessura para visualização


                map.getOverlays().removeIf(overlay -> overlay instanceof Polyline);
                map.getOverlays().add(routeLine);


                BoundingBox boundingBox = BoundingBox.fromGeoPoints(geoPoints);
                map.zoomToBoundingBox(boundingBox, true, 100); // 100 é a margem

                map.invalidate();
            });
        }
    }

    private void requestPermissionsIfNecessary(String[] permissions) {
        List<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (map != null) map.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (map != null) map.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }
}