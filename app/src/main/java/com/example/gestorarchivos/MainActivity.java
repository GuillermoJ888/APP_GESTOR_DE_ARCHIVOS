package com.example.gestorarchivos;

import android.content.Intent;
import android.net.Uri;
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
import androidx.core.content.FileProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // Elementos de la interfaz
    private EditText etNombreArchivo;
    private EditText etContenido;
    private EditText etBuscar;
    private Button btnGuardar;
    private Button btnCompartir;
    private Button btnLimpiar;
    private Button btnPagAnterior;
    private Button btnPagSiguiente;
    private TextView tvPagina;
    private ListView lvArchivos;
    private TextView tvContenidoArchivo;

    // Lista con TODOS los nombres reales (sin filtrar, sin paginar)
    private ArrayList<String> listaArchivos;

    // Lista con los nombres reales que cumplen la búsqueda (antes de paginar)
    private ArrayList<String> listaArchivosFiltrados;

    // Lista de nombres reales que se muestran en la página actual (para el clic)
    private ArrayList<String> listaArchivosPaginaActual;

    // Lista con el texto que se MUESTRA en el ListView (nombre + tamaño) de la página actual
    private ArrayList<String> listaMostrar;

    private ArrayAdapter<String> adaptadorArchivos;
    private String archivoSeleccionado = null;

    // Paginación
    private static final int TAMANO_PAGINA = 5;
    private int paginaActual = 0;

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
        btnPagAnterior = findViewById(R.id.btnPagAnterior);
        btnPagSiguiente = findViewById(R.id.btnPagSiguiente);
        tvPagina = findViewById(R.id.tvPagina);
        lvArchivos = findViewById(R.id.lvArchivos);
        tvContenidoArchivo = findViewById(R.id.tvContenidoArchivo);

        listaArchivos = new ArrayList<>();
        listaArchivosFiltrados = new ArrayList<>();
        listaArchivosPaginaActual = new ArrayList<>();
        listaMostrar = new ArrayList<>();

        adaptadorArchivos = new ArrayAdapter<>(
                this,
                R.layout.item_archivo,
                listaMostrar
        );

        lvArchivos.setAdapter(adaptadorArchivos);

        // Evento del botón Guardar
        btnGuardar.setOnClickListener(v -> guardarArchivo());

        // Evento del botón Compartir (comparte el ARCHIVO real, no solo el texto)
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

        // Evento al seleccionar un archivo de la lista (usa la lista de la página actual)
        lvArchivos.setOnItemClickListener((parent, view, position, id) -> {
            String nombreArchivo = listaArchivosPaginaActual.get(position);
            archivoSeleccionado = nombreArchivo;
            leerArchivo(nombreArchivo);
        });

        // Botones de paginación
        btnPagAnterior.setOnClickListener(v -> {
            if (paginaActual > 0) {
                paginaActual--;
                mostrarPaginaActual();
            }
        });

        btnPagSiguiente.setOnClickListener(v -> {
            int totalPaginas = calcularTotalPaginas();
            if (paginaActual < totalPaginas - 1) {
                paginaActual++;
                mostrarPaginaActual();
            }
        });

        // Evento de búsqueda: al buscar, siempre regresa a la página 1
        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                paginaActual = 0;
                filtrarArchivos(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        listarArchivos();
    }

    /**
     * Guarda un archivo de texto en el almacenamiento externo privado.
     * Valida que no exista ya un archivo con el mismo nombre.
     */
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

    /**
     * Relee todos los archivos de la carpeta, los ordena, y vuelve a aplicar
     * el filtro de búsqueda actual (esto recalcula la paginación).
     */
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

    /**
     * Filtra listaArchivos según el texto de búsqueda y actualiza listaArchivosFiltrados.
     * Luego muestra la página actual sobre ese resultado filtrado.
     */
    private void filtrarArchivos(String textoBusqueda) {
        listaArchivosFiltrados.clear();

        for (String nombre : listaArchivos) {
            if (nombre.toLowerCase().contains(textoBusqueda.toLowerCase())) {
                listaArchivosFiltrados.add(nombre);
            }
        }

        // Si la página actual quedó fuera de rango (ej. al borrar archivos), ajusta
        int totalPaginas = calcularTotalPaginas();
        if (paginaActual >= totalPaginas) {
            paginaActual = Math.max(0, totalPaginas - 1);
        }

        mostrarPaginaActual();
    }

    private int calcularTotalPaginas() {
        if (listaArchivosFiltrados.isEmpty()) return 1;
        return (int) Math.ceil(listaArchivosFiltrados.size() / (double) TAMANO_PAGINA);
    }

    /**
     * Toma el "recorte" de listaArchivosFiltrados correspondiente a la página
     * actual y lo muestra en el ListView, junto con el nombre + tamaño.
     */
    private void mostrarPaginaActual() {
        listaArchivosPaginaActual.clear();
        listaMostrar.clear();

        File carpeta = getExternalFilesDir(null);

        int inicio = paginaActual * TAMANO_PAGINA;
        int fin = Math.min(inicio + TAMANO_PAGINA, listaArchivosFiltrados.size());

        for (int i = inicio; i < fin; i++) {
            String nombre = listaArchivosFiltrados.get(i);
            listaArchivosPaginaActual.add(nombre);

            if (carpeta != null) {
                File f = new File(carpeta, nombre);
                listaMostrar.add(nombre + "  (" + formatearTamano(f.length()) + ")");
            } else {
                listaMostrar.add(nombre);
            }
        }

        adaptadorArchivos.notifyDataSetChanged();

        int totalPaginas = calcularTotalPaginas();
        tvPagina.setText("Página " + (paginaActual + 1) + " de " + totalPaginas);

        btnPagAnterior.setEnabled(paginaActual > 0);
        btnPagSiguiente.setEnabled(paginaActual < totalPaginas - 1);
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

    /**
     * Comparte el ARCHIVO REAL (.txt) como adjunto, usando FileProvider,
     * en vez de solo mandar el texto como copiar-pegar.
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

        Uri uriArchivo = FileProvider.getUriForFile(
                this,
                getPackageName() + ".fileprovider",
                archivo
        );

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_STREAM, uriArchivo);
        intent.putExtra(Intent.EXTRA_SUBJECT, nombreArchivo);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(intent, "Compartir archivo vía"));
    }
}