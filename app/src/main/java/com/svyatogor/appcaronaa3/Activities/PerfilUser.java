package com.svyatogor.appcaronaa3.Activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.svyatogor.appcaronaa3.R;

import java.io.IOException;

public class PerfilUser extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;
    private TextView etNomePerfil;
    private TextView etEmailPerfil;
    private EditText etTelefone;
    private View vEditFoto;
    private View vEditTelefone;
    private ImageView vContainerUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_perfil_user);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        iniciarComponentes();
        editarFoto();
        editarTelefone();
    } // fim do onCreate

    private void iniciarComponentes(){
        etNomePerfil = findViewById(R.id.et_nome_perfil);
        etEmailPerfil = findViewById(R.id.et_email_perfil);
        vEditFoto = findViewById(R.id.v_edit_foto);
        vEditTelefone = findViewById(R.id.v_edit_telefone);
    }

    // Launcher para o Photo Picker
    ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    vContainerUser.setImageURI(imageUri);
                    Toast.makeText(this, "Imagem selecionada", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Nenhuma imagem selecionada", Toast.LENGTH_SHORT).show();
                }
            });

    public void editarFoto(){
        vEditFoto.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            pickImageLauncher.launch(intent);
        });
    }

    public void editarTelefone() {
        // Variável para controlar o estado de edição
        final boolean[] editandoTelefone = {false};

        vEditTelefone.setOnClickListener(v -> {
            if (editandoTelefone[0]) {
                // Desabilita a edição
                etNomePerfil.setFocusable(false);
                etNomePerfil.setFocusableInTouchMode(false);
                etNomePerfil.setClickable(false);
                etNomePerfil.setCursorVisible(false);
                etNomePerfil.setKeyListener(null);
            } else {
                // Habilita a edição
                etNomePerfil.setFocusable(true);
                etNomePerfil.setFocusableInTouchMode(true);
                etNomePerfil.setClickable(true);
                etNomePerfil.setCursorVisible(true);
                etNomePerfil.setKeyListener(new EditText(this).getKeyListener());
                etNomePerfil.requestFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(etNomePerfil, InputMethodManager.SHOW_IMPLICIT);
                }
            }
            // Alterna o estado
            editandoTelefone[0] = !editandoTelefone[0];
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                vContainerUser.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
}