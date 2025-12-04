package com.example.canicross_vet

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class CertificacionesActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var editTextNombre: TextInputEditText
    private lateinit var estadoText: android.widget.TextView
    private var todosLosPerros = mutableListOf<Map<String, Any>>()
    private lateinit var adapter: PerroAdapter

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

        recyclerView = findViewById<RecyclerView>(R.id.recyclerViewPerros)
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        editTextNombre = findViewById<TextInputEditText>(R.id.editTextNombrePerro)
        estadoText = findViewById<android.widget.TextView>(R.id.text_certificaciones_estado)
        val btnBuscar = findViewById<MaterialButton>(R.id.btn_buscar_certificaciones)

        // Búsqueda en tiempo real mientras escribe
        editTextNombre.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                buscarPerros(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnBuscar.setOnClickListener {
            val nombre = editTextNombre.text.toString().trim()
            if (nombre.isEmpty()) {
                Toast.makeText(this, "Ingresa el nombre del perro", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            buscarPerros(nombre)
        }

        // Cargar todos los perros inicialmente
        cargarTodosLosPerros()
    }

    private fun cargarTodosLosPerros() {
        val dbRef = FirebaseDatabase.getInstance().getReference("Perro")
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                todosLosPerros.clear()
                
                for (perroSnapshot in snapshot.children) {
                    val perro = perroSnapshot.value as? Map<String, Any>
                    perro?.let {
                        val perroConId = it.toMutableMap()
                        perroConId["id"] = perroSnapshot.key ?: ""
                        todosLosPerros.add(perroConId)
                    }
                }
                
                // Si hay texto en el buscador, aplicar el filtro
                val textoBusqueda = editTextNombre.text?.toString() ?: ""
                if (textoBusqueda.isNotEmpty()) {
                    buscarPerros(textoBusqueda)
                } else {
                    // Mostrar mensaje inicial
                    estadoText.text = "Ingresa el nombre del perro para buscar."
                    adapter = PerroAdapter(emptyList()) { }
                    recyclerView.adapter = adapter
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@CertificacionesActivity, 
                    "Error al cargar los datos: ${error.message}", 
                    Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun buscarPerros(nombreBusqueda: String) {
        val textoFiltrado = nombreBusqueda.trim().lowercase()
        
        val perrosFiltrados = if (textoFiltrado.isEmpty()) {
            emptyList()
        } else {
            todosLosPerros.filter { perro ->
                val nombre = perro["nombre"]?.toString()?.lowercase() ?: ""
                nombre.contains(textoFiltrado, ignoreCase = true)
            }
        }
        
        if (perrosFiltrados.isEmpty() && textoFiltrado.isNotEmpty()) {
            estadoText.text = "No se encontraron perros con ese nombre."
        } else if (perrosFiltrados.isNotEmpty()) {
            estadoText.text = "Se encontraron ${perrosFiltrados.size} perro(s):"
        } else {
            estadoText.text = "Ingresa el nombre del perro para buscar."
        }
        
        adapter = PerroAdapter(perrosFiltrados) { perro ->
            // Abrir actividad de detalle de certificaciones
            val intent = Intent(this, DetalleCertificacionesActivity::class.java)
            intent.putExtra("idPerro", perro["id"]?.toString() ?: "")
            intent.putExtra("idDueno", perro["id_dueño"]?.toString() ?: "")
            intent.putExtra("nombrePerro", perro["nombre"]?.toString() ?: "")
            startActivity(intent)
        }
        recyclerView.adapter = adapter
    }
}

