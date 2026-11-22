/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Enum.java to edit this template
 */
package avilaos.so.archivos;

/**
 *
 * @author juandiegogodoy
 * Permiso de un archivo o directorio: público (cualquiera puede leerlo) o privado (solo su dueño y el admin).
 */
public enum Permiso {
    PUBLICO("Público", "public"),
    PRIVADO("Privado", "private");

    private final String nombre;
    private final String nombreJson;

    Permiso(String nombre, String nombreJson) {
        this.nombre = nombre;
        this.nombreJson = nombreJson;
    }

    public String getNombreJson() {
        return nombreJson;
    }

    //convierte el texto del JSON (public, private); null si no es valido
    public static Permiso desdeJson(String texto) {
        for (Permiso p : values()) {
            if (texto != null && p.nombreJson.equals(texto.trim())) {
                return p;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
