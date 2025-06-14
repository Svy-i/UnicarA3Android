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
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.svyatogor.appcaronaa3.Adapters.MotoristaAdapter;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TelaPassageiro extends AppCompatActivity implements LocationListener, MotoristaAdapter.OnAceitarClickListener {

    private MapView map;
    private MyLocationNewOverlay mLocationOverlay;
    private final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private EditText etOrigem, etDestino;
    private Button btnSolicitarCarona;
    private ListView lvMotoristas;
    private TextView tvMotoristasDisponiveis;
    private TextView tvStatusCarona; // Novo TextView para exibir o status da carona

    private LocationManager locationManager;
    private GeoPoint currentLocation;
    private ImageView icUserPassageiro;
    private DatabaseReference databaseReference;
    private FirebaseAuth auth;
    private List<Usuario> motoristasDisponiveis;
    private MotoristaAdapter motoristasAdapter;

    private final String API_KEY = "5b3ce3597851110001cf624859742347a61f4c38a2cf371021231169";

    private Handler locationHandler = new Handler(Looper.getMainLooper());
    private Runnable reverseGeocodeRunnable;
    private static final long REVERSE_GEOCODE_DELAY_MS = 4000;
    private boolean isInitialLocationSet = false;

    private Handler etHandler = new Handler(Looper.getMainLooper());
    private Runnable etOrigemDebounceRunnable, etDestinoDebounceRunnable;
    private static final long ET_DEBOUNCE_DELAY_MS = 1000;

    private String currentRideRequestId = null; // ID da solicitação de carona atual do passageiro
    private ValueEventListener rideRequestListener; // Listener para a solicitação de carona

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_tela_passageiro);

        iniciarComponentes();

        requestPermissionsIfNecessary(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        });

        setupMap();

        databaseReference = FirebaseDatabase.getInstance().getReference();
        auth = FirebaseAuth.getInstance();

        motoristasDisponiveis = new ArrayList<>();
        motoristasAdapter = new MotoristaAdapter(this, motoristasDisponiveis, this);
        lvMotoristas.setAdapter(motoristasAdapter);

        // Inicialmente, tudo relacionado à solicitação de motoristas deve estar oculto
        tvMotoristasDisponiveis.setVisibility(View.GONE);
        lvMotoristas.setVisibility(View.GONE);
        tvStatusCarona.setVisibility(View.GONE); // Ocultar o novo TextView de status inicialmente

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, this);
            Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastKnownLocation != null) {
                currentLocation = new GeoPoint(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                reverseGeocode(currentLocation, etOrigem);
                isInitialLocationSet = true;
            }
        } else {
            Toast.makeText(this, "Permissão de localização não concedida. Por favor, habilite nas configurações.", Toast.LENGTH_LONG).show();
            etOrigem.setHint("Permissão de localização necessária");
            etOrigem.setEnabled(false);
        }

        btnSolicitarCarona.setOnClickListener(v -> solicitarCarona());

        icUserPassageiro.setOnClickListener(v -> {
            startActivity(new Intent(TelaPassageiro.this, PerfilUser.class));
        });

        etOrigem.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
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
            } else {
                if (etOrigemDebounceRunnable != null) {
                    etHandler.removeCallbacks(etOrigemDebounceRunnable);
                }
            }
        });

        etDestino.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
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
            } else {
                if (etDestinoDebounceRunnable != null) {
                    etHandler.removeCallbacks(etDestinoDebounceRunnable);
                }
            }
        });
    }

    private void iniciarComponentes(){
        map = findViewById(R.id.map_view_passageiro);
        etOrigem = findViewById(R.id.et_origem);
        etDestino = findViewById(R.id.et_destino);
        btnSolicitarCarona = findViewById(R.id.btn_solicitar_carona);
        lvMotoristas = findViewById(R.id.lv_motoristas);
        tvMotoristasDisponiveis = findViewById(R.id.tv_motoristas_disponiveis);
        icUserPassageiro = findViewById(R.id.ic_user_passageiro);
        tvStatusCarona = findViewById(R.id.tv_status_carona); // Inicialize o novo TextView
    }

    private void solicitarCarona() {
        String origem = etOrigem.getText().toString().trim();
        String destino = etDestino.getText().toString().trim();

        if (origem.isEmpty() || destino.isEmpty()) {
            Toast.makeText(this, "Por favor, informe a origem e o destino.", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = auth.getCurrentUser();
        // Logando o estado do usuário antes de usar o UID
        Log.d("TelaPassageiro", "Current User: " + (user != null ? user.getUid() : "null"));


        if (user == null) {
            Toast.makeText(this, "Você precisa estar logado para solicitar uma carona.", Toast.LENGTH_SHORT).show();
            Log.e("TelaPassageiro", "Erro: currentUser é null. Não é possível solicitar carona.");
            return;
        }

        // Verificação adicional para o UID ser nulo (embora currentUser não seja nulo)
        if (user.getUid() == null) {
            Toast.makeText(this, "Erro: UID do usuário é nulo. Tente fazer login novamente.", Toast.LENGTH_LONG).show();
            Log.e("TelaPassageiro", "Erro: currentUser.getUid() é null. Não é possível solicitar carona.");
            return;
        }

        // Esconde os campos de input e o botão de solicitar
        etOrigem.setVisibility(View.GONE);
        etDestino.setVisibility(View.GONE);
        btnSolicitarCarona.setVisibility(View.GONE);
        tvMotoristasDisponiveis.setVisibility(View.GONE); // Garante que a lista não esteja visível
        lvMotoristas.setVisibility(View.GONE);

        // Exibe a mensagem de status
        tvStatusCarona.setText("Carona solicitada! Aguardando motorista...");
        tvStatusCarona.setVisibility(View.VISIBLE);

        // Cria uma nova solicitação de carona no Firebase
        currentRideRequestId = databaseReference.child("solicitacoes_carona").push().getKey();
        if (currentRideRequestId != null) {
            Map<String, Object> solicitacaoCarona = new HashMap<>();
            solicitacaoCarona.put("passageiroUid", user.getUid()); // Agora com verificação de null
            solicitacaoCarona.put("origem", origem);
            solicitacaoCarona.put("destino", destino);
            solicitacaoCarona.put("status", "pendente"); // Status inicial
            solicitacaoCarona.put("timestamp", System.currentTimeMillis());

            databaseReference.child("solicitacoes_carona").child(currentRideRequestId).setValue(solicitacaoCarona)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("TelaPassageiro", "Solicitação de carona enviada. ID: " + currentRideRequestId + " Passageiro UID: " + user.getUid());
                        // Inicia a escuta para o status desta solicitação
                        listenForRideRequestStatus(currentRideRequestId);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(TelaPassageiro.this, "Erro ao solicitar carona: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("TelaPassageiro", "Erro ao enviar solicitação de carona: " + e.getMessage());
                        resetPassengerUI(); // Em caso de erro, resetar a UI
                    });
        } else {
            Toast.makeText(TelaPassageiro.this, "Erro ao gerar ID de solicitação.", Toast.LENGTH_SHORT).show();
            resetPassengerUI(); // Em caso de erro, resetar a UI
        }

        // Desenhar a rota no mapa (opcional, pode ser feito após aceitação)
        buscarCoordenadas(origem, destino);
    }

    private void listenForRideRequestStatus(String requestId) {
        // Remover listener anterior se existir para evitar múltiplos listeners para a mesma solicitação
        if (rideRequestListener != null) {
            databaseReference.child("solicitacoes_carona").child(requestId).removeEventListener(rideRequestListener);
        }

        rideRequestListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String status = snapshot.child("status").getValue(String.class);
                String driverUid = snapshot.child("driverUid").getValue(String.class);
                Log.d("TelaPassageiro", "Status da solicitação " + requestId + ": " + status + " Driver UID: " + driverUid);

                if ("accepted".equals(status) && driverUid != null) {
                    // Motorista aceitou a carona
                    tvStatusCarona.setVisibility(View.GONE); // Esconde a mensagem de status
                    tvMotoristasDisponiveis.setText("Motorista Aceito:");
                    tvMotoristasDisponiveis.setVisibility(View.VISIBLE);
                    lvMotoristas.setVisibility(View.VISIBLE);

                    // Busca os dados do motorista aceito
                    databaseReference.child("usuarios").child(driverUid).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot driverSnapshot) {
                            Usuario motoristaAceito = driverSnapshot.getValue(Usuario.class);
                            if (motoristaAceito != null) {
                                motoristaAceito.setUid(driverSnapshot.getKey()); // Garante que o UID está setado
                                motoristasDisponiveis.clear();
                                motoristasDisponiveis.add(motoristaAceito);
                                motoristasAdapter.notifyDataSetChanged();
                                Toast.makeText(TelaPassageiro.this, "Motorista " + motoristaAceito.getNome() + " aceitou sua carona!", Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(TelaPassageiro.this, "Motorista aceito não encontrado no cadastro de usuários.", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e("TelaPassageiro", "Erro ao buscar dados do motorista aceito: " + error.getMessage());
                            Toast.makeText(TelaPassageiro.this, "Erro ao carregar detalhes do motorista.", Toast.LENGTH_SHORT).show();
                        }
                    });

                } else if ("cancelled".equals(status)) {
                    // Motorista cancelou ou solicitação foi cancelada
                    Toast.makeText(TelaPassageiro.this, "Sua solicitação de carona foi cancelada pelo motorista.", Toast.LENGTH_LONG).show();
                    resetPassengerUI();
                } else if (!snapshot.exists()) {
                    // Se a solicitação não existe mais (ex: foi excluída)
                    Toast.makeText(TelaPassageiro.this, "Sua solicitação de carona não existe mais.", Toast.LENGTH_LONG).show();
                    resetPassengerUI();
                }
                // Adicione outras condições de status se necessário (ex: "in_progress", "completed")
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("TelaPassageiro", "Erro ao ouvir status da solicitação: " + error.getMessage());
                Toast.makeText(TelaPassageiro.this, "Erro de comunicação com a carona.", Toast.LENGTH_SHORT).show();
                resetPassengerUI();
            }
        };
        databaseReference.child("solicitacoes_carona").child(requestId).addValueEventListener(rideRequestListener);
    }

    // Método para resetar a UI do passageiro para o estado inicial de solicitação
    private void resetPassengerUI() {
        tvStatusCarona.setVisibility(View.GONE);
        tvMotoristasDisponiveis.setVisibility(View.GONE);
        lvMotoristas.setVisibility(View.GONE);

        etOrigem.setVisibility(View.VISIBLE);
        etDestino.setVisibility(View.VISIBLE);
        btnSolicitarCarona.setVisibility(View.VISIBLE);

        motoristasDisponiveis.clear();
        motoristasAdapter.notifyDataSetChanged();

        if (currentRideRequestId != null && rideRequestListener != null) {
            databaseReference.child("solicitacoes_carona").child(currentRideRequestId).removeEventListener(rideRequestListener);
            currentRideRequestId = null;
        }
        Toast.makeText(TelaPassageiro.this, "Você pode solicitar uma nova carona.", Toast.LENGTH_SHORT).show();
    }


    public void setupMap() {
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
            map.getOverlays().removeIf(overlay -> overlay instanceof Marker && ((Marker) overlay).getTitle() != null && ((Marker) overlay).getTitle().equals(titulo));

            Marker marker = new Marker(map);
            marker.setPosition(p);
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marker.setTitle(titulo);
            map.getOverlays().add(marker);
            map.invalidate();
        });
    }

    private GeoPoint geocodificar(String endereco) throws IOException {
        OkHttpClient client = new OkHttpClient();
        String encodedAddress = java.net.URLEncoder.encode(endereco, "UTF-8");
        String url = "https://api.openrouteservice.org/geocode/search?api_key=%s&text=%s"; // Use %s para formatar

        // Use String.format para injetar a API_KEY e o endereço codificado
        String finalUrl = String.format(Locale.US, url, API_KEY, encodedAddress);

        Log.d("GEOCODIFICAR_URL", finalUrl);

        Request request = new Request.Builder().url(finalUrl).build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                Log.e("GEOCODIFICAR_ERRO", "Erro na resposta: " + response.code() + " - " + response.message());
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
                map.getOverlays().removeIf(overlay -> overlay instanceof Polyline);
                map.getOverlays().removeIf(overlay -> overlay instanceof Marker && (
                        ((Marker) overlay).getTitle() != null &&
                                (((Marker) overlay).getTitle().equals("Origem") || ((Marker) overlay).getTitle().equals("Destino"))));

                Polyline routeLine = new Polyline();
                routeLine.setPoints(geoPoints);
                routeLine.setColor(Color.BLUE);
                routeLine.setWidth(10f);
                map.getOverlays().add(routeLine);

                adicionarMarcador(origem, "Origem", Color.GREEN);
                adicionarMarcador(destino, "Destino", Color.RED);

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
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, this);
                        Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        if (lastKnownLocation != null && !isInitialLocationSet) {
                            currentLocation = new GeoPoint(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                            reverseGeocode(currentLocation, etOrigem);
                            isInitialLocationSet = true;
                        }
                        etOrigem.setEnabled(true);
                        etOrigem.setHint("Sua localização atual");
                    } catch (SecurityException e) {
                        Log.e("PERMISSIONS", "SecurityException ao tentar requestLocationUpdates: " + e.getMessage());
                        Toast.makeText(this, "Não foi possível iniciar as atualizações de localização.", Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                Toast.makeText(this, "Permissão de localização negada. Algumas funcionalidades podem não funcionar.", Toast.LENGTH_LONG).show();
                etOrigem.setHint("Permissão de localização necessária");
                etOrigem.setEnabled(false);
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        currentLocation = new GeoPoint(location.getLatitude(), location.getLongitude());

        Log.d("GPS_DEBOUNCE", "onLocationChanged chamado. Lat: " + location.getLatitude() + ", Lon: " + location.getLongitude());

        if (!isInitialLocationSet || (etOrigem.getText().toString().isEmpty() && !etOrigem.isFocused())) {
            if (reverseGeocodeRunnable != null) {
                locationHandler.removeCallbacks(reverseGeocodeRunnable);
            }

            reverseGeocodeRunnable = () -> {
                Log.d("GPS_DEBOUNCE", "Executando reverseGeocode após debounce.");
                reverseGeocode(currentLocation, etOrigem);
                isInitialLocationSet = true;
                if (!etOrigem.isFocused() && !etDestino.isFocused()) {
                    map.getController().animateTo(currentLocation);
                }
            };
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
        Log.d("TelaPassageiro", "buscarMotoristasDisponiveis() chamado. Esta função agora preenche a lista APENAS com o motorista aceito.");
    }

    @Override
    public void onAceitarClick(Usuario motoristaAceito) {
        if (auth.getCurrentUser() == null || currentRideRequestId == null) {
            Toast.makeText(this, "Erro: Carona não está em estado de aceitação.", Toast.LENGTH_SHORT).show();
            return;
        }

        databaseReference.child("solicitacoes_carona").child(currentRideRequestId)
                .child("status").setValue("in_progress")
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(TelaPassageiro.this, "Carona com " + motoristaAceito.getNome() + " iniciada!", Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(TelaPassageiro.this, "Erro ao iniciar a carona: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("TelaPassageiro", "Erro ao iniciar carona: " + e.getMessage());
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (map != null) map.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, this);
            } catch (SecurityException e) {
                Log.e("LOCATION", "SecurityException em onResume: " + e.getMessage());
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (map != null) map.onPause();
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
        if (locationHandler != null) {
            locationHandler.removeCallbacksAndMessages(null);
        }
        if (etHandler != null) {
            etHandler.removeCallbacksAndMessages(null);
        }
        if (currentRideRequestId != null && rideRequestListener != null) {
            databaseReference.child("solicitacoes_carona").child(currentRideRequestId).removeEventListener(rideRequestListener);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (map != null) {
            map.onDetach();
            map = null;
        }
        if (locationHandler != null) {
            locationHandler.removeCallbacksAndMessages(null);
        }
        if (etHandler != null) {
            etHandler.removeCallbacksAndMessages(null);
        }
        if (currentRideRequestId != null && rideRequestListener != null) {
            databaseReference.child("solicitacoes_carona").child(currentRideRequestId).removeEventListener(rideRequestListener);
        }
    }
}
