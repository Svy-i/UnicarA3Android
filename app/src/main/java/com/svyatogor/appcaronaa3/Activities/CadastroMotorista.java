package com.svyatogor.appcaronaa3.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.svyatogor.appcaronaa3.Model.Carro;
import com.svyatogor.appcaronaa3.Model.Usuario;
import com.svyatogor.appcaronaa3.R;

public class CadastroMotorista extends AppCompatActivity {

    // UI elements
    private EditText etModeloCarro, etPlacaCarro, etCorCarro, etAnoCarro;
    private Button btnCadastrar;

    // Firebase instances
    private FirebaseAuth auth;
    private DatabaseReference database;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_cadastro_motorista);

        // Adjust padding for system bars (status bar, navigation bar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference(); // Get the root reference

        // Check if user is authenticated
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            // User is not logged in, redirect to login/registration
            Toast.makeText(this, "Por favor, faça login ou cadastre-se primeiro.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(CadastroMotorista.this, TelaLogin.class)); // Assuming TelaLogin is your login activity
            finish();
            return;
        } else {
            currentUserId = currentUser.getUid();
        }

        // Initialize UI elements
        etModeloCarro = findViewById(R.id.et_modelo_carro);
        etPlacaCarro = findViewById(R.id.et_placa_carro);
        etCorCarro = findViewById(R.id.et_cor_carro);
        etAnoCarro = findViewById(R.id.et_ano_carro);
        btnCadastrar = findViewById(R.id.btn_cadastrar_motorista);

        // Set up click listener for the registration button
        btnCadastrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registrarComoMotorista();
            }
        });
    }

    private void registrarComoMotorista() {
        // Get input values from EditText fields
        String modeloCarro = etModeloCarro.getText().toString().trim();
        String placaCarro = etPlacaCarro.getText().toString().trim();
        String corCarro = etCorCarro.getText().toString().trim();
        String anoCarroStr = etAnoCarro.getText().toString().trim();

        // Input validation
        if (TextUtils.isEmpty(modeloCarro) || TextUtils.isEmpty(placaCarro) ||
                TextUtils.isEmpty(corCarro) || TextUtils.isEmpty(anoCarroStr)) {
            Toast.makeText(this, "Por favor, preencha todos os campos do veículo.", Toast.LENGTH_SHORT).show();
            return;
        }

        int anoCarro;
        try {
            anoCarro = Integer.parseInt(anoCarroStr);
            if (anoCarro < 1900 || anoCarro > 2100) { // Basic year validation
                Toast.makeText(this, "Ano do carro inválido.", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Ano do carro inválido. Use apenas números.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Fetch the existing user data from Firebase Realtime Database
        database.child("usuarios").child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Usuario usuarioExistente = dataSnapshot.getValue(Usuario.class);
                    if (usuarioExistente != null) {
                        // Create a Carro object
                        Carro carro = new Carro(modeloCarro, placaCarro, corCarro, anoCarro);

                        // Update the existing Usuario object
                        usuarioExistente.setDriver(true); // Mark as a driver
                        usuarioExistente.setCarro(carro); // Associate the car with the driver

                        // Save the updated user data back to Firebase
                        database.child("usuarios").child(currentUserId).setValue(usuarioExistente)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            Toast.makeText(CadastroMotorista.this, "Seu perfil foi atualizado para motorista!", Toast.LENGTH_LONG).show();
                                            // Redirect to another activity, e.g., driver's main screen
                                            startActivity(new Intent(CadastroMotorista.this, TelaMotorista.class)); // Or a driver specific activity
                                            finish();
                                        } else {
                                            Toast.makeText(CadastroMotorista.this, "Erro ao atualizar dados do motorista: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                        }
                                    }
                                });
                    }
                } else {
                    Toast.makeText(CadastroMotorista.this, "Perfil de usuário não encontrado. Por favor, entre em contato com o suporte.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(CadastroMotorista.this, "Erro ao carregar dados do usuário: " + databaseError.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
