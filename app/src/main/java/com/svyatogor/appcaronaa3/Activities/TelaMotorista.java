package com.svyatogor.appcaronaa3.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.svyatogor.appcaronaa3.Interfaces.OnAcceptRideClickListener; // Importe a nova interface
import com.svyatogor.appcaronaa3.Model.PassengerRequestAdapter;
import com.svyatogor.appcaronaa3.Model.Usuario;
import com.svyatogor.appcaronaa3.R;
import org.osmdroid.views.MapView; // Mantido, mas não usado diretamente nesta lógica.

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TelaMotorista extends AppCompatActivity implements OnAcceptRideClickListener {
    private RecyclerView recyclerViewPassengerRequests;
    private PassengerRequestAdapter passengerRequestAdapter;
    private List<Usuario> passengerList;
    private TextView tvNoRequests;

    private FirebaseAuth auth;
    private DatabaseReference usersRef;
    private DatabaseReference ridesRef;

    private ValueEventListener passengerRequestListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tela_motorista);

        auth = FirebaseAuth.getInstance();
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        usersRef = firebaseDatabase.getReference("usuarios");
        ridesRef = firebaseDatabase.getReference("viagens"); // Nó para as viagens/caronas

        // Verifique se o usuário atual é um motorista (opcional, mas recomendado)
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Faça login como motorista.", Toast.LENGTH_LONG).show();
            finish(); // Fecha a Activity se não houver usuário logado
            // Você pode redirecionar para uma tela de login aqui
            startActivity(new Intent(this, TelaLogin.class)); // Exemplo de redirecionamento
            return;
        }

        recyclerViewPassengerRequests = findViewById(R.id.recyclerViewPassengerRequests);
        tvNoRequests = findViewById(R.id.tvNoRequests);
        recyclerViewPassengerRequests.setLayoutManager(new LinearLayoutManager(this));

        passengerList = new ArrayList<>();
        // Inicializa o adaptador passando a lista e 'this' como o listener
        passengerRequestAdapter = new PassengerRequestAdapter(passengerList, this::onAcceptRideClick);
        recyclerViewPassengerRequests.setAdapter(passengerRequestAdapter);

        // Ir para o perfil
        Button btnDriverProfile = findViewById(R.id.btnDriverProfile);
        btnDriverProfile.setOnClickListener(v -> {
            Toast.makeText(TelaMotorista.this, "Navegar para o Perfil seu perfil", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(TelaMotorista.this, PerfilUser.class));
        });

        // Inicia a escuta por passageiros no Realtime Database
        listenForPassengerRequests();
    }

    private void listenForPassengerRequests() {
        // Ouve por usuários que são passageiros e estão buscando carona
        passengerRequestListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                passengerList.clear(); // Limpa a lista para adicionar os dados atualizados
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    Usuario usuario = userSnapshot.getValue(Usuario.class);
                    // Adicionando uma verificação para garantir que o usuário não é um motorista e está procurando carona
                    if (usuario != null && !usuario.isDriver() && usuario.isLookingForRide()) {
                        // Certifique-se de que o UID do usuário é setado no objeto Usuario
                        usuario.setUid(userSnapshot.getKey());
                        passengerList.add(usuario);
                    }
                }
                passengerRequestAdapter.notifyDataSetChanged(); // Notifica o adaptador

                // Mostra/oculta a mensagem de "nenhuma solicitação"
                if (passengerList.isEmpty()) {
                    tvNoRequests.setVisibility(View.VISIBLE);
                    recyclerViewPassengerRequests.setVisibility(View.GONE);
                } else {
                    tvNoRequests.setVisibility(View.GONE);
                    recyclerViewPassengerRequests.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w("TelaMotorista", "Erro ao carregar solicitações de passageiros: " + error.getMessage());
                Toast.makeText(TelaMotorista.this, "Erro ao carregar solicitações.", Toast.LENGTH_SHORT).show();
            }
        };

        // Adiciona o listener ao nó "usuarios"
        usersRef.addValueEventListener(passengerRequestListener);
    }

    @Override
    public void onAcceptRideClick(Usuario passageiro) {
        FirebaseUser driver = auth.getCurrentUser();
        if (driver == null) {
            Toast.makeText(this, "Você precisa estar logado para aceitar uma carona.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Lógica para aceitar a carona:
        // 1. Marcar o passageiro como 'não procurando' no nó de usuários
        // 2. Criar um novo nó de 'viagem' ou 'carona' no Realtime Database
        //    com informações do motorista, passageiro, locais, status, etc.

        // Passo 1: Atualizar o status do passageiro
        usersRef.child(passageiro.getUid())
                .child("isLookingForRide")
                .setValue(false)
                .addOnSuccessListener(aVoid -> {
                    Log.d("DriverActivity", "Status do passageiro " + passageiro.getNome() + " atualizado.");
                    Toast.makeText(TelaMotorista.this, "Carona de " + passageiro.getNome() + " aceita!", Toast.LENGTH_SHORT).show();

                    // Passo 2: Criar um registro de viagem
                    String rideId = ridesRef.push().getKey(); // Gera um ID único para a viagem
                    if (rideId != null) {
                        Map<String, Object> rideDetails = new HashMap<>();
                        rideDetails.put("driverUid", driver.getUid());
                        rideDetails.put("passengerUid", passageiro.getUid());
                        rideDetails.put("pickupLocation", passageiro.getPontoDeChegada());
                        rideDetails.put("destination", passageiro.getDestino());
                        rideDetails.put("status", "accepted"); // "pending", "accepted", "in_progress", "completed", "cancelled"
                        rideDetails.put("timestampAccepted", System.currentTimeMillis());

                        ridesRef.child(rideId).setValue(rideDetails)
                                .addOnSuccessListener(aVoid1 -> {
                                    Log.d("TelaMotorista", "Viagem criada com ID: " + rideId);
                                    Toast.makeText(TelaMotorista.this, "Viagem iniciada! Detalhes em breve.", Toast.LENGTH_LONG).show();

                                    // Opcional: Remover o passageiro da lista de solicitações pendentes
                                    // A lista será atualizada automaticamente pelo listener,
                                    // então não precisamos remover manualmente aqui.

                                    // Em um aplicativo real, você navegaria para uma tela de acompanhamento da viagem
                                    // com mapa, informações do passageiro, etc.
                                    // Exemplo: startActivity(new Intent(TelaMotorista.this, RideTrackingActivity.class));
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("TelaMotorista", "Erro ao criar viagem: " + e.getMessage());
                                    Toast.makeText(TelaMotorista.this, "Erro ao iniciar viagem.", Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("TelaMotorista", "Erro ao aceitar carona (atualizar passageiro): " + e.getMessage());
                    Toast.makeText(TelaMotorista.this, "Erro ao aceitar carona.", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remova o listener para evitar vazamentos de memória e garantir que ele não continue ouvindo
        if (passengerRequestListener != null) {
            usersRef.removeEventListener(passengerRequestListener);
        }
    }
}
