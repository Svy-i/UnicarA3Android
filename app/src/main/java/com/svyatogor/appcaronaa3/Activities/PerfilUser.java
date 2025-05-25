package com.svyatogor.appcaronaa3.Activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.svyatogor.appcaronaa3.R;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PerfilUser extends AppCompatActivity {
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private String currentPhotoPath;
    private TextView tvNomePerfil;
    private TextView tvEmailPerfil;
    private ImageView ivEditTelefone;
    private ImageView ivEditFoto;
    private ImageView icUser;


    // Definindo o ActivityResultLauncher para capturar fotos
    private final ActivityResultLauncher<Intent> capturarFoto = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {

                if (result.getResultCode() == RESULT_OK) {
                    // Corrigir a rotação da imagem e exibi-la
                    Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath);
                    Bitmap rotatedBitmap = rotacionarImagem(bitmap, currentPhotoPath);
                    icUser.setImageBitmap(rotatedBitmap);
                }
            }
    );

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
        editarTelefone();

        // Ao clicar na ImageView, a câmera é aberta
        ivEditFoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ContextCompat.checkSelfPermission(PerfilUser.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){

                    // Se a permissão de câmera ou de gravação não foi concedida, solicite-as
                    ActivityCompat.requestPermissions(PerfilUser.this, new String[]{
                            Manifest.permission.CAMERA
                    }, CAMERA_PERMISSION_REQUEST_CODE);
                } else {
                    // Se as permissões já foram concedidas, abra a câmera
                    abrirCamera();
                }
            }
        });
    } // fim do onCreate()

    private void iniciarComponentes() {
        tvNomePerfil = findViewById(R.id.tv_nome_perfil);
        tvEmailPerfil = findViewById(R.id.tv_email_perfil);
        ivEditFoto = findViewById(R.id.iv_edit_foto);
        ivEditTelefone = findViewById(R.id.iv_edit_telefone);
        icUser = findViewById(R.id.ic_user);
    }

    private void abrirCamera() {
        Intent obterFotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Verifica se há uma atividade de câmera disponível
        if (obterFotoIntent.resolveActivity(getPackageManager()) != null) {
            // Cria um arquivo temporário para armazenar a imagem
            File photoFile = null;
            try {
                photoFile = criarArquivoDeImagem();
            } catch (Exception ex) {
                Toast.makeText(this, "Erro ao criar o arquivo de imagem", Toast.LENGTH_SHORT).show();
                return; // Sai do método se não conseguir criar o arquivo
            }
            // Prossegue se o arquivo foi criado com sucesso
            if (photoFile != null) {
                // Aqui está a chamada ao FileProvider com a URI correta
                Uri photoURI = FileProvider.getUriForFile(this,
                        "br.com.svyatogor.appcaronaa3.fileprovider",  // Use o nome correto do pacote
                        photoFile);


                obterFotoIntent.putExtra(MediaStore.EXTRA_OUTPUT,photoURI);
                capturarFoto.launch(obterFotoIntent);
            }
        }
    }

    // Criar arquivo temporário para salvar a foto
    private File criarArquivoDeImagem() throws IOException{
        // Cria um nome de arquivo único baseado na data
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  // Nome do arquivo
                ".jpg",         // Extensão
                storageDir      // Diretório de armazenamento
        );

        // Salva o caminho do arquivo para uso posterior
        currentPhotoPath = image.getAbsolutePath();
        return image;

    }

    // Tratar a resposta da solicitação de permissão
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permissão concedida, abrir câmera
                abrirCamera();
            } else {
                // Permissão negada
                Toast.makeText(this, "PERMISSÃO DE CÂMERA NEGADA!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Exibindo a imagem capturada
    private Bitmap rotacionarImagem(Bitmap img, String photoPath) {

        ExifInterface exif; // ExifInterface captura os metadados da foto. É possível saber a orientação por ele.
        try {
            exif = new ExifInterface(photoPath);
        } catch (IOException e) {
            e.printStackTrace();
            return img;
        }

        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        Matrix matrix = new Matrix(); // Matrix permite rotacionar a foto.
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270);
                break;
            default:
                return img;
        }

        return Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
    }

    public void editarTelefone() {
        // Variável para controlar o estado de edição
        final boolean[] editandoTelefone = {false};

        ivEditTelefone.setOnClickListener(v -> {
            if (editandoTelefone[0]) {
                // Desabilita a edição
                tvNomePerfil.setFocusable(false);
                tvNomePerfil.setFocusableInTouchMode(false);
                tvNomePerfil.setClickable(false);
                tvNomePerfil.setCursorVisible(false);
                tvNomePerfil.setKeyListener(null);
            } else {
                // Habilita a edição
                tvNomePerfil.setFocusable(true);
                tvNomePerfil.setFocusableInTouchMode(true);
                tvNomePerfil.setClickable(true);
                tvNomePerfil.setCursorVisible(true);
                tvNomePerfil.setKeyListener(new EditText(this).getKeyListener());
                tvNomePerfil.requestFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(tvNomePerfil, InputMethodManager.SHOW_IMPLICIT);
                }
            }
            // Alterna o estado
            editandoTelefone[0] = !editandoTelefone[0];
        });
    }
}