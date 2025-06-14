package com.svyatogor.appcaronaa3.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.svyatogor.appcaronaa3.Model.Usuario;
import com.svyatogor.appcaronaa3.R;

import java.util.List;

public class MotoristaAdapter extends ArrayAdapter<Usuario> {

    public interface OnAceitarClickListener {
        void onAceitarClick(Usuario motorista);
    }

    private OnAceitarClickListener listener;

    public MotoristaAdapter(@NonNull Context context, @NonNull List<Usuario> motoristas, OnAceitarClickListener listener) {
        super(context, 0, motoristas);
        this.listener = listener;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItemView = convertView;
        // Se a visualização não existe (primeira vez ou reciclagem), infle-a do XML
        if (listItemView == null) {
            // Garante que 'list_item_motorista' seja inflado corretamente
            listItemView = LayoutInflater.from(getContext()).inflate(
                    R.layout.list_item_motorista, parent, false);
            Log.d("MotoristaAdapter", "Inflating new listItemView for position: " + position);
        } else {
            Log.d("MotoristaAdapter", "Reusing convertView for position: " + position);
        }

        Usuario currentMotorista = getItem(position);

        // Encontra as views dentro do listItemView inflado
        TextView tvNomeMotorista = listItemView.findViewById(R.id.tv_nome_motorista);
        Button btnAceitar = listItemView.findViewById(R.id.btn_aceitar_motorista);

        // Adiciona uma verificação para garantir que as views foram encontradas
        if (tvNomeMotorista == null) {
            Log.e("MotoristaAdapter", "tv_nome_motorista é null! Verifique seu list_item_motorista.xml e R.id.");
        }
        if (btnAceitar == null) {
            Log.e("MotoristaAdapter", "btn_aceitar_motorista é null! Verifique seu list_item_motorista.xml e R.id.");
        }


        if (currentMotorista != null) {
            // Verifica se tvNomeMotorista não é null antes de usar
            if (tvNomeMotorista != null) {
                tvNomeMotorista.setText(currentMotorista.getNome());
            } else {
                Log.e("MotoristaAdapter", "Erro: TextView tvNomeMotorista é null para motorista: " + currentMotorista.getNome());
            }

            // Verifica se btnAceitar não é null antes de usar
            if (btnAceitar != null) {
                btnAceitar.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onAceitarClick(currentMotorista);
                    }
                });
            } else {
                Log.e("MotoristaAdapter", "Erro: Button btnAceitar é null para motorista: " + currentMotorista.getNome());
            }

        } else {
            Log.w("MotoristaAdapter", "currentMotorista é null na posição: " + position);
        }

        return listItemView;
    }
}
