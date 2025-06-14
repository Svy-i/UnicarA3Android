package com.svyatogor.appcaronaa3.Model;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class Usuario {
    private String uid;
    private String nome;
    private String email;
    private String telefone;
    private String fotoPerfilUrl;

    // Campos para a funcionalidade da carona pelo lado do Passageiro
    private String origem;
    private String destino;
    private String requestId;
    private boolean isLookingForRide;

    // Campos para a funcionalidade do Motorista
    private boolean isDriver; // true if the user is a driver, false if a passenger
    private Carro carro; // Car details for drivers

    public Usuario() {
        // Default constructor required for Firebase DataSnapshot.getValue(Usuario.class)
    }

    // Construtor completo
    public Usuario(String uid, String nome, String email, String telefone, String fotoPerfilUrl,
                   String origem, String destino, boolean isLookingForRide, boolean isDriver, Carro carro) {
        this.uid = uid;
        this.nome = nome;
        this.email = email;
        this.telefone = telefone;
        this.fotoPerfilUrl = fotoPerfilUrl;
        this.origem = origem;
        this.destino = destino;
        this.isLookingForRide = isLookingForRide;
        this.isDriver = isDriver;
        this.carro = carro;
    }

    // Getters
    public String getUid() { return uid; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getTelefone() { return telefone; }
    public String getFotoPerfilUrl() { return fotoPerfilUrl; }
    public String getOrigem() { return origem; }
    public String getDestino() { return destino; }
    public boolean isLookingForRide() { return isLookingForRide; }
    public boolean isDriver() { return isDriver; }
    public Carro getCarro() { return carro; }

    public String getRequestId() {
        return requestId;
    }

    // Setters
    public void setUid(String uid) { this.uid = uid; }
    public void setNome(String nome) { this.nome = nome; }
    public void setEmail(String email) { this.email = email; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public void setFotoPerfilUrl(String fotoPerfilUrl) { this.fotoPerfilUrl = fotoPerfilUrl; }
    public void setOrigem(String origem) { this.origem = origem; }
    public void setDestino(String destino) { this.destino = destino; }
    public void setLookingForRide(boolean lookingForRide) { isLookingForRide = lookingForRide; }
    public void setDriver(boolean driver) { isDriver = driver; }
    public void setCarro(Carro carro) { this.carro = carro; }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    @Override
    public String toString() {
        return nome; // Para exibir na lista
    }

    public void salvar(){
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        reference.child("usuarios").child(getUid()).setValue(this);
    }
}
