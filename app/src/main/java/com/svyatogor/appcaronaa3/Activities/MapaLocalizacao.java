package com.svyatogor.appcaronaa3;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.MapController;

public class MapaLocalizacao extends AppCompatActivity {

    private Button btPublicarCarona;
    private Button btBuscarCarona;
    private MapView map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Configuration.getInstance().load(getApplicationContext(),
                androidx.preference.PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        Configuration.getInstance().setUserAgentValue(getPackageName());

        EdgeToEdge.enable(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        iniciarComponentes();


        map.setVisibility(View.GONE);


        btPublicarCarona.setOnClickListener(v -> {
        });

        btBuscarCarona.setOnClickListener(v -> {
            map.setVisibility(View.VISIBLE);

            map.setTileSource(TileSourceFactory.MAPNIK);
            map.setMultiTouchControls(true);
            MapController mapController = (MapController) map.getController();
            mapController.setZoom(15.0);
            GeoPoint startPoint = new GeoPoint(-23.5505, -46.6333); // São Paulo
            mapController.setCenter(startPoint);

        });
    }

    private void iniciarComponentes() {
        EditText etOrigem1 = findViewById(R.id.et_origem1);
        EditText etDestino1 = findViewById(R.id.et_destino1);
        EditText etData = findViewById(R.id.et_data);
        EditText etnVagas = findViewById(R.id.etn_vagas);
        EditText etOrigem2 = findViewById(R.id.et_origem2);
        EditText etDestino2 = findViewById(R.id.et_destino2);
        btPublicarCarona = findViewById(R.id.bt_publicar_carona);
        btBuscarCarona = findViewById(R.id.bt_buscar_carona);
        map = findViewById(R.id.map_view_dedicated);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (map != null) {
            map.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (map != null) {
            map.onPause();
        }
    }
}