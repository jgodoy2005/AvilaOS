/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Record.java to edit this template
 */
package avilaos.simulacion;

/**
 *
 * @author juandiegogodoy
 * Una fila de la tabla de asignación de archivos.
 */
public record FotoArchivo(
    String nombre,
    String ruta,
    int cantidadBloques,
    int primerBloque,
    String dueno,
    String creador,
    int color) {
}
