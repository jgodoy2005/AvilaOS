/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Enum.java to edit this template
 */
package avilaos.so.archivos;

/**
 *
 * @author juandiegogodoy
 * Cada elemento del sistema de archivos es un archivo (ocupa bloques) o un directorio (no ocupa).
 */
public enum TipoNodo {
    ARCHIVO("Archivo", "FILE"),
    DIRECTORIO("Directorio", "DIRECTORY");

    private final String nombre;
    private final String nombreJson;

    TipoNodo(String nombre, String nombreJson) {
        this.nombre = nombre;
        this.nombreJson = nombreJson;
    }

    public String getNombreJson() {
        return nombreJson;
    }

    //convierte el texto del JSON (FILE, DIRECTORY); null si no es valido
    public static TipoNodo desdeJson(String texto) {
        for (TipoNodo t : values()) {
            if (texto != null && t.nombreJson.equals(texto.trim())) {
                return t;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
