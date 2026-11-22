/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Record.java to edit this template
 */
package avilaos.so;

import avilaos.so.archivos.Permiso;
import avilaos.so.archivos.TipoNodo;

/**
 *
 * @author juandiegogodoy
* Lo que un proceso de E/S de disco quiere hacer con el sistema de archivos.
 *
 * @param operacion   crear, leer, actualizar o eliminar
 * @param ruta        ruta del archivo o directorio (para crear: la ruta del elemento nuevo)
 * @param tipoNodo    al crear: archivo o directorio
 * @param bloques     al crear un archivo: su tamaño en bloques
 * @param permiso     al crear: público o privado (null = público)
 * @param dueno       al crear: dueño del elemento nuevo (null = quien lanzó el proceso)
 * @param nombreNuevo al actualizar: el nombre nuevo (null si no viene, como en el JSON del enunciado)
 * @param usuario     quien lanzó el proceso ("admin" o userN), para revisar permisos
 */


public record OperacionArchivo(
    TipoOperacionES operacion,
    String ruta,
    TipoNodo tipoNodo,
    int bloques,
    Permiso permiso,
    String dueno,
    String nombreNuevo,
    String usuario) {
    
    
     public static OperacionArchivo leer(String ruta, String usuario) {
        return new OperacionArchivo(TipoOperacionES.LEER, ruta, null, 0, null, null, null, usuario);
    }

    public static OperacionArchivo eliminar(String ruta, String usuario) {
        return new OperacionArchivo(TipoOperacionES.ELIMINAR, ruta, null, 0, null, null, null, usuario);
    }

    public static OperacionArchivo renombrar(String ruta, String nombreNuevo, String usuario) {
        return new OperacionArchivo(TipoOperacionES.ACTUALIZAR, ruta, null, 0, null, null, nombreNuevo, usuario);
    }

    public static OperacionArchivo crearArchivo(String ruta, int bloques, Permiso permiso, String dueno, String usuario) {
        return new OperacionArchivo(TipoOperacionES.CREAR, ruta, TipoNodo.ARCHIVO, bloques, permiso, dueno, null, usuario);
    }

    public static OperacionArchivo crearDirectorio(String ruta, Permiso permiso, String dueno, String usuario) {
        return new OperacionArchivo(TipoOperacionES.CREAR, ruta, TipoNodo.DIRECTORIO, 0, permiso, dueno, null, usuario);
    }

    //texto corto para el registro y el PCB: "Leer /sistema/boot.bin"
    public String descripcion() {
        return operacion + " " + ruta + (nombreNuevo != null ? " a " + nombreNuevo : "");
    }
}
