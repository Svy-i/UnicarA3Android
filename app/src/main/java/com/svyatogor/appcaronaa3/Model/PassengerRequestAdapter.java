package com.svyatogor.appcaronaa3.Model;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.svyatogor.appcaronaa3.R;

import java.util.List;

public class PassengerRequestAdapter extends RecyclerView.Adapter<PassengerRequestAdapter.PassengerRequestViewHolder> {

    private List<Usuario> passageirosLista;
    private OnItemClickListener listener;

    // Interface para lidar com cliques nos itens
    public interface OnItemClickListener {
        void onAcceptRideClick(Usuario passageiro);
    }

    public PassengerRequestAdapter(List<Usuario> passageirosLista, OnItemClickListener listener) {
        this.passageirosLista = passageirosLista;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PassengerRequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_passenger_request, parent, false);
        return new PassengerRequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PassengerRequestViewHolder holder, int position) {
        Usuario passenger = passageirosLista.get(position);
        holder.tvPassengerName.setText(passenger.getNome());
        holder.tvPickupLocation.setText("Partida: " + passenger.getPontoDeChegada());
        holder.tvDestination.setText("Destino: " + passenger.getDestino());

        // Opcional: Se você tiver a distância, defina e torne visível
        // holder.tvEstimatedDistance.setText(String.format("Distância: %.1f km", passenger.getDistance()));
        // holder.tvEstimatedDistance.setVisibility(View.VISIBLE);

        holder.btnAcceptRide.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAcceptRideClick(passenger);
            }
        });
    }

    @Override
    public int getItemCount() {
        return passageirosLista.size();
    }

    // ViewHolder para cada item da lista
    public static class PassengerRequestViewHolder extends RecyclerView.ViewHolder {
        public TextView tvPassengerName;
        public TextView tvPickupLocation;
        public TextView tvDestination;
        public TextView tvEstimatedDistance; // Opcional
        public Button btnAcceptRide;

        public PassengerRequestViewHolder(View itemView) {
            super(itemView);
            tvPassengerName = itemView.findViewById(R.id.tvPassengerName);
            tvPickupLocation = itemView.findViewById(R.id.tvPickupLocation);
            tvDestination = itemView.findViewById(R.id.tvDestination);
            tvEstimatedDistance = itemView.findViewById(R.id.tvEstimatedDistance); // Opcional
            btnAcceptRide = itemView.findViewById(R.id.btnAcceptRide);
        }
    }
}