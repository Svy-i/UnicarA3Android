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

public class TelaCadastro extends AppCompatActivity {
    ConexaoBD conexaoBD = new ConexaoBD();
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
            String nomeUsuario = etNomeCadastro.getText().toString();
            String emailUsuario = etEmailCadastro.getText().toString();
            String senhaUsuario = etSenhaCadastro.getText().toString();

            String sql = "INSERT INTO usuario (nomeUsuario, emailUsuario, senhaUsuario) " +
                    "VALUES ('" + nomeUsuario + "', '" + emailUsuario + "', '" + senhaUsuario + "');";

            boolean salvo = ConexaoBD.salvar(sql);
            if (salvo){
                System.out.println("\nUsuário cadastrado com sucesso!\n");
            } else {
                System.out.println("\nErro ao cadastrar o usuário!\n");
            }
            startActivity(new Intent(this, TelaLogin.class));
        });
    }
}