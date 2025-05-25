package com.svyatogor.appcaronaa3.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.svyatogor.appcaronaa3.Model.ConexaoBD;
import com.svyatogor.appcaronaa3.R;

public class MainActivity extends AppCompatActivity {
    ConexaoBD conexaoBD = new ConexaoBD();
    private EditText etOrigem1;
    private EditText etDestino1;
    private EditText etData;
    private EditText etnVagas;
    private Button btPublicarCarona;
    private EditText etOrigem2;
    private EditText etDestino2;
    private Button btBuscarCarona;

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
        iniciarComponentes();
        btMotorista();
        btPassageiro();
    } // final do onCreate
    private void iniciarComponentes(){
        etOrigem1 = findViewById(R.id.et_origem1);
        etDestino1 = findViewById(R.id.et_destino1);
        etData = findViewById(R.id.et_data);
        etnVagas = findViewById(R.id.etn_vagas);
        btPublicarCarona = findViewById(R.id.bt_publicar_carona);
        etOrigem2 = findViewById(R.id.et_origem2);
        etDestino2 = findViewById(R.id.et_destino2);
        btBuscarCarona = findViewById(R.id.bt_buscar_carona);
    }
    private void btMotorista(){
        btPublicarCarona.setOnClickListener(v -> {

            startActivity(new Intent(this, PerfilUser.class));
        });
    }

    private void btPassageiro(){
        btBuscarCarona.setOnClickListener(v -> {
            startActivity(new Intent(this, TelaPassageiro.class));
        });
    }
    

}
/* double valorDistancia = Double.parseDouble(etDistancia.getText().toString());
                double valorConsumo = Double.parseDouble(etConsumoMedio.getText().toString());
                double combustivelGasto = valorDistancia / valorConsumo;
                tvResultado.setText(String.format("Serão gastos: %.2f litros", combustivelGasto));*/