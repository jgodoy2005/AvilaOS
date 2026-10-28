/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.config;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
/**
 *
 * @author erick
 * Gestor de lectura y escritura en disco.
 Convierte instancias de ConfiguracionSimulacion a texto JSON usando Gson.
 */
public final class ArchivoConfiguracion {

    // Instancia reutilizable y segura para uso multihilo (thread-safe)
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ArchivoConfiguracion() {
        // Constructor privado para evitar instanciación
    }

    // Guarda la configuración validando previamente su contenido
    public static void guardar(ConfiguracionSimulacion config, Path archivo) throws IOException {
        config.validar(); // Nunca escribe en disco una configuración inválida
        try (Writer escritor = Files.newBufferedWriter(archivo, StandardCharsets.UTF_8)) {
            GSON.toJson(config, escritor);
        }
    }

    // Lee un archivo JSON desde el disco y valida las reglas del sistema
    public static ConfiguracionSimulacion cargar(Path archivo) throws IOException {
        ConfiguracionSimulacion config;
        try (Reader lector = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
            config = GSON.fromJson(lector, ConfiguracionSimulacion.class);
        } catch (JsonParseException e) {
            throw new IOException("El archivo no es un JSON válido de ÁvilaOS: " + e.getMessage(), e);
        }

        if (config == null) {
            throw new IOException("El archivo está vacío");
        }

        config.validar(); // Se comprueba que los datos numéricos estén en rango
        return config;
    }

    // Métodos auxiliares para conversión directa a String
    public static String aTexto(ConfiguracionSimulacion config) {
        return GSON.toJson(config);
    }

    public static ConfiguracionSimulacion desdeTexto(String json) {
        return GSON.fromJson(json, ConfiguracionSimulacion.class);
    }
}
