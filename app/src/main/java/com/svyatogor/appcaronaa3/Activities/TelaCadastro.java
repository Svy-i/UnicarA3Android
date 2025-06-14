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
    private void iniciarComponentes(){
        btCadastro = findViewById(R.id.bt_cadastro);
        etNomeCadastro = findViewById(R.id.et_nome_cadastro);
        etEmailCadastro = findViewById(R.id.et_email_cadastro);
        etSenhaCadastro = findViewById(R.id.et_senha_cadastro);
        etTelefoneCadastro = findViewById(R.id.et_telefone_cadastro);
    }
    private void Cadastrar() {
        btCadastro.setOnClickListener(v -> {
            Usuario usuario = new Usuario();
            usuario.setNome(etNomeCadastro.getText().toString().trim());
            usuario.setEmail(etEmailCadastro.getText().toString().trim());
            String senhaCadastro = etSenhaCadastro.getText().toString();
            usuario.setTelefone(etTelefoneCadastro.getText().toString().trim());

            if (usuario.getNome().isEmpty() || usuario.getEmail().isEmpty() || senhaCadastro.isEmpty() || usuario.getTelefone().isEmpty()){
                Toast.makeText(this, "Preencha todos os campos!", Toast.LENGTH_SHORT).show();
            } else if (!usuario.getEmail().endsWith("@ulife.com.br")) { // Validação do domínio
                Toast.makeText(this, "Por favor, use um e-mail @ulife.com.br para o cadastro.", Toast.LENGTH_LONG).show();
            }
            else {
                auth.createUserWithEmailAndPassword(usuario.getEmail(), senhaCadastro).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Usuário é criado com sucesso
                            FirebaseUser user = auth.getCurrentUser();
                            assert user != null;

                            // Envia e-mail de verificação
                            user.sendEmailVerification()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> emailTask) {
                                            if (emailTask.isSuccessful()) {
                                                Toast.makeText(TelaCadastro.this, "Cadastro feito com sucesso! Um e-mail de verificação foi enviado para " + user.getEmail(), Toast.LENGTH_LONG).show();
                                                usuario.setUid(user.getUid());
                                                usuario.salvar();
                                                etNomeCadastro.setText("");
                                                etEmailCadastro.setText("");
                                                etSenhaCadastro.setText("");
                                                etTelefoneCadastro.setText("");
                                                startActivity(new Intent(TelaCadastro.this, TelaLogin.class));
                                                finish(); // Finaliza a atividade de cadastro
                                            } else {
                                                Toast.makeText(TelaCadastro.this, "Cadastro feito, mas falha ao enviar e-mail de verificação. Erro: " + emailTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                                                user.delete();
                                            }
                                        }
                                    });
                        } else {
                            Exception exception = task.getException();

                            if (exception instanceof FirebaseAuthWeakPasswordException) {
                                Toast.makeText(TelaCadastro.this, "Senha precisa de mais de 6 caracteres", Toast.LENGTH_SHORT).show();
                            } else if (exception instanceof FirebaseAuthInvalidCredentialsException) {
                                Toast.makeText(TelaCadastro.this, "E-mail inválido ou mal formatado", Toast.LENGTH_SHORT).show();
                            } else if (exception instanceof FirebaseAuthUserCollisionException) {
                                Toast.makeText(TelaCadastro.this, "Este e-mail já está em uso", Toast.LENGTH_SHORT).show();
                            }else if(exception instanceof FirebaseNetworkException){
                                Toast.makeText(TelaCadastro.this, "Sem conexão a internet", Toast.LENGTH_SHORT).show();
                            }else {
                                assert exception != null;
                                Toast.makeText(TelaCadastro.this, "Erro ao cadastrar: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
            }
        });
    }
}