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
public class PoliticaFCFS implements PoliticaPlanificacion{
    
    
    @Override
    public TipoPolitica getTipo(){
        return TipoPolitica.FCFS;
    }
    
    @Override
    // por orden de llegada a la cola de listos
    public Comparador<PCB> criterio() {
        return PoliticaPlanificacion::porLlegada;   
    }
    
    @Override
    // No apropiativa, nadie le quita la CPU a quien la tiene 
    public boolean debeExpulsar(PCB enEjecucion, PCB primeroListo, int ciclosEnRafaga) {
        return false;   
    }
}
