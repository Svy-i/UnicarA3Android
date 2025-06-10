package com.svyatogor.appcaronaa3.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.svyatogor.appcaronaa3.R;

public class MainActivity extends AppCompatActivity {

    /*private EditText etOrigem1, etDestino1, etData, etnVagas;
    private Button btPublicarCarona;
    private EditText etOrigem2, etDestino2;
    private Button btBuscarCarona;
    private ImageView icUserMain;*/
    private Button btnEntrarMotorista;
    private Button btnEntrarPassageiro;

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

        //iniciarComponentes();
        //configurarBotoes();
        btnEntrarMotorista = findViewById(R.id.btn_entrar_motorista);
        btnEntrarPassageiro = findViewById(R.id.btn_entrar_passageiro);
        btnEntrarMotorista.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, TelaMotorista.class));
        });
        btnEntrarPassageiro.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, TelaPassageiro.class));
        });
    }

    /*private void iniciarComponentes() {
        // Motorista
        etOrigem1 = findViewById(R.id.et_origem1);
        etDestino1 = findViewById(R.id.et_destino1);
        etData = findViewById(R.id.et_data);
        etnVagas = findViewById(R.id.etn_vagas);
        btPublicarCarona = findViewById(R.id.bt_publicar_carona);

        // Passageiro
        etOrigem2 = findViewById(R.id.et_origem2);
        etDestino2 = findViewById(R.id.et_destino2);
        btBuscarCarona = findViewById(R.id.bt_buscar_carona);

        //Ícones
        icUserMain = findViewById(R.id.ic_user_main);
    }

    private void configurarBotoes() {

        btPublicarCarona.setOnClickListener(v -> {
            String origem = etOrigem1.getText().toString();
            String destino = etDestino1.getText().toString();
            String data = etData.getText().toString();
            String vagas = etnVagas.getText().toString();

            if (!origem.isEmpty() && !destino.isEmpty() && !data.isEmpty() && !vagas.isEmpty()) {
                Intent intent = new Intent(MainActivity.this, TelaMotorista.class);
                intent.putExtra("tipo_usuario", "motorista");
                intent.putExtra("origem", origem);
                intent.putExtra("destino", destino);
                intent.putExtra("data", data);
                intent.putExtra("vagas", vagas);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Preencha todos os campos!", Toast.LENGTH_SHORT).show();
            }
        });


        btBuscarCarona.setOnClickListener(v -> {
            String origem = etOrigem2.getText().toString();
            String destino = etDestino2.getText().toString();

            if (!origem.isEmpty() && !destino.isEmpty()) {
                Intent intent = new Intent(MainActivity.this, TelaPassageiro.class);
                intent.putExtra("tipo_usuario", "passageiro");
                intent.putExtra("origem", origem);
                intent.putExtra("destino", destino);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Preencha origem e destino!", Toast.LENGTH_SHORT).show();
            }
        });

        icUserMain.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PerfilUser.class);
            startActivity(intent);
        });
    }*/
}