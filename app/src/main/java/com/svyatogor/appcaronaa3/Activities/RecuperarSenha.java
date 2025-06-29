package com.svyatogor.appcaronaa3.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.svyatogor.appcaronaa3.R;

public class RecuperarSenha extends AppCompatActivity {

    private EditText etEmailRecuperacao;
    private Button btEnviarEmail;
    private FirebaseAuth auth;
    private ImageView icSetaReturn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recuperar_senha);
        iniciarComponentes();

        btEnviarEmail.setOnClickListener(v -> {
            String email = etEmailRecuperacao.getText().toString().trim();

            if (email.isEmpty()) {
                Toast.makeText(this, "Digite seu email", Toast.LENGTH_SHORT).show();
                return;
            }

            auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Link de recuperação de senha enviado para seu email!", Toast.LENGTH_LONG).show();
                            finish();
                        } else {
                            Toast.makeText(this, "Erro: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });
        icSetaReturn.setOnClickListener(v -> {
            finish();
            startActivity(new Intent(RecuperarSenha.this, TelaLogin.class));
        });
    }//fim do onCreate

    private void iniciarComponentes(){
        etEmailRecuperacao = findViewById(R.id.et_email_recuperacao);
        btEnviarEmail = findViewById(R.id.bt_enviar_email);
        auth = FirebaseAuth.getInstance();
        icSetaReturn = findViewById(R.id.ic_seta_return);
    }
}