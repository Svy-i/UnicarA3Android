package com.svyatogor.appcaronaa3.Model;

public class Carro {
    private String modelo;
    private String placa;
    private String cor;
    private int ano;

    public Carro() {
    }

    //Construtor completo
    public Carro(String modelo, String placa, String cor, int ano) {
        this.modelo = modelo;
        this.placa = placa;
        this.cor = cor;
        this.ano = ano;
    }

    public String getModelo(){
        return modelo;
    }

    public String getPlaca(){
        return placa;
    }

    public String getCor(){
        return cor;
    }

    public int getAno() {
        return ano;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public void setPlaca(String placa) {
        this.placa = placa;
    }

    public void setCor(String cor) {
        this.cor = cor;
    }

    public void setAno(int ano) {
        this.ano = ano;
    }
}
