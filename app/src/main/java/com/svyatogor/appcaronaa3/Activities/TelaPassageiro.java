package com.svyatogor.appcaronaa3.Activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler; // Importar Handler
import android.os.Looper; // Importar Looper
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.svyatogor.appcaronaa3.Model.Usuario;
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
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TelaPassageiro extends AppCompatActivity implements LocationListener {

    private MapView map;
    private MyLocationNewOverlay mLocationOverlay;
    private final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private EditText etOrigem, etDestino;
    private Button btnSolicitarCarona;
    private ListView lvMotoristas;
    private TextView tvMotoristasDisponiveis;

    private LocationManager locationManager;
    private GeoPoint currentLocation;
    private ImageView icUserPassageiro;
    private DatabaseReference databaseReference;
    private FirebaseAuth auth;
    private List<Usuario> motoristasDisponiveis;
    private ArrayAdapter<Usuario> motoristasAdapter;
    private final String API_KEY = "5b3ce3597851110001cf624859742347a61f4c38a2cf371021231169";

    // --- Variáveis para o Debounce da Localização GPS (etOrigem preenchimento automático) ---
    private Handler locationHandler = new Handler(Looper.getMainLooper());
    private Runnable reverseGeocodeRunnable;
    private static final long REVERSE_GEOCODE_DELAY_MS = 4000; // 4 segundos de atraso
    private boolean isInitialLocationSet = false; // Flag para garantir a primeira atualização rápida

    // --- Variáveis para o Debounce dos EditText (origem/destino digitados) ---
    private Handler etHandler = new Handler(Looper.getMainLooper());
    private Runnable etOrigemDebounceRunnable, etDestinoDebounceRunnable;
    private static final long ET_DEBOUNCE_DELAY_MS = 1000; // 1 segundo de atraso para os EditText

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_tela_passageiro);

        // Inicialização dos componentes da UI
        iniciarComponentes();

        // Permissões
        requestPermissionsIfNecessary(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        });

        // Configuração do mapa
        setupMap();

        // Inicialização do Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference();
        auth = FirebaseAuth.getInstance();

        // Configuração da lista de motoristas
        motoristasDisponiveis = new ArrayList<>();
        motoristasAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, motoristasDisponiveis);
        lvMotoristas.setAdapter(motoristasAdapter);

        // Obter localização atual e configurar listeners
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Ajustando a frequência das atualizações do GPS (5s e 10m)
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, this);
            // Tenta obter a última localização conhecida para preencher o campo de origem
            Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastKnownLocation != null) {
                currentLocation = new GeoPoint(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                reverseGeocode(currentLocation, etOrigem); // Preenche a origem rapidamente no início
                isInitialLocationSet = true;
            }
        } else {
            Toast.makeText(this, "Permissão de localização não concedida. Por favor, habilite nas configurações.", Toast.LENGTH_LONG).show();
            // Desabilita o campo de origem ou mostra um placeholder diferente se a localização não for concedida
            etOrigem.setHint("Permissão de localização necessária");
            etOrigem.setEnabled(false); // Desabilita edição se não houver GPS
        }

        // Listener para o botão de solicitar carona (executa o metodo)
        btnSolicitarCarona.setOnClickListener(v -> solicitarCarona());

        // Listener para o icone de perfil para configurar perfil de usuario
        icUserPassageiro.setOnClickListener(v -> {
            startActivity(new Intent(TelaPassageiro.this, PerfilUser.class));
        });

        // Listeners com Debounce para os EditText de Origem e Destino
        etOrigem.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) { // Quando o foco sai do EditText
                if (etOrigemDebounceRunnable != null) {
                    etHandler.removeCallbacks(etOrigemDebounceRunnable);
                }
                etOrigemDebounceRunnable = () -> {
                    String origem = etOrigem.getText().toString().trim();
                    if (!origem.isEmpty()) {
                        buscarCoordenadasEPonto(origem, "origem");
                    }
                };
                etHandler.postDelayed(etOrigemDebounceRunnable, ET_DEBOUNCE_DELAY_MS);
            } else { // Quando o foco entra no EditText
                if (etOrigemDebounceRunnable != null) {
                    etHandler.removeCallbacks(etOrigemDebounceRunnable);
                }
            }
        });

        etDestino.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) { // Quando o foco sai do EditText
                if (etDestinoDebounceRunnable != null) {
                    etHandler.removeCallbacks(etDestinoDebounceRunnable);
                }
                etDestinoDebounceRunnable = () -> {
                    String destino = etDestino.getText().toString().trim();
                    if (!destino.isEmpty()) {
                        buscarCoordenadasEPonto(destino, "destino");
                    }
                };
                etHandler.postDelayed(etDestinoDebounceRunnable, ET_DEBOUNCE_DELAY_MS);
            } else { // Quando o foco entra no EditText
                if (etDestinoDebounceRunnable != null) {
                    etHandler.removeCallbacks(etDestinoDebounceRunnable);
                }
            }
        });

        // Buscar motoristas disponíveis
        buscarMotoristasDisponiveis();

    } // fim do onCreate

    private void iniciarComponentes(){
        map = findViewById(R.id.map_view_passageiro);
        etOrigem = findViewById(R.id.et_origem);
        etDestino = findViewById(R.id.et_destino);
        btnSolicitarCarona = findViewById(R.id.btn_solicitar_carona);
        lvMotoristas = findViewById(R.id.lv_motoristas);
        tvMotoristasDisponiveis = findViewById(R.id.tv_motoristas_disponiveis);
        icUserPassageiro = findViewById(R.id.ic_user_passageiro);
    }

    private void solicitarCarona() {
        String origem = etOrigem.getText().toString().trim();
        String destino = etDestino.getText().toString().trim();

        if (origem.isEmpty() || destino.isEmpty()) {
            Toast.makeText(this, "Por favor, informe a origem e o destino.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Salvar a solicitação de carona no Firebase para o usuário atual
        String userId = auth.getCurrentUser().getUid();
        DatabaseReference usuarioRef = databaseReference.child("usuarios").child(userId);
        usuarioRef.child("pontoDeChegada").setValue(origem);
        usuarioRef.child("destino").setValue(destino);
        usuarioRef.child("isLookingForRide").setValue(true)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(TelaPassageiro.this, "Carona solicitada! Aguarde um motorista.", Toast.LENGTH_LONG).show();
                    // Aqui você pode adicionar lógica para esperar por uma resposta de motoristas
                    // e talvez mostrar uma animação de "buscando motoristas"
                })
                .addOnFailureListener(e -> Toast.makeText(TelaPassageiro.this, "Erro ao solicitar carona: " + e.getMessage(), Toast.LENGTH_SHORT).show());

        // Desenhar a rota no mapa
        buscarCoordenadas(origem, destino);
    }

    public void setupMap() {
        if (map != null) {
            map.setTileSource(TileSourceFactory.MAPNIK);
            map.setMultiTouchControls(true);
            IMapController mapController = map.getController();
            mapController.setZoom(14.5);
            // Centraliza em uma localização padrão (ex: Sete Lagoas, MG)
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
                            Toast.makeText(this, "Erro ao obter coordenadas para a rota. Verifique os endereços.", Toast.LENGTH_SHORT).show()
                    );
                }
            } catch (Exception e) {
                Log.e("ROTA_ERRO_GERAL", "Erro ao buscar rota: " + e.getMessage());
                runOnUiThread(() ->
                        Toast.makeText(this, "Erro ao buscar rota: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    private void buscarCoordenadasEPonto(String endereco, String tipo) {
        new Thread(() -> {
            try {
                GeoPoint coords = geocodificar(endereco);
                if (coords != null) {
                    runOnUiThread(() -> {
                        adicionarMarcador(coords, tipo.equals("origem") ? "Origem" : "Destino", tipo.equals("origem") ? Color.GREEN : Color.RED);
                        map.getController().animateTo(coords);
                    });
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(this, "Não foi possível encontrar o endereço: " + endereco, Toast.LENGTH_SHORT).show()
                    );
                }
            } catch (IOException e) {
                Log.e("GEOCODIFICAR_ERRO", "Erro de IO ao buscar coordenadas: " + e.getMessage());
                runOnUiThread(() ->
                        Toast.makeText(this, "Erro de comunicação ao buscar endereço.", Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    private void adicionarMarcador(GeoPoint p, String titulo, int cor) {
        runOnUiThread(() -> {
            // Remove marcadores existentes de origem/destino para evitar sobreposição
            // Remove marcadores existentes com o mesmo título para evitar duplicidade
            map.getOverlays().removeIf(overlay -> overlay instanceof Marker && ((Marker) overlay).getTitle() != null && ((Marker) overlay).getTitle().equals(titulo));

            Marker marker = new Marker(map);
            marker.setPosition(p);
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marker.setTitle(titulo);
            // Defina um ícone personalizado se desejar, por exemplo:
            // marker.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_marker_origem));
            map.getOverlays().add(marker);
            map.invalidate(); // Redesenha o mapa
        });
    }

    private GeoPoint geocodificar(String endereco) throws IOException {
        OkHttpClient client = new OkHttpClient();
        // Codifica o endereço para URL para lidar com espaços e caracteres especiais
        String encodedAddress = java.net.URLEncoder.encode(endereco, "UTF-8");
        String url = "https://api.openrouteservice.org/geocode/search?api_key=" + API_KEY + "&text=" + encodedAddress;

        Log.d("GEOCODIFICAR_URL", url);

        Request request = new Request.Builder().url(url).build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                Log.e("GEOCODIFICAR_ERRO", "Erro na resposta: " + response.code() + " - " + response.message());
                // Lidar com o erro 429 especificamente
                if (response.code() == 429) {
                    runOnUiThread(() -> Toast.makeText(this, "Limite de requisições excedido. Tente novamente mais tarde.", Toast.LENGTH_LONG).show());
                }
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
                Log.w("GEOCODIFICAR", "Nenhuma coordenada encontrada para: " + endereco);
            }
        } catch (JSONException e) {
            Log.e("GEOCODIFICAR_JSON", "Erro no JSON: " + e.getMessage());
            throw new RuntimeException(e);
        }
        return null;
    }

    private void reverseGeocode(GeoPoint geoPoint, EditText editText) {
        new Thread(() -> {
            OkHttpClient client = new OkHttpClient();
            String url = String.format(Locale.US, "https://api.openrouteservice.org/geocode/reverse?api_key=%s&point.lon=%f&point.lat=%f",
                    API_KEY, geoPoint.getLongitude(), geoPoint.getLatitude());

            Request request = new Request.Builder().url(url).build();
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    Log.e("REVERSE_GEOCODIFICAR_ERRO", "Erro na resposta: " + response.code() + " - " + response.message());
                    if (response.code() == 429) {
                        runOnUiThread(() -> Toast.makeText(this, "Limite de requisições excedido. Tente novamente mais tarde.", Toast.LENGTH_LONG).show());
                    }
                    return;
                }
                String responseBody = response.body().string();
                JSONObject json = new JSONObject(responseBody);
                JSONArray features = json.getJSONArray("features");
                if (features.length() > 0) {
                    JSONObject properties = features.getJSONObject(0).getJSONObject("properties");
                    String address = properties.optString("label", "Endereço desconhecido");
                    runOnUiThread(() -> {
                        // Só atualiza o EditText se o usuário não estiver focado nele ou se ele estiver vazio
                        if (!editText.isFocused() || editText.getText().toString().isEmpty()) {
                            editText.setText(address);
                        }
                    });
                }
            } catch (IOException | JSONException e) {
                Log.e("REVERSE_GEOCODIFICAR_EXC", "Erro no reverseGeocode: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
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
                .header("Authorization", API_KEY)
                .header("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                Log.e("ROTA_HTTP_ERRO", "Código de erro: " + response.code() + " - " + response.message());
                if (response.code() == 429) {
                    runOnUiThread(() -> Toast.makeText(this, "Limite de requisições excedido ao buscar rota. Tente novamente mais tarde.", Toast.LENGTH_LONG).show());
                }
                return;
            }

            String responseData = response.body().string();
            JSONObject json = new JSONObject(responseData);

            Log.d("ROTA_JSON", json.toString(4));

            if (json.has("error")) {
                String errorMessage = json.getJSONObject("error").getString("message");
                Log.e("ROTA_API_ERRO", "A API retornou um erro ao buscar rota: " + errorMessage);
                runOnUiThread(() -> Toast.makeText(this, "Erro na API ao buscar rota: " + errorMessage, Toast.LENGTH_LONG).show());
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
                // Limpa rotas anteriores e marcadores de origem/destino antes de desenhar a nova
                map.getOverlays().removeIf(overlay -> overlay instanceof Polyline);
                map.getOverlays().removeIf(overlay -> overlay instanceof Marker && (
                        ((Marker) overlay).getTitle() != null &&
                                (((Marker) overlay).getTitle().equals("Origem") || ((Marker) overlay).getTitle().equals("Destino"))));

                Polyline routeLine = new Polyline();
                routeLine.setPoints(geoPoints);
                routeLine.setColor(Color.BLUE);
                routeLine.setWidth(10f);
                map.getOverlays().add(routeLine);

                // Adiciona marcadores de origem e destino na rota
                adicionarMarcador(origem, "Origem", Color.GREEN);
                adicionarMarcador(destino, "Destino", Color.RED);

                // Zoom para a rota completa, se houver pontos
                if (!geoPoints.isEmpty()) {
                    BoundingBox boundingBox = BoundingBox.fromGeoPoints(geoPoints);
                    map.zoomToBoundingBox(boundingBox, true, 100);
                }


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
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            boolean fineLocationGranted = false;
            for (int i = 0; i < permissions.length; i++) {
                if (permissions[i].equals(Manifest.permission.ACCESS_FINE_LOCATION) && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    fineLocationGranted = true;
                }
            }

            if (fineLocationGranted) {
                if (locationManager != null) {
                    try {
                        // Re-registra as atualizações de localização após a permissão ser concedida
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, this);
                        // Tenta obter a última localização conhecida novamente
                        Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        if (lastKnownLocation != null && !isInitialLocationSet) {
                            currentLocation = new GeoPoint(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                            reverseGeocode(currentLocation, etOrigem);
                            isInitialLocationSet = true;
                        }
                        etOrigem.setEnabled(true); // Habilita o campo de origem
                        etOrigem.setHint("Sua localização atual");
                    } catch (SecurityException e) {
                        Log.e("PERMISSIONS", "SecurityException ao tentar requestLocationUpdates: " + e.getMessage());
                        Toast.makeText(this, "Não foi possível iniciar as atualizações de localização.", Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                Toast.makeText(this, "Permissão de localização negada. Algumas funcionalidades podem não funcionar.", Toast.LENGTH_LONG).show();
                etOrigem.setHint("Permissão de localização necessária");
                etOrigem.setEnabled(false); // Mantém o campo de origem desabilitado
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        currentLocation = new GeoPoint(location.getLatitude(), location.getLongitude());

        // Log para depuração do debounce
        Log.d("GPS_DEBOUNCE", "onLocationChanged chamado. Lat: " + location.getLatitude() + ", Lon: " + location.getLongitude());

        // Se é a primeira atualização ou o campo de origem está vazio e o usuário não está editando
        if (!isInitialLocationSet || (etOrigem.getText().toString().isEmpty() && !etOrigem.isFocused())) {
            // Remove qualquer runnable anterior para garantir que apenas o mais recente seja executado
            if (reverseGeocodeRunnable != null) {
                locationHandler.removeCallbacks(reverseGeocodeRunnable);
            }

            reverseGeocodeRunnable = () -> {
                Log.d("GPS_DEBOUNCE", "Executando reverseGeocode após debounce.");
                reverseGeocode(currentLocation, etOrigem);
                isInitialLocationSet = true; // Marca que a localização inicial foi definida
                // Opcional: Centralizar o mapa na localização atual, mas apenas se o usuário não estiver focado nos EditText
                if (!etOrigem.isFocused() && !etDestino.isFocused()) {
                    map.getController().animateTo(currentLocation);
                }
            };
            // Agenda a execução da geocodificação reversa
            locationHandler.postDelayed(reverseGeocodeRunnable, REVERSE_GEOCODE_DELAY_MS);
        } else {
            Log.d("GPS_DEBOUNCE", "Ignorando reverseGeocode (debounce ou campo sendo editado).");
        }
    }

    @Override
    public void onProviderEnabled(String provider) {
        Toast.makeText(this, "GPS habilitado!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onProviderDisabled(String provider) {
        Toast.makeText(this, "GPS desabilitado! Por favor, habilite para melhor experiência.", Toast.LENGTH_LONG).show();
    }

    private void buscarMotoristasDisponiveis() {
        // Altere a consulta inicial para buscar *todos* os usuários, e então filtre localmente
        // Isso é feito para poder aplicar o filtro isDriver() que não pode ser usado com orderByChild("isLookingForRide").equalTo(false) diretamente se isDriver for outro nó.
        databaseReference.child("usuarios")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        motoristasDisponiveis.clear();
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Usuario usuario = snapshot.getValue(Usuario.class);
                            // Filtra por motoristas que não estão procurando carona e são de fato motoristas
                            if (usuario != null && usuario.getNome() != null && usuario.isDriver() && !usuario.isLookingForRide()) {
                                // Adicione um filtro para garantir que não é o próprio usuário (passageiro)
                                if (auth.getCurrentUser() != null && !usuario.getUid().equals(auth.getCurrentUser().getUid())) {
                                    motoristasDisponiveis.add(usuario);
                                }
                            }
                        }
                        motoristasAdapter.notifyDataSetChanged();

                        if (!motoristasDisponiveis.isEmpty()) {
                            tvMotoristasDisponiveis.setVisibility(View.VISIBLE);
                            lvMotoristas.setVisibility(View.VISIBLE);
                        } else {
                            tvMotoristasDisponiveis.setVisibility(View.GONE);
                            lvMotoristas.setVisibility(View.GONE);
                            Toast.makeText(TelaPassageiro.this, "Nenhum motorista disponível no momento.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.e("Firebase", "Erro ao buscar motoristas: " + databaseError.getMessage());
                        Toast.makeText(TelaPassageiro.this, "Erro ao carregar motoristas.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (map != null) map.onResume();
        // Re-registra as atualizações de localização ao retornar à Activity
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, this);
            } catch (SecurityException e) {
                Log.e("LOCATION", "SecurityException em onResume: " + e.getMessage());
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (map != null) map.onPause();
        // Remove as atualizações de localização quando a Activity não está em primeiro plano
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
        // Limpa callbacks pendentes para evitar vazamentos de memória ou chamadas após a pausa
        if (locationHandler != null) {
            locationHandler.removeCallbacksAndMessages(null);
        }
        if (etHandler != null) {
            etHandler.removeCallbacksAndMessages(null);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Finaliza o mapa e libera recursos
        if (map != null) {
            map.onDetach();
            map = null;
        }
        // Garante que todos os callbacks sejam removidos ao destruir a Activity
        if (locationHandler != null) {
            locationHandler.removeCallbacksAndMessages(null);
        }
        if (etHandler != null) {
            etHandler.removeCallbacksAndMessages(null);
        }
    }
}
