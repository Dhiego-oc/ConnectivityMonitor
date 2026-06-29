package com.dhiego.connectivitymonitor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EventAdapter(
    private val events: MutableList<ConnectivityEvent>
) : RecyclerView.Adapter<EventAdapter.ViewHolder>() {

    // Formata o timestamp em hora legível: "14:32:07"
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTime: TextView        = view.findViewById(R.id.tvEventTime)
        val tvType: TextView        = view.findViewById(R.id.tvEventType)
        val tvDescription: TextView = view.findViewById(R.id.tvEventDescription)
    }

    // Cria o ViewHolder inflando o layout de cada item
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event, parent, false)
        return ViewHolder(view)
    }

    // Preenche os dados em cada item da lista
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val event = events[position]
        holder.tvTime.text        = timeFormat.format(Date(event.timestamp))
        holder.tvType.text        = event.type
        holder.tvDescription.text = event.description

        // Cor diferente por tipo de evento
        val color = when {
            event.type.startsWith("WIFI") -> 0xFF2980B9.toInt()  // azul
            event.type.startsWith("BT")   -> 0xFF8E44AD.toInt()  // roxo
            event.type.startsWith("GNSS") -> 0xFF27AE60.toInt()  // verde
            else                           -> 0xFF7F8C8D.toInt()  // cinza
        }
        holder.tvType.setTextColor(color)
    }

    override fun getItemCount(): Int = events.size

    // Adiciona novo evento no topo da lista
    fun addEvent(event: ConnectivityEvent) {
        events.add(0, event)
        notifyItemInserted(0)
    }
}