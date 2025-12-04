package com.example.canicross_vet

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.*

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        val navView = findViewById<NavigationView>(R.id.nav_view)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)

        setSupportActionBar(toolbar)
        
        // Centrar el título del toolbar
        supportActionBar?.setDisplayShowTitleEnabled(false)
        val titleTextView = TextView(this).apply {
            text = "Eventos Próximos"
            setTextColor(android.graphics.Color.WHITE)
            textSize = 20f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val params = Toolbar.LayoutParams(
            Toolbar.LayoutParams.WRAP_CONTENT,
            Toolbar.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER
        }
        toolbar.addView(titleTextView, params)
        
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_registro -> {
                    val intent = Intent(this, AprobacionActivity::class.java)
                    startActivity(intent)
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_consulta -> {
                    val intent = Intent(this, ListaMascotasActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_certificaciones -> {
                    val intent = Intent(this, CertificacionesActivity::class.java)
                    startActivity(intent)
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_logout -> {
                    finish() // Aquí puedes poner la lógica de cerrar sesión
                    true
                }
                else -> false
            }
        }

        // Cargar eventos
        cargarEventos()
    }

    private fun cargarEventos() {
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewEventos)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val eventosRef = FirebaseDatabase.getInstance().getReference("eventos")
        eventosRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val eventosList = mutableListOf<Map<String, Any>>()
                
                for (eventoSnapshot in snapshot.children) {
                    val evento = eventoSnapshot.value as? Map<String, Any>
                    evento?.let {
                        val eventoConId = it.toMutableMap()
                        eventoConId["id"] = eventoSnapshot.key ?: ""
                        eventosList.add(eventoConId)
                    }
                }

                // Ordenar eventos por fecha (más próximos primero)
                val eventosOrdenados = eventosList.sortedBy { evento ->
                    parsearFecha(evento["fecha"]?.toString() ?: "")
                }

                // Aplicar colores pastel según la proximidad
                val eventosConColor = eventosOrdenados.map { evento ->
                    val fechaEvento = parsearFecha(evento["fecha"]?.toString() ?: "")
                    val colorEstado = obtenerColorEstadoPastel(fechaEvento)
                    evento.toMutableMap().apply {
                        put("colorEstado", colorEstado)
                        put("fechaDate", fechaEvento)
                    }
                }

                // Agrupar eventos por mes
                val itemsAgrupados = agruparEventosPorMes(eventosConColor)

                recyclerView.adapter = EventoAdapter(itemsAgrupados) { evento ->
                    mostrarDetalleEvento(evento)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Manejar error si es necesario
            }
        })
    }

    private fun agruparEventosPorMes(eventos: List<Map<String, Any>>): List<EventoItem> {
        val items = mutableListOf<EventoItem>()
        val formatoMes = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        
        // Mapeo de meses en español
        val mesesEspañol = mapOf(
            "January" to "Enero", "February" to "Febrero", "March" to "Marzo",
            "April" to "Abril", "May" to "Mayo", "June" to "Junio",
            "July" to "Julio", "August" to "Agosto", "September" to "Septiembre",
            "October" to "Octubre", "November" to "Noviembre", "December" to "Diciembre"
        )
        
        var mesActual: String? = null
        
        eventos.forEach { evento ->
            val fecha = evento["fechaDate"] as? Date ?: Date(0)
            val mesEventoFormateado = formatoMes.format(fecha)
            
            // Convertir a español si es necesario
            val mesEvento = mesesEspañol.entries.find { mesEventoFormateado.contains(it.key) }?.value
                ?.let { "$it ${mesEventoFormateado.split(" ")[1]}" }
                ?: mesEventoFormateado
            
            if (mesActual != mesEvento) {
                mesActual = mesEvento
                items.add(EventoItem.Header("📅 $mesEvento"))
            }
            
            items.add(EventoItem.Evento(evento))
        }
        
        return items
    }

    private fun parsearFecha(fechaStr: String): Date {
        return try {
            val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            formato.parse(fechaStr) ?: Date(0)
        } catch (e: Exception) {
            Date(0)
        }
    }

    private fun obtenerColorEstadoPastel(fechaEvento: Date): Int {
        val ahora = Date()
        val diferencia = fechaEvento.time - ahora.time
        val diasDiferencia = diferencia / (1000 * 60 * 60 * 24)

        return when {
            diasDiferencia < 0 -> android.graphics.Color.parseColor("#F8D7DA") // Rosa pastel - ya pasó
            diasDiferencia <= 7 -> android.graphics.Color.parseColor("#FFF8C6") // Amarillo suave - dentro de una semana
            else -> android.graphics.Color.parseColor("#D9F8D6") // Verde menta - más de una semana
        }
    }

    private fun mostrarDetalleEvento(evento: Map<String, Any>) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_detalle_evento, null)

        // Obtener referencias a los TextViews
        val textViewTitulo = dialogView.findViewById<TextView>(R.id.textViewTituloEvento)
        val textViewTipo = dialogView.findViewById<TextView>(R.id.textViewTipoEventoDetalle)
        val textViewFecha = dialogView.findViewById<TextView>(R.id.textViewFechaDetalle)
        val textViewHora = dialogView.findViewById<TextView>(R.id.textViewHoraDetalle)
        val textViewUbicacion = dialogView.findViewById<TextView>(R.id.textViewUbicacionDetalle)
        val textViewDistancia = dialogView.findViewById<TextView>(R.id.textViewDistanciaDetalle)
        val textViewCupo = dialogView.findViewById<TextView>(R.id.textViewCupoDetalle)
        val textViewRegistrados = dialogView.findViewById<TextView>(R.id.textViewRegistradosDetalle)
        val textViewDescripcion = dialogView.findViewById<TextView>(R.id.textViewDescripcionDetalle)

        // Llenar los datos
        textViewTitulo.text = evento["nombre"]?.toString() ?: "Sin nombre"
        textViewTipo.text = "Tipo: ${evento["tipo"]?.toString() ?: "N/A"}"
        textViewFecha.text = evento["fecha"]?.toString() ?: "N/A"
        textViewHora.text = evento["hora"]?.toString() ?: "N/A"
        textViewUbicacion.text = evento["ubicacion"]?.toString() ?: "No especificada"
        textViewDistancia.text = evento["distancia"]?.toString() ?: "N/A"
        textViewCupo.text = evento["cupo"]?.toString() ?: "0"
        textViewRegistrados.text = evento["registrados"]?.toString() ?: "0"
        textViewDescripcion.text = evento["descripcion"]?.toString() ?: "Sin descripción disponible"

        // Crear y mostrar el diálogo
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Cerrar", null)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        // Configurar el tamaño del diálogo
        val displayMetrics = resources.displayMetrics
        val width = (displayMetrics.widthPixels * 0.92).toInt()
        val maxHeight = (displayMetrics.heightPixels * 0.85).toInt()
        
        dialog.window?.setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        
        // Animación de entrada suave
        dialog.window?.attributes?.windowAnimations = android.R.style.Animation_Dialog
        
        dialog.show()
        
        // Animación personalizada de entrada
        dialogView.alpha = 0f
        dialogView.scaleX = 0.9f
        dialogView.scaleY = 0.9f
        
        dialogView.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(300)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()
    }
} 