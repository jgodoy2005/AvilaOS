/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Enum.java to edit this template
 */
package avilaos.so.planificacion;
import avilaos.so.Parametros;
/**
 *
 * @author juandiegogodoy
 */

public enum TipoPolitica {
    //politicas disponibles
    FCFS("FCFS"),
    ROUND_ROBIN("Round Robin"),
    PRIORIDAD("Prioridades apropiativas"),
    EDF("EDF");
    
    
    private final String nombre;
    
    
    TipoPolitica(String nombre) {
        this.nombre =nombre;
    }
  
    public PoliticaPlanificacion crear(Parametros parametros){
        return switch(this){
            case FCFS -> new PoliticaFCFS();
            default -> throw new UnsupportedOperationException("Politica no implementada");
        };
    }
    
    @Override 
    public String toString(){
        return nombre;
    }
    
    
}
