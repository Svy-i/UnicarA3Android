package com.svyatogor.appcaronaa3;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class TelaCadastro extends AppCompatActivity {

    private Button btCadastro;
    private EditText etNomeCadastro;
    private EditText etEmailCadastro;
    private EditText etSenhaCadastro;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_tela_cadastro);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        iniciarComponentes();
        btCadastrar();
    } // fim do onCreate

    private void iniciarComponentes(){
        btCadastro = findViewById(R.id.bt_cadastro);
        etNomeCadastro = findViewById(R.id.et_nome_cadastro);
        etEmailCadastro = findViewById(R.id.et_email_cadastro);
        etSenhaCadastro = findViewById(R.id.et_senha_cadastro);
    }
    private void btCadastrar() {
        btCadastro.setOnClickListener(v -> {
            startActivity(new Intent(this, TelaLogin.class));
        });
    }

}