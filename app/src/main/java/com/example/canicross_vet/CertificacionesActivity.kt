package com.example.canicross_vet

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class CertificacionesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_certificaciones)

        val toolbar = findViewById<Toolbar>(R.id.toolbar_certificaciones)
        toolbar.setNavigationOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish()
        }

        val editIdPerro = findViewById<EditText>(R.id.editTextIdPerroCert)
        val editIdDueno = findViewById<EditText>(R.id.editTextIdDuenoCert)
        val btnBuscar = findViewById<Button>(R.id.btn_buscar_certificaciones)
        val estadoText = findViewById<TextView>(R.id.text_certificaciones_estado)
        val certificacionesContainer = findViewById<LinearLayout>(R.id.layout_certificaciones)

        btnBuscar.setOnClickListener {
            val idPerro = editIdPerro.text.toString().trim()
            val idDueno = editIdDueno.text.toString().trim()

            if (idPerro.isEmpty() || idDueno.isEmpty()) {
                Toast.makeText(this, "Ingresa ambos ID", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            estadoText.text = "Verificando información del perro..."
            certificacionesContainer.removeAllViews()

            verificarPerro(idPerro, idDueno, estadoText) { coincide ->
                if (coincide) {
                    consultarCertificaciones(idPerro, idDueno, estadoText, certificacionesContainer)
                }
            }
        }
    }

    private fun verificarPerro(
        idPerro: String,
        idDueno: String,
        estadoText: TextView,
        onResult: (Boolean) -> Unit
    ) {
        val perroRef = FirebaseDatabase.getInstance().getReference("Perro").child(idPerro)
        perroRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    estadoText.text = "No se encontró ningún perro con ese ID."
                    onResult(false)
                    return
                }

                val perro = snapshot.value as? Map<*, *>
                if (perro == null) {
                    estadoText.text = "Error al leer la información del perro."
                    onResult(false)
                    return
                }

                if (perro["id_dueño"]?.toString() != idDueno) {
                    estadoText.text = "El ID del dueño no coincide."
                    onResult(false)
                    return
                }

                estadoText.text = "Buscando certificaciones..."
                onResult(true)
            }

            override fun onCancelled(error: DatabaseError) {
                estadoText.text = "Error al consultar: ${error.message}"
                onResult(false)
            }
        })
    }

    private fun consultarCertificaciones(
        idPerro: String,
        idDueno: String,
        estadoText: TextView,
        container: LinearLayout
    ) {
        val certInscritasRef = FirebaseDatabase.getInstance().getReference("CertificacionesInscritas").child(idPerro)
        certInscritasRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                container.removeAllViews()
                if (!snapshot.exists()) {
                    estadoText.text = "Este perro no tiene certificaciones inscritas."
                    return
                }

                var tieneCertificaciones = false
                snapshot.children.forEach { certSnapshot ->
                    val certificacion = certSnapshot.value as? Map<*, *>
                    if (certificacion != null) {
                        val certMap = certificacion.asStringMap()
                        val idDuenoRegistro = certMap["id_dueño"]?.toString()
                            ?: certMap["idDueño"]?.toString()
                            ?: certMap["id_dueno"]?.toString()
                        
                        if (idDuenoRegistro == idDueno) {
                            tieneCertificaciones = true
                            val idCertificacion = certSnapshot.key ?: ""
                            agregarFilaCertificacion(
                                container, 
                                construirTextoCertificacionInscrita(certMap),
                                idPerro,
                                idCertificacion,
                                certMap
                            )
                        }
                    }
                }
                
                if (!tieneCertificaciones) {
                    estadoText.text = "Este perro no tiene certificaciones inscritas para este dueño."
                } else {
                    estadoText.text = "Certificaciones inscritas encontradas:"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                estadoText.text = "Error al consultar: ${error.message}"
            }
        })
    }

    private fun construirTextoCertificacionInscrita(datos: Map<String, Any?>): String {
        val nombreCert = obtenerValor(datos, "nombre_certificacion", "nombreCertificacion", "nombre") 
            ?: "Nombre no especificado"
        val estado = obtenerValor(datos, "estado", "status") ?: "No especificado"
        val idCert = obtenerValor(datos, "id_certificacion", "idCertificacion", "idCertificado") ?: ""
        val calificacionRaw = datos["calificacion"]?.toString() ?: datos["calificación"]?.toString()
        val calificacion = when {
            calificacionRaw != null && calificacionRaw.isNotEmpty() -> calificacionRaw
            calificacionRaw == "" -> "Sin calificar"
            else -> "N/A"
        }
        val fechaInscripcion = obtenerValor(datos, "fecha_inscripcion", "fechaInscripcion", "fecha")
        val observaciones = obtenerValor(datos, "observaciones_veterinario", "observacionesVeterinario", "observaciones")
            ?: "Sin observaciones"
        
        val fechaFormateada = if (fechaInscripcion != null && fechaInscripcion.isNotEmpty()) {
            try {
                val timestamp = fechaInscripcion.toLongOrNull()
                if (timestamp != null) {
                    val fecha = java.util.Date(timestamp)
                    val formato = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
                    formato.format(fecha)
                } else {
                    fechaInscripcion
                }
            } catch (e: Exception) {
                fechaInscripcion
            }
        } else {
            "No especificada"
        }

        return buildString {
            append("📋 ").append(nombreCert)
            if (idCert.isNotEmpty()) append(" (ID: $idCert)")
            append("\n\nEstado: ").append(estado)
            append("\nCalificación: ").append(calificacion)
            append("\nFecha de inscripción: ").append(fechaFormateada)
            if (observaciones.isNotEmpty() && observaciones != "Sin observaciones") {
                append("\nObservaciones: ").append(observaciones)
            }
        }
    }

    private fun obtenerValor(mapa: Map<String, Any?>, vararg claves: String): String? {
        claves.forEach { clave ->
            val valor = mapa[clave]
            if (valor != null) {
                val str = valor.toString()
                if (str.isNotEmpty()) return str
            }
        }
        return null
    }

    private fun Map<*, *>.asStringMap(): MutableMap<String, Any?> {
        val resultado = mutableMapOf<String, Any?>()
        for ((clave, valor) in this) {
            val key = clave?.toString() ?: continue
            resultado[key] = valor
        }
        return resultado
    }

    private fun agregarFilaCertificacion(
        container: LinearLayout, 
        texto: String,
        idPerro: String,
        idCertificacion: String,
        datosCertificacion: Map<String, Any?>
    ) {
        val textView = TextView(this).apply {
            this.text = texto
            setTextColor(resources.getColor(android.R.color.white, null))
            textSize = 16f
            setPadding(32, 16, 32, 16)
            isClickable = true
            isFocusable = true
            background = android.graphics.drawable.ColorDrawable(android.graphics.Color.parseColor("#40000000"))
            setOnClickListener {
                mostrarDialogoEditarCalificacion(idPerro, idCertificacion, datosCertificacion, this)
            }
        }
        val divider = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                1
            ).also { it.setMargins(0, 8, 0, 8) }
            setBackgroundColor(resources.getColor(android.R.color.white, null))
            alpha = 0.3f
        }
        container.addView(textView)
        container.addView(divider)
    }

    private fun mostrarDialogoEditarCalificacion(
        idPerro: String,
        idCertificacion: String,
        datosCertificacion: Map<String, Any?>,
        textView: TextView
    ) {
        val calificacionActual = datosCertificacion["calificacion"]?.toString() 
            ?: datosCertificacion["calificación"]?.toString() 
            ?: ""
        
        val nombreCert = obtenerValor(datosCertificacion, "nombre_certificacion", "nombreCertificacion", "nombre") 
            ?: "Certificación"

        val numberPicker = NumberPicker(this).apply {
            minValue = 1
            maxValue = 10
            wrapSelectorWheel = false
            // Establecer el valor actual si existe y está en el rango válido
            val valorActual = calificacionActual.toIntOrNull()
            if (valorActual != null && valorActual in 1..10) {
                value = valorActual
            } else {
                value = 1
            }
            setPadding(32, 16, 32, 16)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Editar Calificación - $nombreCert")
            .setMessage("Seleccione la calificación (1-10):")
            .setView(numberPicker)
            .setPositiveButton("Guardar") { _, _ ->
                val nuevaCalificacion = numberPicker.value.toString()
                actualizarCalificacion(idPerro, idCertificacion, nuevaCalificacion, textView, datosCertificacion)
            }
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.show()
    }

    private fun actualizarCalificacion(
        idPerro: String,
        idCertificacion: String,
        nuevaCalificacion: String,
        textView: TextView,
        datosCertificacion: Map<String, Any?>
    ) {
        val certRef = FirebaseDatabase.getInstance()
            .getReference("CertificacionesInscritas")
            .child(idPerro)
            .child(idCertificacion)

        val updates = mutableMapOf<String, Any>(
            "calificacion" to nuevaCalificacion,
            "estado" to "calificado"
        )

        certRef.updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Calificación actualizada correctamente", Toast.LENGTH_SHORT).show()
                // Actualizar el texto en la vista
                val datosActualizados = datosCertificacion.toMutableMap()
                datosActualizados["calificacion"] = nuevaCalificacion
                datosActualizados["estado"] = "calificado"
                textView.text = construirTextoCertificacionInscrita(datosActualizados)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al actualizar la calificación: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}

