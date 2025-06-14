package com.svyatogor.appcaronaa3.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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
import com.google.firebase.auth.FirebaseUser;
import com.svyatogor.appcaronaa3.R;

public class TelaLogin extends AppCompatActivity {

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private TextView tvCadastro;
    private TextView tvEsqueciSenha;
    private Button btEntrar;
    private EditText etEmail;
    private EditText etSenha;
    private ImageView icSenhaVisibilidade;
    private boolean senhaVisivel = false;

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
        mudarSenhaVisibilidade();

        // Clicar no texto "Criar Conta" leva para a tela de cadastro
        tvCadastro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TelaLogin.this, TelaCadastro.class);
                startActivity(intent);
            }
        });
        // Clicar no texto "Esqueci minha senha" leva para a activity de RecuperarSenha
        tvEsqueciSenha.setOnClickListener(v -> {
            startActivity(new Intent(this, RecuperarSenha.class));
        });
    }

    private void inicializarComponentes(){
        tvCadastro = findViewById(R.id.tv_cadastro);
        tvEsqueciSenha = findViewById(R.id.tv_esqueci_senha);
        btEntrar = findViewById(R.id.btn_entrar);
        etEmail = findViewById(R.id.et_email);
        etSenha = findViewById(R.id.et_senha);
        icSenhaVisibilidade = findViewById(R.id.ic_password_toggle);
    }

    // Icone do olho muda a visibilidade da senha
    private void mudarSenhaVisibilidade() {
        icSenhaVisibilidade.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int selection = etSenha.getSelectionEnd();

                if (senhaVisivel) {
                    etSenha.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    icSenhaVisibilidade.setImageResource(R.drawable.ic_eye);
                    senhaVisivel = false;
                } else {
                    etSenha.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    icSenhaVisibilidade.setImageResource(R.drawable.ic_eye_off);
                    senhaVisivel = true;
                }
                etSenha.setSelection(selection);
            }
        });
    }

    // Clicar no botão de entrar avança para a Main Activity se tudo estiver correto
    private void entrar(){
        btEntrar.setOnClickListener(v -> {
            String emailEntrar = etEmail.getText().toString().trim();
            String senhaEntrar = etSenha.getText().toString();

            if (emailEntrar.isEmpty() || senhaEntrar.isEmpty()){
                Toast.makeText(this, "Preencha todos os campos!", Toast.LENGTH_SHORT).show();
            } else if (!emailEntrar.endsWith("@ulife.com.br")) { // Vê se termina em @ulife
                Toast.makeText(this, "Por favor, use um e-mail @ulife.com.br para entrar.", Toast.LENGTH_LONG).show();
            }
            else {
                auth.signInWithEmailAndPassword(emailEntrar, senhaEntrar).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()){
                            FirebaseUser user = auth.getCurrentUser();
                            if (user != null && user.isEmailVerified()) { // Verifica se o e-mail foi verificado
                                Toast.makeText(TelaLogin.this, "Seja Bem Vindo!", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(TelaLogin.this, MainActivity.class));
                                finish();
                            } else {
                                auth.signOut(); // Desloga o usuário se o e-mail não estiver verificado
                                Toast.makeText(TelaLogin.this, "Por favor, verifique seu e-mail antes de fazer login.", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            // Trate erros de login específicos, como "usuário não encontrado" ou "senha incorreta"
                            String errorMessage = "Email ou Senha errados.";
                            if (task.getException() instanceof com.google.firebase.auth.FirebaseAuthInvalidUserException) {
                                errorMessage = "Usuário não encontrado. Verifique seu e-mail.";
                            } else if (task.getException() instanceof com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
                                errorMessage = "Senha incorreta.";
                            }
                            Toast.makeText(TelaLogin.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });
    }
}