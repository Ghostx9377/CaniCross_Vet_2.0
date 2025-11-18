package com.example.canicross_vet

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AprobacionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_aprobacion)

        val toolbar = findViewById<Toolbar>(R.id.toolbar_aprobacion)
        toolbar.setNavigationOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish()
        }

        val radioGroup = findViewById<RadioGroup>(R.id.radioGroupEstatus)
        val radioInvalido = findViewById<RadioButton>(R.id.radioInvalido)
        val radioValido = findViewById<RadioButton>(R.id.radioValido)
        val editTextObservaciones = findViewById<EditText>(R.id.editTextObservaciones)

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.radioInvalido) {
                editTextObservaciones.visibility = View.VISIBLE
            } else if (checkedId == R.id.radioValido) {
                editTextObservaciones.setText("")
                editTextObservaciones.visibility = View.GONE
            } else {
                editTextObservaciones.visibility = View.GONE
            }
        }

        // Campos de la interfaz
        val editIdDueno = findViewById<EditText>(R.id.editTextText2)
        val editIdPerro = findViewById<EditText>(R.id.editTextText)
        val editNombre = findViewById<EditText>(R.id.editTextText3)
        val editRaza = findViewById<EditText>(R.id.editTextText4)
        val editFecha = findViewById<EditText>(R.id.editTextText5)
        val editSexo = findViewById<EditText>(R.id.editTextText6)
        val editPeso = findViewById<EditText>(R.id.editTextText7)
        val editCartilla = findViewById<EditText>(R.id.editTextText8)
        val btnBuscar = findViewById<Button>(R.id.btn_buscar)
        val btnLimpiar = findViewById<Button>(R.id.btn_limpiar)
        val btnEditar = findViewById<Button>(R.id.btn_editar)

        btnBuscar.setOnClickListener {
            val idPerro = editIdPerro.text.toString().trim()
            val idDueno = editIdDueno.text.toString().trim()
            if (idPerro.isEmpty() || idDueno.isEmpty()) {
                Toast.makeText(this, "Ingresa ambos ID", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val dbRef = FirebaseDatabase.getInstance().getReference("Perro").child(idPerro)
            dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val perro = snapshot.value as Map<*, *>
                        if (perro["id_dueño"]?.toString() == idDueno) {
                            editNombre.setText(perro["nombre"]?.toString() ?: "")
                            editNombre.setTextColor(android.graphics.Color.BLACK)
                            editRaza.setText(perro["raza"]?.toString() ?: "")
                            editRaza.setTextColor(android.graphics.Color.BLACK)
                            editFecha.setText(perro["fecha_nacimiento"]?.toString() ?: "")
                            editFecha.setTextColor(android.graphics.Color.BLACK)
                            editSexo.setText(perro["sexo"]?.toString() ?: "")
                            editSexo.setTextColor(android.graphics.Color.BLACK)
                            editPeso.setText(perro["peso"]?.toString() ?: "")
                            editPeso.setTextColor(android.graphics.Color.BLACK)
                            editCartilla.setText(perro["numero_cartilla"]?.toString() ?: "")
                            editCartilla.setTextColor(android.graphics.Color.BLACK)
                            // Selección automática del radio button de estatus
                            val estatus = perro["estatus"]?.toString() ?: ""
                            if (estatus == "invalido") {
                                radioInvalido.isChecked = true
                            } else if (estatus == "valido") {
                                radioValido.isChecked = true
                            } else {
                                radioGroup.clearCheck()
                            }
                            Toast.makeText(this@AprobacionActivity, "Registro encontrado exitosamente", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@AprobacionActivity, "El ID del dueño no coincide", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@AprobacionActivity, "No se encontró ningún perro con ese ID", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@AprobacionActivity, "Error al consultar la base de datos", Toast.LENGTH_SHORT).show()
                }
            })
        }

        btnLimpiar.setOnClickListener {
            editIdPerro.setText("")
            editIdDueno.setText("")
            editNombre.setText("")
            editRaza.setText("")
            editFecha.setText("")
            editSexo.setText("")
            editPeso.setText("")
            editCartilla.setText("")
            editTextObservaciones.setText("")
            editTextObservaciones.visibility = View.GONE
            radioGroup.clearCheck()
        }

        btnEditar.setOnClickListener {
            val idPerro = editIdPerro.text.toString().trim()
            if (idPerro.isEmpty()) {
                Toast.makeText(this, "Ingresa el ID del perro para editar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Obtener el estatus seleccionado
            val estatus = when {
                radioValido.isChecked -> "valido"
                radioInvalido.isChecked -> "invalido"
                else -> ""
            }

            // Obtener observaciones
            var observaciones = editTextObservaciones.text.toString().trim()
            // Si es válido, limpiar observaciones
            if (estatus == "valido") {
                observaciones = ""
            }
            // Obtener peso
            val peso = editPeso.text.toString().trim()
            // Obtener sexo
            val sexo = editSexo.text.toString().trim()

            // Crear el mapa de actualización
            val updates = mutableMapOf<String, Any>()
            if (estatus.isNotEmpty()) updates["estatus"] = estatus
            updates["observaciones"] = observaciones
            if (peso.isNotEmpty()) updates["peso"] = peso
            if (sexo.isNotEmpty()) updates["sexo"] = sexo

            // Actualizar en Firebase
            val dbRef = FirebaseDatabase.getInstance().getReference("Perro").child(idPerro)
            dbRef.updateChildren(updates)
                .addOnSuccessListener {
                    Toast.makeText(this, "Registro actualizado correctamente", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al actualizar el registro", Toast.LENGTH_SHORT).show()
                }
        }
    }
} 