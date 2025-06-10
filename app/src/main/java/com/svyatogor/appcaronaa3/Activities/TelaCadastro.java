package com.svyatogor.appcaronaa3.Activities;

import android.content.Intent;
import android.os.Bundle;
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
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.svyatogor.appcaronaa3.Model.Usuario;
import com.svyatogor.appcaronaa3.R;

import java.util.HashMap;
import java.util.Map;

public class TelaCadastro extends AppCompatActivity {
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private Button btCadastro;
    private EditText etNomeCadastro;
    private EditText etEmailCadastro;
    private EditText etSenhaCadastro;
    private EditText etTelefoneCadastro;

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
        Cadastrar();
    } // fim do onCreate
    //Inicia as variáveis
    private void iniciarComponentes(){
        btCadastro = findViewById(R.id.bt_cadastro);
        etNomeCadastro = findViewById(R.id.et_nome_cadastro);
        etEmailCadastro = findViewById(R.id.et_email_cadastro);
        etSenhaCadastro = findViewById(R.id.et_senha_cadastro);
        etTelefoneCadastro = findViewById(R.id.et_telefone_cadastro);
    }
    //Botão para cadastrar as informações de um usuário no banco de dados
    private void Cadastrar() {
        btCadastro.setOnClickListener(v -> {
            Usuario usuario = new Usuario();
            usuario.setNome(etNomeCadastro.getText().toString());
            usuario.setEmail(etEmailCadastro.getText().toString());
            String senhaCadastro = etSenhaCadastro.getText().toString();
            usuario.setTelefone(etTelefoneCadastro.getText().toString());

           if (usuario.getNome().isEmpty() || usuario.getEmail().isEmpty() || senhaCadastro.isEmpty() || usuario.getTelefone().isEmpty()){
               Toast.makeText(this, "Preencha todos os campos!", Toast.LENGTH_SHORT).show();
           } else {
                auth.createUserWithEmailAndPassword(usuario.getEmail(), senhaCadastro).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                   @Override
                   public void onComplete(@NonNull Task<AuthResult> task) {
                       if (task.isSuccessful()) {
                           //Usuario é criado com sucesso
                           FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                           assert user != null;
                           usuario.setUid(auth.getUid());
                           usuario.salvar();
                           Toast.makeText(TelaCadastro.this, "Cadastro feito com sucesso!", Toast.LENGTH_SHORT).show();
                           //Apaga as caixas de texto dos EditTexts e leva para a activity de login

                           etNomeCadastro.setText("");
                           etEmailCadastro.setText("");
                           etSenhaCadastro.setText("");
                           etTelefoneCadastro.setText("");
                           startActivity(new Intent(TelaCadastro.this, TelaLogin.class));
                       } else {
                           Exception exception = task.getException();

                           if (exception instanceof FirebaseAuthWeakPasswordException) {
                               Toast.makeText(TelaCadastro.this, "Senha precisa de mais de 6 caractéres", Toast.LENGTH_SHORT).show();
                           } else if (exception instanceof FirebaseAuthInvalidCredentialsException) {
                               Toast.makeText(TelaCadastro.this, "Email inválido", Toast.LENGTH_SHORT).show();
                           } else if (exception instanceof FirebaseAuthUserCollisionException) {
                               Toast.makeText(TelaCadastro.this, "Este e-mail já está em uso", Toast.LENGTH_SHORT).show();
                           }else if(exception instanceof FirebaseNetworkException){
                               Toast.makeText(TelaCadastro.this, "Sem conexão a internet", Toast.LENGTH_SHORT).show();
                           }else {
                               assert exception != null;
                               Toast.makeText(TelaCadastro.this, "Erro: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                           }
                       }
                   }
               });
           }
        });
    }
}
