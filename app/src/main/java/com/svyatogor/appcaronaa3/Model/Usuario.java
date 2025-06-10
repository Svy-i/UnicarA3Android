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
    private String pontoDeChegada;
    private String destino;
    private boolean isLookingForRide;

    public Usuario() {
    }

    //Construtor completo
    public Usuario(String uid, String nome, String email, String telefone, String fotoPerfilUrl,
                   String pontoDeChegada, String destino, boolean isLookingForRide) {
        this.uid = uid;
        this.nome = nome;
        this.email = email;
        this.telefone = telefone;
        this.fotoPerfilUrl = fotoPerfilUrl;
        this.pontoDeChegada = pontoDeChegada;
        this.destino = destino;
        this.isLookingForRide = isLookingForRide;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public void setUid(String uid) { this.uid = uid; }

    public void setFotoPerfilUrl(String fotoPerfilUrl) {
        this.fotoPerfilUrl = fotoPerfilUrl;
    }

    public void setPontoDeChegada(String pontoDeChegada) {
        this.pontoDeChegada = pontoDeChegada;
    }

    public void setDestino(String destino) {
        this.destino = destino;
    }

    public void setLookingForRide(boolean lookingForRide) {
        isLookingForRide = lookingForRide;
    }

    public String getEmail() {
        return email;
    }

    public String getTelefone() {
        return telefone;
    }

    public String getUid() {
        return uid;
    }
    public String getNome() {
        return nome;
    }
    public String getFotoPerfilUrl() {
        return fotoPerfilUrl;
    }

    public String getPontoDeChegada() {
        return pontoDeChegada;
    }

    public String getDestino() {
        return destino;
    }

    public boolean isLookingForRide() {
        return isLookingForRide;
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
