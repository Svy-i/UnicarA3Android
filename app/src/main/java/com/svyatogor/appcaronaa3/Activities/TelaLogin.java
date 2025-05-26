package com.svyatogor.appcaronaa3.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.svyatogor.appcaronaa3.R;

public class TelaLogin extends AppCompatActivity {

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private TextView tvCadastro;
    private TextView tvEsqueciSenha;
    private Button btEntrar;
    private EditText etEmail;
    private EditText etSenha;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_tela_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        inicializarComponentes();
        entrar();
        //Clicar no texto "Criar Conta" leva para a tela de cadastro
        tvCadastro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TelaLogin.this, TelaCadastro.class);
                startActivity(intent);
            }
        });
        //Clicar no texto "Esqueci minha senha" leva para a activity de RecuperarSenha
        tvEsqueciSenha.setOnClickListener(v -> {
            startActivity(new Intent(this, RecuperarSenha.class));
        });
    } // fim do onCreate
    private void inicializarComponentes(){
        tvCadastro = findViewById(R.id.tv_cadastro);
        tvEsqueciSenha = findViewById(R.id.tv_esqueci_senha);
        btEntrar = findViewById(R.id.bt_entrar);
        etEmail = findViewById(R.id.et_email);
        etSenha = findViewById(R.id.et_senha);
    }

    //Clicar no botão de entrar leva para avança para a próxima tela se tudo estiver correto
    private void entrar(){
        btEntrar.setOnClickListener(v -> {
        String emailEntrar = etEmail.getText().toString();
        String senhaEntrar = etSenha.getText().toString();

        if (emailEntrar.isEmpty() || senhaEntrar.isEmpty()){
            Toast.makeText(this, "Preencha todos os campos!", Toast.LENGTH_SHORT).show();
        } else {
            auth.signInWithEmailAndPassword(emailEntrar, senhaEntrar).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()){
                        Toast.makeText(TelaLogin.this, "Seja Bem Vindo!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(TelaLogin.this, MainActivity.class));
                    } else {
                        Toast.makeText(TelaLogin.this, "Email ou Senha errados ", Toast.LENGTH_SHORT).show();
                    }
                }
            });
            }
        });
    }
}