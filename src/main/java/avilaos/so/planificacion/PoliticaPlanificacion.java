/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package avilaos.so.planificacion;
import avilaos.estructuras.Comparador;
import avilaos.so.PCB;
/**
 *
 * @author juandiegogodoy
 */
public interface PoliticaPlanificacion {
    
    TipoPolitica getTipo();
    
    //orden de la cola de listos 
    Comparador<PCB> criterio();
    
    //booleano para determinar si hay que quitar o no el CPU al proceso en ejecucion
    //dependiendo de la politica de planificacion
    boolean debeExpulsar(PCB enEjecucion, PCB primeroListo, int ciclosEnRafaga);
    
    //descripcion para el registro de eventos cuando hay una expulsion
    default String razonExpulsion(PCB enEjecucion, PCB primeroListo) {
        return "lo indica la política " + getTipo();
    }
    
    //desempate comun por orden de llegada
    static int porLlegada(PCB a, PCB b) {
        return Long.compare(a.getOrdenLlegadaListos(), b.getOrdenLlegadaListos());
    }
}
