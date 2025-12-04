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
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ListaMascotasActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var editTextBuscar: TextInputEditText
    private var todasLasMascotas = mutableListOf<Map<String, Any>>()
    private lateinit var adapter: MascotaAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lista_mascotas)

        val toolbar = findViewById<Toolbar>(R.id.toolbar_lista_mascotas)
        toolbar.setNavigationOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish()
        }

        recyclerView = findViewById<RecyclerView>(R.id.recyclerViewMascotas)
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        editTextBuscar = findViewById<TextInputEditText>(R.id.editTextBuscar)

        // Configurar el listener de búsqueda
        editTextBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filtrarMascotas(s.toString())
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })

        // Obtener datos de Firebase
        val dbRef = FirebaseDatabase.getInstance().getReference("Perro")
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                todasLasMascotas.clear()
                
                for (mascotaSnapshot in snapshot.children) {
                    val mascota = mascotaSnapshot.value as? Map<String, Any>
                    mascota?.let {
                        val mascotaConId = it.toMutableMap()
                        mascotaConId["id"] = mascotaSnapshot.key ?: ""
                        todasLasMascotas.add(mascotaConId)
                    }
                }
                
                // Inicializar el adapter con todas las mascotas
                adapter = MascotaAdapter(todasLasMascotas)
                recyclerView.adapter = adapter
                
                // Si hay texto en el buscador, aplicar el filtro
                val textoBusqueda = editTextBuscar.text?.toString() ?: ""
                if (textoBusqueda.isNotEmpty()) {
                    filtrarMascotas(textoBusqueda)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ListaMascotasActivity, 
                    "Error al cargar los datos: ${error.message}", 
                    Toast.LENGTH_SHORT).show()
            }
        })
    }
    
    private fun filtrarMascotas(textoBusqueda: String) {
        val textoFiltrado = textoBusqueda.trim().lowercase()
        
        val mascotasFiltradas = if (textoFiltrado.isEmpty()) {
            todasLasMascotas
        } else {
            todasLasMascotas.filter { mascota ->
                val nombre = mascota["nombre"]?.toString()?.lowercase() ?: ""
                nombre.contains(textoFiltrado, ignoreCase = true)
            }
        }
        
        adapter = MascotaAdapter(mascotasFiltradas)
        recyclerView.adapter = adapter
    }
} 