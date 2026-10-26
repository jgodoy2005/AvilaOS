/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.so.planificacion;
import avilaos.estructuras.Comparador;
import avilaos.so.PCB;
/**
 *
 * @author juandiegogodoy
 */
public class PoliticaPrioridad implements PoliticaPlanificacion {
    @Override
    public TipoPolitica getTipo() {
        return TipoPolitica.PRIORIDAD;
    }
    
    
    //criterio para ordenar los procesos por su prioridad, de menor a mayor
    //si hay empate en prioridad, primero va el que tenga el ciclo limite mas cercano, despues por llegada
    @Override
    public Comparador<PCB> criterio() {
        return (a, b) -> {
            if (a.getPrioridad() != b.getPrioridad()) { 
                return Integer.compare(a.getPrioridad(), b.getPrioridad());   
            }
            if (a.getCicloLimite() != b.getCicloLimite()) {
                return Long.compare(a.getCicloLimite(), b.getCicloLimite());
            }
            return PoliticaPlanificacion.porLlegada(a, b);
        };
    }
    
    
    //expulsa si el primero en listos tiene prioridad menor que el que se ejecuta
     @Override
    public boolean debeExpulsar(PCB enEjecucion, PCB primeroListo, int ciclosEnRafaga) {
        return primeroListo.getPrioridad() < enEjecucion.getPrioridad();
    }
    
    
    //etiqueta para registrar la expulsion
    @Override
    public String razonExpulsion(PCB enEjecucion, PCB primeroListo) {
        return primeroListo.etiqueta() + " tiene mayor prioridad (" + primeroListo.getPrioridad()
                + " contra " + enEjecucion.getPrioridad() + ")";
    }
    
}
