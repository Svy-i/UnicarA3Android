package com.svyatogor.appcaronaa3.Model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ConexaoBD {

    private static final String URL = "jdbc:mysql://localhost:3306/unicara3db";
    private static final String USER = "Svy";
    private static final String PASSWORD = "S3nhaSQL!";

    // Metodo para conectar ao banco de dados
    public static Connection conectar() {
        try {
            Connection conexao = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Conexão estabelecida com sucesso!");
            return conexao;
        } catch (SQLException e) {
            System.out.println("Erro ao conectar ao banco de dados: " + e.getMessage());
            return null;
        }
    }

    // Metodo para buscar dados do banco
    public static ResultSet buscar(String sql) {
        try {
            Connection conexao = conectar();
            PreparedStatement stmt = conexao.prepareStatement(sql);
            ResultSet resultado = stmt.executeQuery();
            return resultado;
        } catch (SQLException e) {
            System.out.println("Erro ao buscar dados: " + e.getMessage());
            return null;
        }
    }

    // Metodo para salvar dados no banco
    public static boolean salvar(String sql) {
        try (Connection conexao = conectar();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.out.println("Erro ao salvar dados: " + e.getMessage());
            return false;
        }
    }

    // Metodo para atualizar dados no banco
    public static boolean atualizar(String sql) {
        return salvar(sql); // Reutiliza o metodo de salvar com SQL de atualização
    }

    // Metodo para deletar dados do banco
    public static boolean deletar(String sql) {
        return salvar(sql); // Reutiliza o metodo de salvar com SQL de deleção
    }
}
