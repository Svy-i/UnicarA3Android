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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import com.svyatogor.appcaronaa3.Model.Usuario;
import com.svyatogor.appcaronaa3.R;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class PerfilUser extends AppCompatActivity {
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private static final String KEY_PHOTO_PATH = "photo_path";
    private String currentPhotoPath;
    private TextView tvNomePerfil;
    private TextView tvEmailPerfil;
    private ImageView ivEditTelefone;
    private ImageView ivEditFoto;
    private ImageView ivFoto;
    private EditText etEditTelefone;

    private Usuario usuario;

    private DatabaseReference databaseReference; // Referência para o Realtime Database

    // Activity para captura de foto
    private final ActivityResultLauncher<Intent> capturarFoto = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    if (currentPhotoPath == null || currentPhotoPath.isEmpty()) {
                        Toast.makeText(this, "Erro: Caminho da foto não foi definido.", Toast.LENGTH_LONG).show();
                        Log.e("PerfilUser", "Erro: currentPhotoPath é nulo ou vazio após a captura.");
                        return;
                    }

                    Bitmap bitmap;
                    try {
                        bitmap = BitmapFactory.decodeFile(currentPhotoPath);
                    } catch (OutOfMemoryError e) {
                        Toast.makeText(this, "Erro: Imagem muito grande para carregar.", Toast.LENGTH_LONG).show();
                        Log.e("PerfilUser", "Falta de memória ao decodificar bitmap: " + e.getMessage(), e);
                        return;
                    }

                    if (bitmap == null) {
                        Toast.makeText(this, "Erro: Não foi possível decodificar a imagem capturada.", Toast.LENGTH_LONG).show();
                        Log.e("PerfilUser", "Erro: Bitmap decodificado é nulo.");
                        return;
                    }

                    Bitmap rotatedBitmap = rotacionarImagem(bitmap, currentPhotoPath);
                    ivFoto.setImageBitmap(rotatedBitmap);

                    uploadImageToCloudinary(new File(currentPhotoPath));

                } else {
                    Toast.makeText(PerfilUser.this, "Captura de imagem cancelada ou falhou.", Toast.LENGTH_SHORT).show();
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

        // Inicializa a referência para o Realtime Database
        databaseReference = FirebaseDatabase.getInstance().getReference();

        if (savedInstanceState != null) {
            currentPhotoPath = savedInstanceState.getString(KEY_PHOTO_PATH);
            if (currentPhotoPath != null && !currentPhotoPath.isEmpty()) {
                try {
                    Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath);
                    if (bitmap != null) {
                        ivFoto.setImageBitmap(rotacionarImagem(bitmap, currentPhotoPath));
                    }
                } catch (Exception e) {
                    Log.e("PerfilUser", "Erro ao carregar foto restaurada: " + e.getMessage());
                }
            }
        }

        usuario = new Usuario();
        carregarDadosDoUsuario();

        ivEditFoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ContextCompat.checkSelfPermission(PerfilUser.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(PerfilUser.this, new String[]{
                            Manifest.permission.CAMERA
                    }, CAMERA_PERMISSION_REQUEST_CODE);
                } else {
                    abrirCamera();
                }
            }
        });
    }

    private void carregarDadosDoUsuario() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Usuário não autenticado. Redirecionando para o login.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, TelaLogin.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        // Usa Realtime Database para carregar dados do usuário
        databaseReference.child("usuarios").child(currentUser.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            // Mapeia os dados do Realtime Database para o objeto Usuario
                            Usuario userFromDb = snapshot.getValue(Usuario.class);
                            if (userFromDb != null) {
                                usuario = userFromDb;
                                tvNomePerfil.setText(usuario.getNome());
                                tvEmailPerfil.setText(usuario.getEmail());
                                etEditTelefone.setText(usuario.getTelefone());

                                String fotoUrl = usuario.getFotoPerfilUrl();
                                if (fotoUrl != null && !fotoUrl.isEmpty()) {
                                    if (fotoUrl.startsWith("http://")) {
                                        fotoUrl = fotoUrl.replace("http://", "https://");
                                    }
                                    Glide.with(PerfilUser.this)
                                            .load(fotoUrl)
                                            .placeholder(R.drawable.ic_user)
                                            .error(R.drawable.ic_launcher_background)
                                            .into(ivFoto);
                                } else {
                                    ivFoto.setImageResource(R.drawable.ic_user);
                                    Log.w("PerfilUser", "URL da foto de perfil é nula ou vazia. Usando imagem padrão.");
                                }
                            } else {
                                Log.e("PerfilUser", "Erro ao converter snapshot para objeto Usuario. Objeto nulo.");
                                exibirDadosPadrao(currentUser.getEmail());
                            }
                        } else {
                            Log.d("PerfilUser", "Documento do usuário não encontrado no Realtime Database. Criando perfil básico.");
                            usuario = new Usuario();
                            usuario.setUid(currentUser.getUid());
                            usuario.setEmail(currentUser.getEmail());
                            usuario.setNome("Novo Usuário");
                            usuario.setTelefone("Não definido");

                            // Salva o perfil básico no Realtime Database
                            databaseReference.child("usuarios").child(currentUser.getUid()).setValue(usuario)
                                    .addOnSuccessListener(aVoid -> Log.d("PerfilUser", "Perfil básico criado no Realtime Database."))
                                    .addOnFailureListener(e -> Log.e("PerfilUser", "Erro ao criar perfil básico: " + e.getMessage()));

                            exibirDadosPadrao(currentUser.getEmail());
                        }
                        editarTelefone();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(PerfilUser.this, "Erro ao carregar dados do usuário: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("PerfilUser", "Erro ao carregar dados do usuário do Realtime Database", error.toException());
                        exibirDadosPadrao(currentUser.getEmail());
                    }
                });
    }

    private void exibirDadosPadrao(String email) {
        tvNomePerfil.setText("Nome não definido");
        tvEmailPerfil.setText(email);
        etEditTelefone.setText("Telefone não definido");
        ivFoto.setImageResource(R.drawable.ic_user);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (currentPhotoPath != null) {
            outState.putString(KEY_PHOTO_PATH, currentPhotoPath);
        }
    }

    private void iniciarComponentes() {
        tvNomePerfil = findViewById(R.id.tv_nome_perfil);
        tvEmailPerfil = findViewById(R.id.tv_email_perfil);
        etEditTelefone = findViewById(R.id.et_edit_telefone);
        ivEditFoto = findViewById(R.id.iv_edit_foto);
        ivEditTelefone = findViewById(R.id.iv_edit_telefone);
        ivFoto = findViewById(R.id.iv_foto);
    }

    private void abrirCamera() {
        Intent obterFotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (obterFotoIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = criarArquivoDeImagem();
            } catch (IOException ex) {
                Toast.makeText(this, "Erro ao criar o arquivo de imagem: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("PerfilUser", "Erro ao criar arquivo de imagem", ex);
                return;
            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "br.com.svyatogor.appcaronaa3.fileprovider",
                        photoFile);

                obterFotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                obterFotoIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                obterFotoIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                capturarFoto.launch(obterFotoIntent);
            }
        } else {
            Toast.makeText(this, "Nenhum aplicativo de câmera encontrado. Por favor, instale um.", Toast.LENGTH_LONG).show();
            Log.w("PerfilUser", "Nenhum aplicativo de câmera disponível para MediaStore.ACTION_IMAGE_CAPTURE.");
        }
    }

    private File criarArquivoDeImagem() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        currentPhotoPath = image.getAbsolutePath();
        Log.d("PerfilUser", "Arquivo de imagem temporário criado: " + currentPhotoPath);
        return image;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                abrirCamera();
            } else {
                Toast.makeText(this, "PERMISSÃO DE CÂMERA NEGADA! Não é possível capturar foto.", Toast.LENGTH_LONG).show();
                Log.w("PerfilUser", "Permissão de câmera negada pelo usuário.");
            }
        }
    }

    private Bitmap rotacionarImagem(Bitmap img, String photoPath) {
        ExifInterface exif;
        try {
            exif = new ExifInterface(photoPath);
        } catch (IOException e) {
            Log.e("PerfilUser", "Erro ao ler EXIF para rotação: " + e.getMessage(), e);
            return img;
        }

        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        Matrix matrix = new Matrix();
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

        try {
            Bitmap rotatedBitmap = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
            img.recycle();
            return rotatedBitmap;
        } catch (OutOfMemoryError e) {
            Log.e("PerfilUser", "OutOfMemoryError ao rotacionar bitmap: " + e.getMessage());
            Toast.makeText(this, "Erro de memória ao rotacionar imagem.", Toast.LENGTH_SHORT).show();
            return img;
        }
    }

    public void editarTelefone() {
        final boolean[] editandoTelefone = {etEditTelefone.isFocusable()};

        ivEditTelefone.setOnClickListener(v -> {
            if (editandoTelefone[0]) {
                String novoTelefone = etEditTelefone.getText().toString().trim();
                if (!novoTelefone.isEmpty()) {
                    salvarTelefone(novoTelefone);
                } else {
                    Toast.makeText(this, "Telefone não pode ser vazio.", Toast.LENGTH_SHORT).show();
                }

                etEditTelefone.setFocusable(false);
                etEditTelefone.setFocusableInTouchMode(false);
                etEditTelefone.setClickable(false);
                etEditTelefone.setCursorVisible(false);
                etEditTelefone.setKeyListener(null);
            } else {
                etEditTelefone.setFocusable(true);
                etEditTelefone.setFocusableInTouchMode(true);
                etEditTelefone.setClickable(true);
                etEditTelefone.setCursorVisible(true);
                etEditTelefone.setKeyListener(new EditText(this).getKeyListener());
                etEditTelefone.requestFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(etEditTelefone, InputMethodManager.SHOW_IMPLICIT);
                }
            }
            editandoTelefone[0] = !editandoTelefone[0];
        });

        etEditTelefone.setFocusable(false);
        etEditTelefone.setFocusableInTouchMode(false);
        etEditTelefone.setClickable(false);
        etEditTelefone.setCursorVisible(false);
        etEditTelefone.setKeyListener(null);
    }

    private void salvarTelefone(String novoTelefone) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("telefone", novoTelefone);

            databaseReference.child("usuarios").child(user.getUid()).updateChildren(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Telefone atualizado com sucesso!", Toast.LENGTH_SHORT).show();
                        if (usuario != null) {
                            usuario.setTelefone(novoTelefone);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Falha ao atualizar telefone: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("PerfilUser", "Erro ao atualizar telefone no Realtime Database", e);
                    });
        }
    }

    private void uploadImageToCloudinary(File imageFile) {
        if (MediaManager.get().getCloudinary() == null || MediaManager.get().getCloudinary().config == null) {
            Toast.makeText(this, "Erro: Cloudinary não inicializado ou configuração ausente. Verifique a classe Application.", Toast.LENGTH_LONG).show();
            Log.e("PerfilUser", "MediaManager.get().getCloudinary() ou sua configuração é nula. Cloudinary não inicializado corretamente.");
            return;
        }

        MediaManager.get().upload(Uri.fromFile(imageFile))
                .unsigned("meu_preset")
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {
                        Toast.makeText(PerfilUser.this, "Iniciando upload da imagem...", Toast.LENGTH_SHORT).show();
                        Log.d("PerfilUser", "Upload Cloudinary iniciado para requestId: " + requestId);
                    }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {
                        Log.d("PerfilUser", "Progresso do upload (" + requestId + "): " + bytes + "/" + totalBytes);
                    }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String imageUrl = (String) resultData.get("url");
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            // NOVO: Assegura que a URL da foto seja HTTPS antes de salvar e usar
                            if (imageUrl.startsWith("http://")) {
                                imageUrl = imageUrl.replace("http://", "https://");
                            }
                            saveImageUrlToRealtimeDatabase(imageUrl);
                            Log.d("PerfilUser", "Upload Cloudinary sucesso. URL: " + imageUrl);
                        } else {
                            Toast.makeText(PerfilUser.this, "Erro: URL da imagem não retornada pelo Cloudinary.", Toast.LENGTH_LONG).show();
                            Log.e("PerfilUser", "URL da imagem é nula ou vazia no sucesso do upload para requestId: " + requestId);
                        }
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        Toast.makeText(PerfilUser.this, "Erro no upload da imagem: " + error.getDescription(), Toast.LENGTH_LONG).show();
                        Log.e("PerfilUser", "Erro Cloudinary no upload para requestId " + requestId + ": " + error.getDescription());
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {
                        Log.w("PerfilUser", "Upload reagendado para requestId " + requestId + ": " + error.getDescription());
                    }
                })
                .dispatch();
    }

    private void saveImageUrlToRealtimeDatabase(String imageUrl) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("fotoPerfilUrl", imageUrl);

            databaseReference.child("usuarios").child(user.getUid()).updateChildren(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Imagem de perfil salva com sucesso!", Toast.LENGTH_SHORT).show();
                        Log.d("PerfilUser", "URL da foto de perfil salva no Realtime Database: " + imageUrl);
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(imageUrl)
                                    .placeholder(R.drawable.ic_user)
                                    .error(R.drawable.ic_launcher_background)
                                    .into(ivFoto);
                        }
                        if (usuario != null) {
                            usuario.setFotoPerfilUrl(imageUrl);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Falha ao salvar imagem de perfil: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("PerfilUser", "Erro ao salvar URL da foto de perfil no Realtime Database", e);
                    });
        }
    }
}
