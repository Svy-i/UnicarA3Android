package com.svyatogor.appcaronaa3;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    private EditText etNome;
    private EditText etOrigem1;
    private EditText etDestino1;
    private EditText etData;
    private EditText etnVagas;
    private Button btVerificacao;

    @SuppressLint("MissingInflatedId")
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

        etNome = findViewById(R.id.et_nome);
        etOrigem1 = findViewById(R.id.et_origem1);
        etDestino1 = findViewById(R.id.et_destino1);
        etData = findViewById(R.id.et_data);
        btVerificacao = findViewById(R.id.bt_verificacao);

        btVerificacao.setOnClickListener(v -> {
            startActivity(new Intent(this, SegundaTela.class));
        });

        btVerificacao.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etNome.getText().toString();
                String senha = etOrigem1.getText().toString();
                if (email.equals("A") & senha.equals("B")){
                    Toast.makeText(MainActivity.this, "Email e senha corretos", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(MainActivity.this, SegundaTela.class));
                } else {
                    Toast.makeText(MainActivity.this, "Email e senha incorretos", Toast.LENGTH_SHORT).show();
                }
            }
        });
    } // final do onCreate

}
/* double valorDistancia = Double.parseDouble(etDistancia.getText().toString());
                double valorConsumo = Double.parseDouble(etConsumoMedio.getText().toString());
                double combustivelGasto = valorDistancia / valorConsumo;
                tvResultado.setText(String.format("Serão gastos: %.2f litros", combustivelGasto));*/