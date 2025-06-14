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

    private EditText etModeloCarro, etPlacaCarro, etCorCarro, etAnoCarro;
    private Button btnCadastrar;

    private FirebaseAuth auth;
    private DatabaseReference database;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_cadastro_motorista);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference();

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Por favor, faça login ou cadastre-se primeiro.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(CadastroMotorista.this, TelaLogin.class));
            finish();
            return;
        } else {
            currentUserId = currentUser.getUid();
        }

        iniciarComponentes();

        btnCadastrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registrarComoMotorista();
            }
        });
    } // fim do onCreate

    private void iniciarComponentes(){
        etModeloCarro = findViewById(R.id.et_modelo_carro);
        etPlacaCarro = findViewById(R.id.et_placa_carro);
        etCorCarro = findViewById(R.id.et_cor_carro);
        etAnoCarro = findViewById(R.id.et_ano_carro);
        btnCadastrar = findViewById(R.id.btn_cadastrar_motorista);
    }

    private void registrarComoMotorista() {
        String modeloCarro = etModeloCarro.getText().toString().trim();
        String placaCarro = etPlacaCarro.getText().toString().trim();
        String corCarro = etCorCarro.getText().toString().trim();
        String anoCarroStr = etAnoCarro.getText().toString().trim();

        if (TextUtils.isEmpty(modeloCarro) || TextUtils.isEmpty(placaCarro) ||
                TextUtils.isEmpty(corCarro) || TextUtils.isEmpty(anoCarroStr)) {
            Toast.makeText(this, "Por favor, preencha todos os campos do veículo.", Toast.LENGTH_SHORT).show();
            return;
        }

        int anoCarro;
        try {
            anoCarro = Integer.parseInt(anoCarroStr);
            if (anoCarro < 1900 || anoCarro > 2030) {
                Toast.makeText(this, "Ano do carro inválido.", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Ano do carro inválido. Use apenas números.", Toast.LENGTH_SHORT).show();
            return;
        }

        database.child("usuarios").child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Usuario usuarioExistente = dataSnapshot.getValue(Usuario.class);
                    if (usuarioExistente != null) {
                        Carro carro = new Carro(modeloCarro, placaCarro, corCarro, anoCarro);

                        usuarioExistente.setDriver(true);
                        usuarioExistente.setCarro(carro);

                        database.child("usuarios").child(currentUserId).setValue(usuarioExistente)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            // Manda pra tela do motorista depois de verificar as informações do cadastro e marcar como motorista
                                            Toast.makeText(CadastroMotorista.this, "Seu perfil foi atualizado para motorista!", Toast.LENGTH_LONG).show();
                                            startActivity(new Intent(CadastroMotorista.this, TelaMotorista.class));
                                            finish();
                                        } else {
                                            Toast.makeText(CadastroMotorista.this, "Erro ao atualizar dados do motorista: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                        }
                                    }
                                });
                    }
                } else {
                    Toast.makeText(CadastroMotorista.this, "Perfil de usuário não encontrado.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(CadastroMotorista.this, "Erro ao carregar dados do usuário: " + databaseError.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
