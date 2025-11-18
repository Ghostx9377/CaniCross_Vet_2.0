package com.example.canicross_vet

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MascotaAdapter(private val mascotas: List<Map<String, Any>>) : 
    RecyclerView.Adapter<MascotaAdapter.MascotaViewHolder>() {

    class MascotaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textViewNombre: TextView = view.findViewById(R.id.textViewNombre)
        val textViewRaza: TextView = view.findViewById(R.id.textViewRaza)
        val textViewFecha: TextView = view.findViewById(R.id.textViewFecha)
        val textViewSexo: TextView = view.findViewById(R.id.textViewSexo)
        val textViewPeso: TextView = view.findViewById(R.id.textViewPeso)
        val textViewEstatus: TextView = view.findViewById(R.id.textViewEstatus)
        val textViewObservaciones: TextView = view.findViewById(R.id.textViewObservaciones)
        val textViewIdPerro: TextView = view.findViewById(R.id.textViewIdPerro)
        val textViewIdDueno: TextView = view.findViewById(R.id.textViewIdDueno)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MascotaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_mascota, parent, false)
        return MascotaViewHolder(view)
    }

    override fun onBindViewHolder(holder: MascotaViewHolder, position: Int) {
        val mascota = mascotas[position]
        
        holder.textViewNombre.text = "Nombre: ${mascota["nombre"]}"
        holder.textViewRaza.text = "Raza: ${mascota["raza"]}"
        holder.textViewFecha.text = "Fecha de nacimiento: ${mascota["fecha_nacimiento"]}"
        holder.textViewSexo.text = "Sexo: ${mascota["sexo"]}"
        holder.textViewPeso.text = "Peso: ${mascota["peso"]} kg"
        holder.textViewEstatus.text = "Estatus: ${mascota["estatus"]}"
        
        val observaciones = mascota["observaciones"]?.toString() ?: ""
        if (observaciones.isNotEmpty()) {
            holder.textViewObservaciones.visibility = View.VISIBLE
            holder.textViewObservaciones.text = "Observaciones: $observaciones"
        } else {
            holder.textViewObservaciones.visibility = View.GONE
        }

        // Mostrar el ID del perro (clave del nodo)
        holder.textViewIdPerro.text = "ID Perro: ${mascota["id"] ?: ""}"
        holder.textViewIdDueno.text = "ID Dueño: ${mascota["id_dueño"] ?: ""}"

        // Estatus con color
        val estatus = mascota["estatus"]?.toString() ?: ""
        holder.textViewEstatus.text = "Estatus: $estatus"
        if (estatus.equals("valido", ignoreCase = true)) {
            holder.textViewEstatus.setTextColor(android.graphics.Color.parseColor("#388E3C")) // Verde
        } else if (estatus.equals("invalido", ignoreCase = true)) {
            holder.textViewEstatus.setTextColor(android.graphics.Color.parseColor("#D32F2F")) // Rojo
        } else {
            holder.textViewEstatus.setTextColor(android.graphics.Color.DKGRAY)
        }
    }

    override fun getItemCount() = mascotas.size
} 