package com.svyatogor.appcaronaa3.Activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.svyatogor.appcaronaa3.Interfaces.OnAcceptRideClickListener;
import com.svyatogor.appcaronaa3.Adapters.PassengerRequestAdapter;
import com.svyatogor.appcaronaa3.Model.Usuario;
import com.svyatogor.appcaronaa3.R;

// Imports para funcionalidade de mapa
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

public class TelaMotorista extends AppCompatActivity implements OnAcceptRideClickListener {
    private RecyclerView recyclerViewPassengerRequests;
    private PassengerRequestAdapter passengerRequestAdapter;
    private List<Usuario> passengerList;
    private TextView tvNoRequests;

    private FirebaseAuth auth;
    private DatabaseReference usersRef;
    private DatabaseReference solicitacoesCaronaRef;

    private ValueEventListener solicitacoesCaronaListener;
    private String currentAcceptedRideId = null;
    private Button btnCancelTrip;

    // Campos para funcionalidade de mapa
    private MapView map;
    private MyLocationNewOverlay mLocationOverlay;
    private TextView tvTripDetails; // Para exibir detalhes da viagem no mapa
    private final String API_KEY = "5b3ce3597851110001cf624859742347a61f4c38a2cf371021231169"; // Sua chave API

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Configuração do Osmdroid (deve ser feita antes de setContentView)
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_tela_motorista);

        auth = FirebaseAuth.getInstance();
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        usersRef = firebaseDatabase.getReference("usuarios");
        solicitacoesCaronaRef = firebaseDatabase.getReference("solicitacoes_carona");

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Faça login como motorista.", Toast.LENGTH_LONG).show();
            finish();
            startActivity(new Intent(this, TelaLogin.class));
            return;
        }

        // Inicialização dos componentes da UI
        recyclerViewPassengerRequests = findViewById(R.id.recyclerViewPassengerRequests);
        tvNoRequests = findViewById(R.id.tvNoRequests);
        map = findViewById(R.id.map_view_motorista); // Inicialize o MapView
        tvTripDetails = findViewById(R.id.tvTripDetails); // Inicialize o TextView de detalhes da viagem
        btnCancelTrip = findViewById(R.id.btn_cancelar_viagem);

        recyclerViewPassengerRequests.setLayoutManager(new LinearLayoutManager(this));

        passengerList = new ArrayList<>();
        passengerRequestAdapter = new PassengerRequestAdapter(passengerList, this);
        recyclerViewPassengerRequests.setAdapter(passengerRequestAdapter);

        btnCancelTrip.setVisibility(View.GONE);
        tvTripDetails.setVisibility(View.GONE); // Ocultar detalhes da viagem inicialmente
        map.setVisibility(View.GONE); // Ocultar mapa inicialmente

        btnCancelTrip.setOnClickListener(v -> cancelCurrentTrip());

        setupMap(); // Configura o mapa

        listenForPassengerRequests();
        checkActiveRideStatus();
    }

    // Configuração básica do mapa
    private void setupMap() {
        if (map != null) {
            map.setTileSource(TileSourceFactory.MAPNIK);
            map.setMultiTouchControls(true);
            IMapController mapController = map.getController();
            mapController.setZoom(14.5);
            mapController.setCenter(new GeoPoint(-19.4600, -44.2486)); // Coordenadas de Sete Lagoas, MG

            mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), map);
            mLocationOverlay.enableMyLocation();
            map.getOverlays().add(mLocationOverlay);
        }
    }

    private void listenForPassengerRequests() {
        // --- MUDANÇA PRINCIPAL AQUI: Remover listener anterior antes de adicionar um novo ---
        if (solicitacoesCaronaListener != null) {
            solicitacoesCaronaRef.removeEventListener(solicitacoesCaronaListener);
            Log.d("TelaMotorista", "Listener de solicitações de carona anterior removido.");
        }

        solicitacoesCaronaListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d("TelaMotorista", "onDataChange - Nova atualização de solicitações.");
                passengerList.clear(); // Limpa a lista para adicionar os dados atualizados

                // Se o motorista já aceitou uma carona, não mostre novas solicitações
                if (currentAcceptedRideId != null) {
                    Log.d("TelaMotorista", "Motorista tem viagem ativa (" + currentAcceptedRideId + "), ocultando novas solicitações.");
                    updateUIForActiveRide(); // Garante que a UI esteja correta
                    return; // Retorna pois não deve processar mais solicitações
                }

                List<Usuario> newPassengerRequests = new ArrayList<>();
                final int[] pendingFetches = {0}; // Contador para garantir que todas as buscas de usuário terminem
                boolean anyPendingRequestsFound = false; // Flag para saber se alguma solicitação "pendente" foi encontrada no loop

                if (!snapshot.hasChildren()) {
                    Log.d("TelaMotorista", "Nenhuma solicitação encontrada no nó 'solicitacoes_carona'.");
                    updateRequestListUI(); // Atualiza a UI para mostrar "nenhuma solicitação"
                    return;
                }

                for (DataSnapshot requestSnapshot : snapshot.getChildren()) {
                    Log.d("TelaMotorista", "Raw request snapshot: " + requestSnapshot.toString());

                    String status = requestSnapshot.child("status").getValue(String.class);
                    String passengerUid = null;
                    // --- CORREÇÃO AQUI: Mudado para "passageiroUid" ---
                    DataSnapshot passengerUidChild = requestSnapshot.child("passageiroUid");

                    if (passengerUidChild.exists()) {
                        Object rawValue = passengerUidChild.getValue();
                        if (rawValue instanceof String) {
                            passengerUid = (String) rawValue;
                        } else if (rawValue != null) {
                            passengerUid = String.valueOf(rawValue);
                        }
                    }

                    String requestId = requestSnapshot.getKey();

                    Log.d("TelaMotorista", "Processando solicitação: " + requestId + ", Status: " + status + ", Passageiro UID: " + passengerUid);


                    // Apenas mostra solicitações 'pendente' e com passengerUid válido (não nulo e não vazio)
                    if ("pendente".equals(status) && passengerUid != null && !passengerUid.isEmpty()) {
                        Log.d("TelaMotorista", "Solicitação PENDENTE e VÁLIDA encontrada para processamento: " + requestId + ", Passageiro UID: " + passengerUid);
                        anyPendingRequestsFound = true; // Marca que uma solicitação pendente foi encontrada
                        pendingFetches[0]++;
                        final String finalPassengerUid = passengerUid; // Variável final para uso no listener aninhado
                        usersRef.child(passengerUid).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                                Usuario passageiro = userSnapshot.getValue(Usuario.class);
                                if (passageiro != null) {
                                    passageiro.setUid(userSnapshot.getKey());
                                    passageiro.setOrigem(requestSnapshot.child("origem").getValue(String.class));
                                    passageiro.setDestino(requestSnapshot.child("destino").getValue(String.class));
                                    passageiro.setRequestId(requestId);

                                    newPassengerRequests.add(passageiro);
                                    Log.d("TelaMotorista", "Adicionado passageiro " + passageiro.getNome() + " da solicitação " + requestId);
                                } else {
                                    Log.w("TelaMotorista", "Usuário passageiro não encontrado para UID: " + finalPassengerUid + " da solicitação: " + requestId);
                                }
                                pendingFetches[0]--;
                                if (pendingFetches[0] == 0) {
                                    passengerList.addAll(newPassengerRequests);
                                    passengerRequestAdapter.notifyDataSetChanged();
                                    updateRequestListUI();
                                    Log.d("TelaMotorista", "Todas as buscas de usuário concluídas. Lista finalizada com " + passengerList.size() + " solicitações.");
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.e("TelaMotorista", "Erro ao buscar nome do passageiro para solicitação " + requestId + ": " + error.getMessage());
                                pendingFetches[0]--;
                                if (pendingFetches[0] == 0) {
                                    passengerList.addAll(newPassengerRequests);
                                    passengerRequestAdapter.notifyDataSetChanged();
                                    updateRequestListUI();
                                }
                            }
                        });
                    } else {
                        Log.d("TelaMotorista", "Solicitação " + requestId + " filtrada. Status: " + status + ", Passenger UID (processed): " + passengerUid + ". Motivo do filtro: " +
                                (!("pendente".equals(status)) ? "status não pendente" : "") +
                                (passengerUid == null ? " passengerUid nulo" : "") +
                                ((passengerUid != null && passengerUid.isEmpty()) ? " passengerUid vazio" : ""));
                    }
                }

                // Este bloco é executado apenas se NENHUMA solicitação pendente com passengerUid válido
                // foi encontrada no loop síncrono. Isso evita que updateRequestListUI() seja chamado prematuramente.
                if (!anyPendingRequestsFound && pendingFetches[0] == 0) {
                    Log.d("TelaMotorista", "Nenhuma solicitação pendente válida encontrada no loop inicial. Atualizando UI.");
                    updateRequestListUI();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("TelaMotorista", "Erro ao carregar solicitações de passageiros (onCancelled): " + error.getMessage());
                Toast.makeText(TelaMotorista.this, "Erro ao carregar solicitações.", Toast.LENGTH_SHORT).show();
                updateRequestListUI();
            }
        };

        solicitacoesCaronaRef.addValueEventListener(solicitacoesCaronaListener);
    }

    private void updateRequestListUI() {
        if (currentAcceptedRideId == null) {
            // Se não há viagem ativa, mostra a lista de solicitações ou a mensagem "nenhuma solicitação"
            map.setVisibility(View.GONE);
            tvTripDetails.setVisibility(View.GONE);
            btnCancelTrip.setVisibility(View.GONE); // Garante que o botão de cancelar esteja oculto

            if (passengerList.isEmpty()) {
                tvNoRequests.setVisibility(View.VISIBLE);
                recyclerViewPassengerRequests.setVisibility(View.GONE);
                Log.d("TelaMotorista", "UI: Nenhum pedido, mostrando mensagem 'NoRequests'.");
            } else {
                tvNoRequests.setVisibility(View.GONE);
                recyclerViewPassengerRequests.setVisibility(View.VISIBLE);
                Log.d("TelaMotorista", "UI: Pedidos encontrados, mostrando RecyclerView. Total: " + passengerList.size());
            }
        } else {
            // Se há uma viagem ativa, oculte a lista de solicitações e mostre o mapa e o botão de cancelar
            tvNoRequests.setVisibility(View.GONE);
            recyclerViewPassengerRequests.setVisibility(View.GONE);
            map.setVisibility(View.VISIBLE);
            tvTripDetails.setVisibility(View.VISIBLE);
            btnCancelTrip.setVisibility(View.VISIBLE);
            Log.d("TelaMotorista", "UI: Viagem ativa detectada, ocultando lista de pedidos e mostrando mapa.");
        }
    }


    private void checkActiveRideStatus() {
        FirebaseUser driver = auth.getCurrentUser();
        if (driver == null) return;

        solicitacoesCaronaRef.orderByChild("driverUid").equalTo(driver.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        currentAcceptedRideId = null;
                        Log.d("TelaMotorista", "checkActiveRideStatus: Verificando viagens ativas para o motorista " + driver.getUid());

                        for (DataSnapshot rideSnapshot : snapshot.getChildren()) {
                            String status = rideSnapshot.child("status").getValue(String.class);
                            if ("accepted".equals(status) || "in_progress".equals(status)) {
                                currentAcceptedRideId = rideSnapshot.getKey();
                                Log.d("TelaMotorista", "checkActiveRideStatus: Viagem ativa encontrada: " + currentAcceptedRideId + " com status: " + status);
                                // Carrega os detalhes da viagem para exibir no mapa
                                String origem = rideSnapshot.child("origem").getValue(String.class);
                                String destino = rideSnapshot.child("destino").getValue(String.class);
                                if (origem != null && destino != null) {
                                    displayAcceptedRideOnMap(origem, destino);
                                }
                                break;
                            } else {
                                Log.d("TelaMotorista", "checkActiveRideStatus: Solicitacao " + rideSnapshot.getKey() + " com status " + status + " nao e ativa.");
                            }
                        }
                        updateUIForActiveRide();
                        Log.d("TelaMotorista", "checkActiveRideStatus: currentAcceptedRideId final: " + currentAcceptedRideId);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("TelaMotorista", "Erro ao verificar viagem ativa: " + error.getMessage());
                    }
                });
    }

    private void updateUIForActiveRide() {
        if (currentAcceptedRideId != null) {
            recyclerViewPassengerRequests.setVisibility(View.GONE);
            tvNoRequests.setVisibility(View.GONE);
            btnCancelTrip.setVisibility(View.VISIBLE);
            map.setVisibility(View.VISIBLE); // Mostra o mapa
            tvTripDetails.setVisibility(View.VISIBLE); // Mostra os detalhes da viagem
            Toast.makeText(TelaMotorista.this, "Você tem uma viagem ativa. Cancele para ver novas solicitações.", Toast.LENGTH_LONG).show();
            Log.d("TelaMotorista", "UI Atualizada: Mostrando botão de cancelar, ocultando solicitações, mostrando mapa.");
        } else {
            btnCancelTrip.setVisibility(View.GONE);
            map.setVisibility(View.GONE); // Oculta o mapa
            tvTripDetails.setVisibility(View.GONE); // Oculta os detalhes da viagem
            Log.d("TelaMotorista", "UI Atualizada: Ocultando botão de cancelar e mapa. Forçando nova busca de solicitações.");
            listenForPassengerRequests();
        }
    }

    @Override
    public void onAcceptRideClick(Usuario passageiroAceito) {
        FirebaseUser driver = auth.getCurrentUser();
        if (driver == null) {
            Toast.makeText(this, "Você precisa estar logado para aceitar uma carona.", Toast.LENGTH_SHORT).show();
            return;
        }

        String requestId = passageiroAceito.getRequestId();
        String passengerUid = passageiroAceito.getUid();
        String passengerName = passageiroAceito.getNome();
        String origem = passageiroAceito.getOrigem();
        String destino = passageiroAceito.getDestino();

        if (requestId == null || passengerUid == null) {
            Toast.makeText(this, "Erro: Dados da solicitação inválidos.", Toast.LENGTH_SHORT).show();
            return;
        }

        solicitacoesCaronaRef.child(requestId)
                .updateChildren(new HashMap<String, Object>() {{
                    put("status", "accepted");
                    put("driverUid", driver.getUid());
                    put("timestampAccepted", System.currentTimeMillis());
                }})
                .addOnSuccessListener(aVoid -> {
                    Log.d("TelaMotorista", "Solicitação " + requestId + " aceita pelo motorista " + driver.getUid());
                    Toast.makeText(TelaMotorista.this, "Carona de " + passengerName + " aceita!", Toast.LENGTH_SHORT).show();

                    usersRef.child(driver.getUid())
                            .child("isLookingForRide")
                            .setValue(false)
                            .addOnSuccessListener(aVoidDriver -> {
                                Log.d("TelaMotorista", "Status do motorista atualizado para não procurando carona.");

                                usersRef.child(passengerUid)
                                        .child("isLookingForRide")
                                        .setValue(false)
                                        .addOnSuccessListener(aVoidPassenger -> {
                                            Log.d("TelaMotorista", "Status do passageiro atualizado para não procurando carona.");
                                            currentAcceptedRideId = requestId;
                                            updateUIForActiveRide(); // Atualiza a UI para mostrar o botão de cancelar e o mapa
                                            displayAcceptedRideOnMap(origem, destino); // Desenha a rota no mapa
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e("TelaMotorista", "Erro ao atualizar status do passageiro: " + e.getMessage());
                                            Toast.makeText(TelaMotorista.this, "Carona aceita, mas erro ao atualizar status do passageiro.", Toast.LENGTH_SHORT).show();
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Log.e("TelaMotorista", "Erro ao atualizar status do motorista: " + e.getMessage());
                                Toast.makeText(TelaMotorista.this, "Erro ao atualizar seu status.", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("TelaMotorista", "Erro ao aceitar solicitação " + requestId + ": " + e.getMessage());
                    Toast.makeText(TelaMotorista.this, "Erro ao aceitar carona.", Toast.LENGTH_SHORT).show();
                });
    }

    private void cancelCurrentTrip() {
        if (currentAcceptedRideId == null) {
            Toast.makeText(this, "Não há viagem ativa para cancelar.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnCancelTrip.setEnabled(false);

        solicitacoesCaronaRef.child(currentAcceptedRideId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Log the raw snapshot to confirm its content
                Log.d("TelaMotorista", "cancelCurrentTrip - Raw snapshot: " + snapshot.toString());

                if (snapshot.exists()) {
                    String passengerUid;
                    // --- CORREÇÃO AQUI: Mudado para "passageiroUid" ---
                    DataSnapshot passengerUidChild = snapshot.child("passageiroUid");

                    // Robust extraction of passengerUid
                    if (passengerUidChild.exists()) {
                        Object rawValue = passengerUidChild.getValue();
                        if (rawValue instanceof String) {
                            passengerUid = (String) rawValue;
                            Log.d("TelaMotorista", "cancelCurrentTrip - Successfully extracted passengerUid: " + passengerUid);
                        } else if (rawValue != null) {
                            passengerUid = String.valueOf(rawValue);
                            Log.w("TelaMotorista", "cancelCurrentTrip - passengerUid is not a String. Converted: " + passengerUid);
                        } else {
                            passengerUid = null;
                            Log.d("TelaMotorista", "cancelCurrentTrip - passengerUid rawValue is null.");
                        }
                    } else {
                        passengerUid = null;
                        Log.d("TelaMotorista", "cancelCurrentTrip - passengerUid child does not exist.");
                    }

                    FirebaseUser driver = auth.getCurrentUser();
                    Log.d("TelaMotorista", "cancelCurrentTrip - Current driver UID: " + (driver != null ? driver.getUid() : "null"));

                    if (driver == null || passengerUid == null || passengerUid.isEmpty()) { // Added isEmpty check
                        Log.e("TelaMotorista", "Erro ao obter detalhes da viagem para cancelar. Driver: " + (driver != null ? driver.getUid() : "null") + ", Passenger UID: " + passengerUid);
                        Toast.makeText(TelaMotorista.this, "Erro ao obter detalhes da viagem para cancelar.", Toast.LENGTH_SHORT).show();
                        btnCancelTrip.setEnabled(true);
                        return;
                    }

                    Map<String, Object> rideUpdates = new HashMap<>();
                    rideUpdates.put("status", "cancelled");
                    rideUpdates.put("timestampCancelled", System.currentTimeMillis());

                    solicitacoesCaronaRef.child(currentAcceptedRideId).updateChildren(rideUpdates)
                            .addOnSuccessListener(aVoid -> {
                                Log.d("TelaMotorista", "Viagem " + currentAcceptedRideId + " cancelada.");

                                usersRef.child(driver.getUid()).child("isLookingForRide").setValue(true);
                                usersRef.child(passengerUid).child("isLookingForRide").setValue(true)
                                        .addOnSuccessListener(aVoidUsers -> {
                                            Toast.makeText(TelaMotorista.this, "Viagem cancelada com sucesso. Você está disponível novamente.", Toast.LENGTH_LONG).show();
                                            currentAcceptedRideId = null;
                                            clearMap();
                                            updateUIForActiveRide();
                                            btnCancelTrip.setEnabled(true);
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e("TelaMotorista", "Erro ao atualizar status de usuário após cancelar: " + e.getMessage());
                                            Toast.makeText(TelaMotorista.this, "Viagem cancelada, mas houve um erro ao atualizar os status. " + e.getMessage(), Toast.LENGTH_LONG).show();
                                            btnCancelTrip.setEnabled(true);
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Log.e("TelaMotorista", "Erro ao cancelar viagem: " + e.getMessage());
                                Toast.makeText(TelaMotorista.this, "Erro ao cancelar viagem: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                btnCancelTrip.setEnabled(true);
                            });
                } else {
                    Log.e("TelaMotorista", "cancelCurrentTrip - Snapshot does not exist for rideId: " + currentAcceptedRideId);
                    Toast.makeText(TelaMotorista.this, "Viagem ativa não encontrada para cancelar.", Toast.LENGTH_SHORT).show();
                    currentAcceptedRideId = null;
                    btnCancelTrip.setEnabled(true);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("TelaMotorista", "Erro de banco de dados ao buscar viagem para cancelar (onCancelled): " + error.getMessage());
                Toast.makeText(TelaMotorista.this, "Erro ao cancelar viagem: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                btnCancelTrip.setEnabled(true);
            }
        });
    }

    // --- Métodos de Mapa ---

    private void displayAcceptedRideOnMap(String origem, String destino) {
        tvTripDetails.setText(String.format("Viagem: %s -> %s", origem, destino));
        tvTripDetails.setVisibility(View.VISIBLE);
        map.setVisibility(View.VISIBLE);
        new Thread(() -> {
            try {
                GeoPoint origemCoords = geocodificar(origem);
                GeoPoint destinoCoords = geocodificar(destino);

                if (origemCoords != null && destinoCoords != null) {
                    desenharRotaNoMapaMotorista(origemCoords, destinoCoords);
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(this, "Erro ao obter coordenadas da viagem aceita.", Toast.LENGTH_SHORT).show()
                    );
                }
            } catch (Exception e) {
                Log.e("TelaMotorista", "Erro ao exibir rota da viagem aceita: " + e.getMessage());
                runOnUiThread(() ->
                        Toast.makeText(this, "Erro ao exibir rota da viagem aceita.", Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    private void desenharRotaNoMapaMotorista(GeoPoint origem, GeoPoint destino) throws IOException, JSONException {
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

            runOnUiThread(() -> {
                map.getOverlays().removeIf(overlay -> overlay instanceof Polyline); // Remove rotas antigas
                map.getOverlays().removeIf(overlay -> overlay instanceof Marker && (
                        ((Marker) overlay).getTitle() != null &&
                                (((Marker) overlay).getTitle().equals("Origem") || ((Marker) overlay).getTitle().equals("Destino")))); // Remove marcadores antigos

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

    private GeoPoint geocodificar(String endereco) throws IOException {
        OkHttpClient client = new OkHttpClient();
        String encodedAddress = java.net.URLEncoder.encode(endereco, "UTF-8");
        String url = String.format(Locale.US, "https://api.openrouteservice.org/geocode/search?api_key=%s&text=%s", API_KEY, encodedAddress);

        Request request = new Request.Builder().url(url).build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) return null;
            String responseBody = response.body().string();
            JSONObject json = new JSONObject(responseBody);
            JSONArray features = json.getJSONArray("features");
            if (features.length() > 0) {
                JSONArray coords = features.getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates");
                double lon = coords.getDouble(0);
                double lat = coords.getDouble(1);
                return new GeoPoint(lat, lon);
            }
        } catch (JSONException e) {
            Log.e("GEOCODIFICAR_JSON", "Erro no JSON: " + e.getMessage());
        }
        return null;
    }

    private void adicionarMarcador(GeoPoint p, String titulo, int cor) {
        runOnUiThread(() -> {
            Marker marker = new Marker(map);
            marker.setPosition(p);
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marker.setTitle(titulo);
            map.getOverlays().add(marker);
            map.invalidate();
        });
    }

    private void clearMap() {
        if (map != null) {
            map.getOverlays().clear(); // Limpa todos os overlays (rotas e marcadores)
            mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), map); // Recria o overlay de localização
            mLocationOverlay.enableMyLocation();
            map.getOverlays().add(mLocationOverlay); // Adiciona de volta o overlay de localização
            map.invalidate(); // Força a redesenhar
        }
    }

    // --- Ciclo de vida da Activity para o mapa ---
    @Override
    protected void onResume() {
        super.onResume();
        if (map != null) {
            map.onResume();
            if (mLocationOverlay != null) {
                mLocationOverlay.enableMyLocation();
                mLocationOverlay.enableFollowLocation(); // Opcional: faz o mapa seguir a localização do motorista
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (map != null) {
            map.onPause();
            if (mLocationOverlay != null) {
                mLocationOverlay.disableMyLocation();
                mLocationOverlay.disableFollowLocation();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (solicitacoesCaronaListener != null) {
            solicitacoesCaronaRef.removeEventListener(solicitacoesCaronaListener);
        }
        if (map != null) {
            map.onDetach();
            map = null;
        }
    }
}
