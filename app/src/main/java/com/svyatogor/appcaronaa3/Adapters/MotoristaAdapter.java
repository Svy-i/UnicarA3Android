package com.svyatogor.appcaronaa3.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.svyatogor.appcaronaa3.Model.Carro;
import com.svyatogor.appcaronaa3.Model.Usuario;
import com.svyatogor.appcaronaa3.R;

import java.util.List;

public class MotoristaAdapter extends ArrayAdapter<Usuario> {

    private final Context context;
    private final List<Usuario> motoristas;
    private final OnMotoristaActionListener listener;
    public interface OnMotoristaActionListener {
        void onAceitarClick(Usuario motorista);
    }

    public MotoristaAdapter(@NonNull Context context, List<Usuario> motoristas, OnMotoristaActionListener listener) {
        super(context, 0, motoristas);
        this.context = context;
        this.motoristas = motoristas;
        this.listener = listener;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.list_item_motorista, parent, false);
        }

        Usuario motorista = motoristas.get(position);

        ImageView ivUserPhoto = convertView.findViewById(R.id.iv_user_photo);
        TextView tvNomeMotorista = convertView.findViewById(R.id.tv_nome_motorista);
        TextView tvPlacaCarro = convertView.findViewById(R.id.tv_placa_carro);
        TextView tvNumVagas = convertView.findViewById(R.id.tv_num_vagas);
        Button btnAceitarMotorista = convertView.findViewById(R.id.btn_aceitar_motorista);

        tvNomeMotorista.setText(motorista.getNome());

        Carro carro = motorista.getCarro();
        if (carro != null) {
            tvPlacaCarro.setText("Placa: " + carro.getPlaca());
            tvNumVagas.setText("Vagas: " + carro.getNumVagas());
        } else {
            tvPlacaCarro.setText("Placa: N/A");
            tvNumVagas.setText("Vagas: N/A");
        }

        if (motorista.getFotoPerfilUrl() != null && !motorista.getFotoPerfilUrl().isEmpty()) {
            Glide.with(context)
                    .load(motorista.getFotoPerfilUrl())
                    .placeholder(R.drawable.ic_user)
                    .error(R.drawable.ic_eye_off)
                    .into(ivUserPhoto);
        } else {
            ivUserPhoto.setImageResource(R.drawable.ic_user);
        }

        btnAceitarMotorista.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAceitarClick(motorista);
            }
        });

        return convertView;
    }
}
