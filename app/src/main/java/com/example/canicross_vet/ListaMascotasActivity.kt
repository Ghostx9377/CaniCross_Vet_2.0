package com.example.canicross_vet

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ListaMascotasActivity : AppCompatActivity() {
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

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewMascotas)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Obtener datos de Firebase
        val dbRef = FirebaseDatabase.getInstance().getReference("Perro")
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val mascotasList = mutableListOf<Map<String, Any>>()
                
                for (mascotaSnapshot in snapshot.children) {
                    val mascota = mascotaSnapshot.value as? Map<String, Any>
                    mascota?.let {
                        val mascotaConId = it.toMutableMap()
                        mascotaConId["id"] = mascotaSnapshot.key ?: ""
                        mascotasList.add(mascotaConId)
                    }
                }
                
                recyclerView.adapter = MascotaAdapter(mascotasList)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ListaMascotasActivity, 
                    "Error al cargar los datos: ${error.message}", 
                    Toast.LENGTH_SHORT).show()
            }
        })
    }
} 