package com.example.gestorarchivos;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
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
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // Elementos de la interfaz
    private EditText etNombreArchivo;
    private EditText etContenido;
    private EditText etBuscar;
    private Button btnGuardar;
    private Button btnListar;
    private Button btnCompartir;
    private ListView lvArchivos;
    private TextView tvContenidoArchivo;

    // Lista con los nombres REALES de TODOS los archivos (sin filtrar)
    private ArrayList<String> listaArchivos;

    // Lista con los nombres REALES que se están mostrando actualmente (puede estar filtrada)
    private ArrayList<String> listaArchivosFiltrados;

    // Lista con el texto que se MUESTRA en el ListView (nombre + tamaño)
    private ArrayList<String> listaMostrar;

    // Adaptador para mostrar los archivos en el ListView
    private ArrayAdapter<String> adaptadorArchivos;

    // Guarda cuál archivo está seleccionado actualmente (para el botón Compartir)
    private String archivoSeleccionado = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Vinculación de elementos XML con Java
        etNombreArchivo = findViewById(R.id.etNombreArchivo);
        etContenido = findViewById(R.id.etContenido);
        etBuscar = findViewById(R.id.etBuscar);
        btnGuardar = findViewById(R.id.btnGuardar);
        btnListar = findViewById(R.id.btnListar);
        btnCompartir = findViewById(R.id.btnCompartir);
        lvArchivos = findViewById(R.id.lvArchivos);
        tvContenidoArchivo = findViewById(R.id.tvContenidoArchivo);

        // Inicialización de las listas
        listaArchivos = new ArrayList<>();
        listaArchivosFiltrados = new ArrayList<>();
        listaMostrar = new ArrayList<>();

        // Creación del adaptador (usa listaMostrar, que trae nombre + tamaño)
        adaptadorArchivos = new ArrayAdapter<>(
                this,
                R.layout.item_archivo,
                listaMostrar
        );

        // Asociar el adaptador con el ListView
        lvArchivos.setAdapter(adaptadorArchivos);

        // Evento del botón Guardar
        btnGuardar.setOnClickListener(v -> guardarArchivo());

        // Evento del botón Listar
        btnListar.setOnClickListener(v -> listarArchivos());

        // Evento del botón Compartir
        btnCompartir.setOnClickListener(v -> {
            if (archivoSeleccionado == null) {
                Toast.makeText(this, "Selecciona un archivo primero", Toast.LENGTH_SHORT).show();
                return;
            }
            compartirArchivo(archivoSeleccionado);
        });

        // Evento al seleccionar un archivo de la lista (clic = leer y marcar como seleccionado)
        lvArchivos.setOnItemClickListener((parent, view, position, id) -> {
            String nombreArchivo = listaArchivosFiltrados.get(position);
            archivoSeleccionado = nombreArchivo;
            leerArchivo(nombreArchivo);
        });

        // Evento de búsqueda: filtra la lista mientras el usuario escribe
        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filtrarArchivos(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Mostrar los archivos existentes al abrir la aplicación
        listarArchivos();
    }

    /**
     * Guarda un archivo de texto en el almacenamiento
     * externo privado de la aplicación.
     * Valida que no exista ya un archivo con el mismo nombre.
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
        File carpeta = getExternalFilesDir(null);

        if (carpeta == null) {
            Toast.makeText(
                    this,
                    "No fue posible acceder al almacenamiento",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        // F) EVITAR NOMBRES DUPLICADOS
        if (archivoExiste(carpeta, nombreArchivo)) {
            Toast.makeText(
                    this,
                    "Ya existe un archivo con ese nombre",
                    Toast.LENGTH_SHORT
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
     * Verifica si ya existe un archivo con ese nombre en la carpeta dada.
     */
    private boolean archivoExiste(File carpeta, String nombreArchivo) {
        File archivo = new File(carpeta, nombreArchivo);
        return archivo.exists();
    }

    /**
     * Convierte un tamaño en bytes a un texto legible (B, KB, MB...).
     */
    private String formatearTamano(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String unidad = "KMGTPE".charAt(exp - 1) + "B";
        return String.format(Locale.getDefault(), "%.1f %s", bytes / Math.pow(1024, exp), unidad);
    }

    /**
     * Obtiene los archivos almacenados en la carpeta
     * externa privada, los ordena por nombre, calcula su tamaño
     * y los muestra en el ListView.
     */
    private void listarArchivos() {

        // Limpiar la lista maestra
        listaArchivos.clear();
        archivoSeleccionado = null;

        // Obtener la carpeta privada de la aplicación
        File carpeta = getExternalFilesDir(null);

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
            // ORDENAR POR NOMBRE (sin distinguir mayúsculas/minúsculas)
            Arrays.sort(archivos, new Comparator<File>() {
                @Override
                public int compare(File f1, File f2) {
                    return f1.getName().compareToIgnoreCase(f2.getName());
                }
            });

            // Agregar únicamente archivos, no carpetas
            for (File f : archivos) {
                if (f.isFile()) {
                    listaArchivos.add(f.getName());
                }
            }
        }

        // Vuelve a aplicar el filtro de búsqueda actual (si hay texto en etBuscar)
        String textoBusqueda = etBuscar.getText().toString();
        filtrarArchivos(textoBusqueda);
    }

    /**
     * Filtra listaArchivos según el texto de búsqueda y actualiza
     * listaArchivosFiltrados + listaMostrar (nombre + tamaño).
     *
     * @param textoBusqueda texto escrito en el campo de búsqueda
     */
    private void filtrarArchivos(String textoBusqueda) {
        listaArchivosFiltrados.clear();
        listaMostrar.clear();

        File carpeta = getExternalFilesDir(null);

        for (String nombre : listaArchivos) {
            if (nombre.toLowerCase().contains(textoBusqueda.toLowerCase())) {
                listaArchivosFiltrados.add(nombre);

                if (carpeta != null) {
                    File f = new File(carpeta, nombre);
                    listaMostrar.add(nombre + "  (" + formatearTamano(f.length()) + ")");
                } else {
                    listaMostrar.add(nombre);
                }
            }
        }

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

    /**
     * Comparte el contenido de un archivo .txt mediante
     * un Intent implícito (WhatsApp, Gmail, etc.).
     *
     * @param nombreArchivo nombre del archivo a compartir
     */
    private void compartirArchivo(String nombreArchivo) {
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
        } catch (Exception e) {
            Toast.makeText(this, "Error al leer el archivo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, nombreArchivo);
        intent.putExtra(Intent.EXTRA_TEXT, contenido.toString());
        startActivity(Intent.createChooser(intent, "Compartir archivo vía"));
    }
}
