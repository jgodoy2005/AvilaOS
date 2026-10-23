/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Enum.java to edit this template
 */
package avilaos.so.planificacion;

/**
 *
 * @author juandiegogodoy
 */

/*
ESQUELETO PARA LOS TIPOS DE POLITICA
*/

public enum TipoPolitica {
    
    FCFS("FCFS"),
    ROUND_ROBIN("Round Robin"),
    PRIORIDAD("Prioridades apropiativas"),
    EDF("EDF");
    
    
    private final String nombre;
    TipoPolitica(String nombre) {this.nombre =nombre;}
    @Override public String toString(){return nombre;}
    
}
