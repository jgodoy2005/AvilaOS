/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Record.java to edit this template
 */
package avilaos.simulacion;

import avilaos.so.archivos.Permiso;
import avilaos.so.archivos.TipoNodo;

/**
 *
 * @author juandiegogodoy
 *Copia de solo lectura de un archivo o directorio, con sus hijos, para el JTree.
 */
public record FotoNodo(
    String nombre,
    String ruta,
    TipoNodo tipo,
    String dueno,
    Permiso permiso,
    int primerBloque,
    int cantidadBloques,
    String creador,
    int color,
    FotoNodo[] hijos) {
    
    @Override
    public String toString() {
        return nombre;   //lo que muestra el JTree
    }

}
