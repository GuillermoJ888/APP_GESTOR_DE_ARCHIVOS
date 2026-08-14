package com.example.gestorarchivos;

import android.os.Bundle;
import android.os.Environment;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

public class MainActivity extends AppCompatActivity {

    // Elementos de la interfaz
    private EditText etNombreArchivo;
    private EditText etContenido;
    private Button btnGuardar;
    private Button btnListar;
    private ListView lvArchivos;
    private TextView tvContenidoArchivo;

    // Lista que almacenará los nombres de los archivos
    private ArrayList<String> listaArchivos;

    // Adaptador para mostrar los archivos en el ListView
    private ArrayAdapter<String> adaptadorArchivos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Vinculación de elementos XML con Java
        etNombreArchivo = findViewById(R.id.etNombreArchivo);
        etContenido = findViewById(R.id.etContenido);
        btnGuardar = findViewById(R.id.btnGuardar);
        btnListar = findViewById(R.id.btnListar);
        lvArchivos = findViewById(R.id.lvArchivos);
        tvContenidoArchivo = findViewById(R.id.tvContenidoArchivo);

        // Inicialización de la lista
        listaArchivos = new ArrayList<>();

        // Creación del adaptador
        adaptadorArchivos = new ArrayAdapter<>(
                this,
                R.layout.item_archivo,
                listaArchivos
        );

        // Asociar el adaptador con el ListView
        lvArchivos.setAdapter(adaptadorArchivos);

        // Evento del botón Guardar
        btnGuardar.setOnClickListener(v -> guardarArchivo());

        // Evento del botón Listar
        btnListar.setOnClickListener(v -> listarArchivos());

        // Evento al seleccionar un archivo de la lista
        lvArchivos.setOnItemClickListener((parent, view, position, id) -> {
            String nombreArchivo = listaArchivos.get(position);
            leerArchivo(nombreArchivo);
        });

        // Mostrar los archivos existentes al abrir la aplicación
        listarArchivos();
    }

    /**
     * Guarda un archivo de texto en el almacenamiento
     * externo privado de la aplicación.
     */
    private void guardarArchivo() {

        // A) Recuperar los datos escritos por el usuario
        String nombreArchivo = etNombreArchivo
                .getText()
                .toString()
                .trim();

        String contenido = etContenido
                .getText()
                .toString();

        // B) Validar información

        // Validar el nombre del archivo
        if (nombreArchivo.isEmpty()) {
            etNombreArchivo.setError("Escribe el nombre del archivo");
            etNombreArchivo.requestFocus();
            return;
        }

        // Agregar la extensión .txt si el usuario no la escribió
        if (!nombreArchivo.toLowerCase().endsWith(".txt")) {
            nombreArchivo = nombreArchivo + ".txt";
        }

        // C) Consultar estado de Almacenamiento
        // La clase Environment permite consultar el estado de Almacenamiento externo
        String estadoAlmacenamiento = Environment.getExternalStorageState();

        // D) Comprobar que el almacenamiento esté disponible
        if (!Environment.MEDIA_MOUNTED.equals(estadoAlmacenamiento)) {
            Toast.makeText(
                    this,
                    "El almacenamiento externo no está disponible",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        // E) Obtener carpeta externa de la aplicación
        // Obtiene la carpeta externa -> el resultado se guarda en un File
        // (también puede representar direcciones y rutas)
        File carpeta = getExternalFilesDir(null);

        // Si el objeto "carpeta" no existe, imprime el mensaje de error
        if (carpeta == null) {
            Toast.makeText(
                    this,
                    "No fue posible acceder al almacenamiento",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        // Crear y escribir el archivo dentro de la carpeta obtenida
        File archivo = new File(carpeta, nombreArchivo);

        try (FileOutputStream fos = new FileOutputStream(archivo)) {
            fos.write(contenido.getBytes(StandardCharsets.UTF_8));
            Toast.makeText(this, "Archivo guardado: " + nombreArchivo, Toast.LENGTH_SHORT).show();

            // Limpiar los campos después de guardar
            etNombreArchivo.setText("");
            etContenido.setText("");

            // Refrescar la lista de archivos
            listarArchivos();

        } catch (Exception e) {
            Toast.makeText(this, "Error al guardar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Obtiene los archivos almacenados en la carpeta
     * externa privada y los muestra en el ListView.
     */
    private void listarArchivos() {

        // Limpiar la lista anterior
        listaArchivos.clear();

        // Obtener la carpeta privada de la aplicación
        File carpeta = getExternalFilesDir(null);

        // Validar que la carpeta exista antes de listar
        if (carpeta == null) {
            Toast.makeText(
                    this,
                    "No fue posible acceder a la carpeta",
                    Toast.LENGTH_SHORT
            ).show();
            adaptadorArchivos.notifyDataSetChanged();
            return;
        }

        // Obtener todos los archivos de la carpeta
        File[] archivos = carpeta.listFiles();

        if (archivos != null) {
            // Ordenar los archivos alfabéticamente (compatible con API 21+)
            Arrays.sort(archivos, new Comparator<File>() {
                @Override
                public int compare(File f1, File f2) {
                    return f1.getName().compareTo(f2.getName());
                }
            });

            // Agregar únicamente archivos, no carpetas
            for (File f : archivos) {
                if (f.isFile()) {
                    listaArchivos.add(f.getName());
                }
            }
        }

        // Notificar al adaptador que la lista cambió
        adaptadorArchivos.notifyDataSetChanged();
    }

    /**
     * Lee el contenido de un archivo seleccionado
     * y lo muestra en la pantalla.
     *
     * @param nombreArchivo nombre del archivo seleccionado
     */
    private void leerArchivo(String nombreArchivo) {
        File carpeta = getExternalFilesDir(null);

        if (carpeta == null) {
            Toast.makeText(this, "No fue posible acceder a la carpeta", Toast.LENGTH_SHORT).show();
            return;
        }

        File archivo = new File(carpeta, nombreArchivo);

        if (!archivo.exists()) {
            Toast.makeText(this, "El archivo no existe", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder contenido = new StringBuilder();

        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                contenido.append(linea).append("\n");
            }
            tvContenidoArchivo.setText(contenido.toString());
        } catch (Exception e) {
            tvContenidoArchivo.setText("Error al leer el archivo: " + e.getMessage());
        }
    }
}