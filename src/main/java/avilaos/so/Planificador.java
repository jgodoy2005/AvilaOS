/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.so;
import avilaos.estructuras.Cola;
import avilaos.so.planificacion.PoliticaPlanificacion;

/**
 *
 * @author juandiegogodoy
 */
public class Planificador {
    private PoliticaPlanificacion politica;
    
     public Planificador(PoliticaPlanificacion politica) {
        this.politica = politica;
    }

    public PoliticaPlanificacion getPolitica() {
        return politica;
    }

    public void setPolitica(PoliticaPlanificacion politica) {
        this.politica = politica;
    }

    
    //cambiar el orden de la cola de listos segun el criterio de la politica
    public void ordenar(Cola<PCB> listos) {
        listos.ordenar(politica.criterio());
    }
    
    
    //elegir al proximo proceso de listos, se saca y reordena
    public PCB seleccionar(Cola<PCB> listos){
        ordenar(listos);
        return listos.desencolar();
    }
    
    
    //definir si se debe sacar al primero proceso en ejecucion de CPU por el primero
    //de listos, segun la politica
    public boolean debeExpulsar(PCB enEjecucion, Cola<PCB> listos, int ciclosEnRafaga){
        ordenar(listos);
        PCB primero = listos.verPrimero();
        return enEjecucion != null && primero != null && politica.debeExpulsar(enEjecucion, primero, ciclosEnRafaga);        
            
    }
    
}
