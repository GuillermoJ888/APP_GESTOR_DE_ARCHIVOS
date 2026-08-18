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
    private Button btnCompartir;
    private Button btnLimpiar;
    private ListView lvArchivos;
    private TextView tvContenidoArchivo;

    private ArrayList<String> listaArchivos;
    private ArrayList<String> listaArchivosFiltrados;
    private ArrayList<String> listaMostrar;
    private ArrayAdapter<String> adaptadorArchivos;
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
        btnCompartir = findViewById(R.id.btnCompartir);
        btnLimpiar = findViewById(R.id.btnLimpiar);
        lvArchivos = findViewById(R.id.lvArchivos);
        tvContenidoArchivo = findViewById(R.id.tvContenidoArchivo);

        listaArchivos = new ArrayList<>();
        listaArchivosFiltrados = new ArrayList<>();
        listaMostrar = new ArrayList<>();

        adaptadorArchivos = new ArrayAdapter<>(
                this,
                R.layout.item_archivo,
                listaMostrar
        );

        lvArchivos.setAdapter(adaptadorArchivos);

        // Evento del botón Guardar
        btnGuardar.setOnClickListener(v -> guardarArchivo());

        // Evento del botón Compartir
        btnCompartir.setOnClickListener(v -> {
            if (archivoSeleccionado == null) {
                Toast.makeText(this, "Selecciona un archivo primero", Toast.LENGTH_SHORT).show();
                return;
            }
            compartirArchivo(archivoSeleccionado);
        });

        // Evento del botón Limpiar
        btnLimpiar.setOnClickListener(v -> {
            etNombreArchivo.setText("");
            etContenido.setText("");
            etNombreArchivo.setError(null);
        });

        // Evento al seleccionar un archivo de la lista
        lvArchivos.setOnItemClickListener((parent, view, position, id) -> {
            String nombreArchivo = listaArchivosFiltrados.get(position);
            archivoSeleccionado = nombreArchivo;
            leerArchivo(nombreArchivo);
        });

        // Evento de búsqueda
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

        listarArchivos();
    }

    private void guardarArchivo() {
        String nombreArchivo = etNombreArchivo.getText().toString().trim();
        String contenido = etContenido.getText().toString();

        if (nombreArchivo.isEmpty()) {
            etNombreArchivo.setError("Escribe el nombre del archivo");
            etNombreArchivo.requestFocus();
            return;
        }

        if (!nombreArchivo.toLowerCase().endsWith(".txt")) {
            nombreArchivo = nombreArchivo + ".txt";
        }

        String estadoAlmacenamiento = Environment.getExternalStorageState();

        if (!Environment.MEDIA_MOUNTED.equals(estadoAlmacenamiento)) {
            Toast.makeText(this, "El almacenamiento externo no está disponible", Toast.LENGTH_LONG).show();
            return;
        }

        File carpeta = getExternalFilesDir(null);

        if (carpeta == null) {
            Toast.makeText(this, "No fue posible acceder al almacenamiento", Toast.LENGTH_LONG).show();
            return;
        }

        if (archivoExiste(carpeta, nombreArchivo)) {
            Toast.makeText(this, "Ya existe un archivo con ese nombre", Toast.LENGTH_SHORT).show();
            return;
        }

        File archivo = new File(carpeta, nombreArchivo);

        try (FileOutputStream fos = new FileOutputStream(archivo)) {
            fos.write(contenido.getBytes(StandardCharsets.UTF_8));
            Toast.makeText(this, "Archivo guardado: " + nombreArchivo, Toast.LENGTH_SHORT).show();

            etNombreArchivo.setText("");
            etContenido.setText("");

            listarArchivos();

        } catch (Exception e) {
            Toast.makeText(this, "Error al guardar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private boolean archivoExiste(File carpeta, String nombreArchivo) {
        File archivo = new File(carpeta, nombreArchivo);
        return archivo.exists();
    }

    private String formatearTamano(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String unidad = "KMGTPE".charAt(exp - 1) + "B";
        return String.format(Locale.getDefault(), "%.1f %s", bytes / Math.pow(1024, exp), unidad);
    }

    private void listarArchivos() {
        listaArchivos.clear();
        archivoSeleccionado = null;

        File carpeta = getExternalFilesDir(null);

        if (carpeta == null) {
            Toast.makeText(this, "No fue posible acceder a la carpeta", Toast.LENGTH_SHORT).show();
            adaptadorArchivos.notifyDataSetChanged();
            return;
        }

        File[] archivos = carpeta.listFiles();

        if (archivos != null) {
            Arrays.sort(archivos, new Comparator<File>() {
                @Override
                public int compare(File f1, File f2) {
                    return f1.getName().compareToIgnoreCase(f2.getName());
                }
            });

            for (File f : archivos) {
                if (f.isFile()) {
                    listaArchivos.add(f.getName());
                }
            }
        }

        String textoBusqueda = etBuscar.getText().toString();
        filtrarArchivos(textoBusqueda);
    }

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