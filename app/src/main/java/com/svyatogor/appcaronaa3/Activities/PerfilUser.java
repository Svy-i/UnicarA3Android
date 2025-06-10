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
import android.util.Log;
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

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.svyatogor.appcaronaa3.R;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class PerfilUser extends AppCompatActivity {

    Cloudinary cloudinary = new Cloudinary();
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private static final String KEY_PHOTO_PATH = "photo_path"; // Adicione esta constante
    private String currentPhotoPath;
    private TextView tvNomePerfil;
    private TextView tvEmailPerfil;
    private ImageView ivEditTelefone;
    private ImageView ivEditFoto;
    private ImageView ivFoto;


    // Definindo o ActivityResultLauncher para capturar fotos
    private final ActivityResultLauncher<Intent> capturarFoto = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {

                if (result.getResultCode() == RESULT_OK) {
                    // **1. Verifique se currentPhotoPath não é nulo ANTES de usar**
                    if (currentPhotoPath == null || currentPhotoPath.isEmpty()) {
                        Toast.makeText(this, "Erro: Caminho da foto não foi definido.", Toast.LENGTH_LONG).show();
                        return; // Sair do método se o caminho for nulo
                    }

                    // **2. Tente decodificar o Bitmap**
                    Bitmap bitmap = null;
                    try {
                        bitmap = BitmapFactory.decodeFile(currentPhotoPath);
                    } catch (OutOfMemoryError e) {
                        // Lidar com OutOfMemoryError para imagens muito grandes
                        Toast.makeText(this, "Erro: Imagem muito grande para carregar.", Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                        return; // Sair do método
                    }


                    // **3. Verifique se o Bitmap foi decodificado com sucesso**
                    if (bitmap == null) {
                        Toast.makeText(this, "Erro: Não foi possível decodificar a imagem capturada.", Toast.LENGTH_LONG).show();
                        // Opcional: tentar carregar de outra forma ou informar ao usuário
                        return; // Sair do método se o bitmap for nulo
                    }

                    // **4. Corrigir a rotação da imagem e exibi-la**
                    // Chame rotacionarImagem APENAS se o bitmap e currentPhotoPath forem válidos
                    Bitmap rotatedBitmap = rotacionarImagem(bitmap, currentPhotoPath);
                    ivFoto.setImageBitmap(rotatedBitmap);

                    // **5. Faça o upload para o Cloudinary (apenas se tudo acima deu certo)**
                    uploadImageToCloudinary(new File(currentPhotoPath));

                } else {
                    // Usuário cancelou a câmera ou ocorreu um erro na câmera
                    Toast.makeText(PerfilUser.this, "Captura de imagem cancelada ou falhou.", Toast.LENGTH_SHORT).show();
                    // É crucial limpar currentPhotoPath aqui para evitar usar um caminho inválido depois
                    currentPhotoPath = null;
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

        // Adicione esta linha para carregar a imagem do perfil
        // Obtenha o ID do usuário logado para carregar a imagem dele
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            loadUserProfileImage(currentUser.getUid());
        }


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
        if (savedInstanceState != null) {
            currentPhotoPath = savedInstanceState.getString(KEY_PHOTO_PATH);
        }
        carregarDadosDoUsuario();
    } // fim do onCreate()

    private void carregarDadosDoUsuario() {
        // Exemplo: pegando o usuário logado ou passando via Intent
        // FirebaseAuth.getInstance().getCurrentUser().getPhotoUrl()
        // Ou de um objeto Usuario que você carregou do Firestore:
        // Usuario usuarioAtual = ... // Carregue seu objeto Usuario

        String fotoUrl = null; // Inicialize como null

        // *** PONTO CRÍTICO: Verifique de onde vem a URL da foto ***
        // Se for do Firebase Auth:
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            if (FirebaseAuth.getInstance().getCurrentUser().getPhotoUrl() != null) {
                fotoUrl = FirebaseAuth.getInstance().getCurrentUser().getPhotoUrl().toString();
            }
        }
        // Ou se for do seu objeto Usuario do Firestore:
        // if (usuarioAtual != null && usuarioAtual.getFotoPerfilUrl() != null) {
        //     fotoUrl = usuarioAtual.getFotoPerfilUrl();
        // }


        // *** AQUI É ONDE O ERRO PROVAVELMENTE ACONTECE SEM A VERIFICAÇÃO ***
        if (fotoUrl != null && !fotoUrl.isEmpty()) {
            // Se você usa Glide:
            Glide.with(this)
                    .load(fotoUrl)
                    .placeholder(R.drawable.ic_user) // Imagem padrão enquanto carrega
                    .error(R.drawable.ic_launcher_background) // Imagem se houver erro
                    .into(ivFoto);
        } else {
            // Se a URL for nula ou vazia, defina uma imagem padrão
            ivFoto.setImageResource(R.drawable.ic_user);
            Log.w("PerfilUser", "URL da foto de perfil é nula ou vazia. Usando imagem padrão.");
        }
    }

    // Adicione este método à sua classe PerfilUser
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Salva o caminho da foto antes que a Activity seja destruída
        if (currentPhotoPath != null) {
            outState.putString("photo_path", currentPhotoPath); // Use a mesma KEY_PHOTO_PATH
        }
    }

    private void iniciarComponentes() {
        tvNomePerfil = findViewById(R.id.tv_nome_perfil);
        tvEmailPerfil = findViewById(R.id.tv_email_perfil);
        ivEditFoto = findViewById(R.id.iv_edit_foto);
        ivEditTelefone = findViewById(R.id.iv_edit_telefone);
        ivFoto = findViewById(R.id.iv_foto);
    }

    private void abrirCamera() {
        Intent obterFotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Verifica se há uma atividade de câmera disponível
        if (obterFotoIntent.resolveActivity(getPackageManager()) != null) {
            // Cria um arquivo temporário para armazenar a imagem
            File photoFile = null;
            try {
                photoFile = criarArquivoDeImagem();
            } catch (IOException ex) {
                Toast.makeText(this, "Erro ao criar o arquivo de imagem", Toast.LENGTH_SHORT).show();
                return; // Sai do método se não conseguir criar o arquivo
            }
            // Prossegue se o arquivo foi criado com sucesso
            if (photoFile != null) {
                // Aqui está a chamada ao FileProvider com a URI correta
                Uri photoURI = FileProvider.getUriForFile(this,
                        "br.com.svyatogor.appcaronaa3.fileprovider",  // Use o nome correto do pacote
                        photoFile);

                obterFotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                obterFotoIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                obterFotoIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                capturarFoto.launch(obterFotoIntent);
            }
        }
    }

    // Criar arquivo temporário para salvar a foto
    private File criarArquivoDeImagem() throws IOException {
        // Cria um nome de arquivo único baseado na data
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  // Nome do arquivo
                ".jpg",         // Extensão
                storageDir      // Diretório de armazenamento
        );
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

    private void uploadImageToCloudinary(File imageFile) {
        MediaManager.get().upload(Uri.fromFile(imageFile))
                .unsigned("meu_preset")
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {
                        // Iniciar animação de carregamento
                    }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {
                        // Atualizar barra de progresso
                    }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String imageUrl = (String) resultData.get("url");
                        // Salvar URL no Firestore
                        saveImageUrlToFirestore(imageUrl);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        // Exibir mensagem de erro
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {
                        // Lidar com reenvios
                    }
                })
                .dispatch();
    }

    private void saveImageUrlToFirestore(String imageUrl) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("usuarios").document(user.getUid())
                    .update("fotoPerfil", imageUrl)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Imagem salva com sucesso!", Toast.LENGTH_SHORT).show();
                        // Recarrega a imagem na ImageView com a URL que acabou de ser salva
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(imageUrl)
                                    .into(ivFoto);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Falha ao salvar imagem de perfil: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void loadUserProfileImage(String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("usuarios").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String imageUrl = documentSnapshot.getString("fotoPerfil");
                        if (imageUrl != null && !imageUrl.isEmpty()) { // Adicione uma verificação de null e vazio
                            Glide.with(this)
                                    .load(imageUrl)
                                    .into(ivFoto);
                        } else {
                            // Opcional: Se não houver imagem, carregar uma imagem padrão
                            ivFoto.setImageResource(R.drawable.ic_user);
                        }
                    } else {
                        // Opcional: Se o documento do usuário não existir, carregar uma imagem padrão
                        ivFoto.setImageResource(R.drawable.ic_user);
                    }
                })
                .addOnFailureListener(e -> {
                    // Exibir mensagem de erro ou carregar imagem padrão em caso de falha
                    Toast.makeText(this, "Erro ao carregar imagem de perfil: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    ivFoto.setImageResource(R.drawable.ic_user); // Carregar imagem padrão
                });
    }
}