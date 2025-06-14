package com.svyatogor.appcaronaa3.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.svyatogor.appcaronaa3.R;
import com.svyatogor.appcaronaa3.Model.Usuario;

public class MainActivity extends AppCompatActivity {

    private Button btnEntrarMotorista;
    private Button btnEntrarPassageiro;
    private ImageView icUser;
    private FirebaseAuth auth;
    private DatabaseReference usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        auth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference("usuarios");

        btnEntrarMotorista = findViewById(R.id.btn_entrar_motorista);
        btnEntrarPassageiro = findViewById(R.id.btn_entrar_passageiro);
        icUser = findViewById(R.id.ic_user);

        btnEntrarMotorista.setOnClickListener(v -> verificarMotoristaStatus());

        btnEntrarPassageiro.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, TelaPassageiro.class));
        });

        icUser.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, PerfilUser.class));
        });
    }

    private void verificarMotoristaStatus() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            usersRef.child(currentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        Usuario usuario = snapshot.getValue(Usuario.class);
                        if (usuario != null && usuario.isDriver()) {
                            // Usuário é um motorista, vai para TelaMotorista
                            Toast.makeText(MainActivity.this, "Bem-vindo de volta, " + usuario.getNome() + "!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(MainActivity.this, TelaMotorista.class));
                            finish();
                        } else {
                            // Usuário não é motorista, vai para CadastroMotorista
                            Toast.makeText(MainActivity.this, "Por favor, complete seu cadastro de motorista.", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(MainActivity.this, CadastroMotorista.class));
                        }
                    } else {
                        // Não há dados do usuário no nó 'usuarios'
                        Toast.makeText(MainActivity.this, "Por favor, complete seu cadastro de motorista.", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(MainActivity.this, CadastroMotorista.class));
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("MainActivity", "Erro ao verificar status do motorista: " + error.getMessage());
                    Toast.makeText(MainActivity.this, "Erro ao verificar status. Tente novamente.", Toast.LENGTH_SHORT).show();
                    // Em caso de erro, volta pro cadastro
                    startActivity(new Intent(MainActivity.this, CadastroMotorista.class));
                }
            });
        } else {
            // Usuário não logado, redireciona para a tela de login
            Toast.makeText(this, "Por favor, faça login para continuar como motorista.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(MainActivity.this, TelaLogin.class));
        }
    }
}
