/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Enum.java to edit this template
 */
package avilaos.so;

/**
 *
 * @author juandiegogodoy
 * Tipos de operación de E/S sobre el disco (enunciado: crear, leer, actualizar, eliminar,
 * suspender y reactivar). Las cuatro primeras las pide un proceso de archivo; las dos últimas,
 * el planificador a mediano plazo.
 */

public enum TipoOperacionES {
    CREAR("Crear", "CREATE", true),
    LEER("Leer", "READ", false),
    ACTUALIZAR("Actualizar", "UPDATE", true),
    ELIMINAR("Eliminar", "DELETE", false),
    SUSPENDER("Suspender", null, true),
    REACTIVAR("Reactivar", null, false);

    private final String nombre;
    private final String nombreJson;   //como se escribe en el JSON de procesos (null si no aplica)
    private final boolean escritura;   //si el disco escribe los bloques (si no, los lee)

    TipoOperacionES(String nombre, String nombreJson, boolean escritura) {
        this.nombre = nombre;
        this.nombreJson = nombreJson;
        this.escritura = escritura;
    }

    public boolean esEscritura() {
        return escritura;
    }

    public String getNombreJson() {
        return nombreJson;
    }

    //operaciones que puede pedir un proceso de archivo (CRUD)
    public boolean esDeArchivo() {
        return this == CREAR || this == LEER || this == ACTUALIZAR || this == ELIMINAR;
    }

    //convierte el texto del JSON (CREATE, READ, UPDATE, DELETE); null si no es valido
    public static TipoOperacionES desdeJson(String texto) {
        if (texto == null) {
            return null;
        }
        for (TipoOperacionES t : values()) {
            if (t.nombreJson != null && t.nombreJson.equals(texto.trim())) {
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