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
public class PoliticaEDF implements PoliticaPlanificacion {
    
    
    
    @Override
    public TipoPolitica getTipo() {
        return TipoPolitica.EDF;
    }
    
    
    //criterio para ordenar procesos en listos por su ciclo limite, el que muera primero
    //si hay empate primero se comparan prioridades, si empatan de nuevo por llegada
    @Override
    public Comparador<PCB> criterio() {
        return (a, b) -> {
            if (a.getCicloLimite() != b.getCicloLimite()) {
                return Long.compare(a.getCicloLimite(), b.getCicloLimite());
            }
            if (a.getPrioridad() != b.getPrioridad()) {
                return Integer.compare(a.getPrioridad(), b.getPrioridad());
            }
            return PoliticaPlanificacion.porLlegada(a, b);
        };
    }
    
    
   //expulsa si el primero de listos muere primero que el que esta en ejecucion
    @Override
    public boolean debeExpulsar(PCB enEjecucion, PCB primeroListo, int ciclosEnRafaga) {
        return primeroListo.getCicloLimite() < enEjecucion.getCicloLimite();
    }

    
    //etiqueta para registrar la expulsion
    @Override
    public String razonExpulsion(PCB enEjecucion, PCB primeroListo) {
        return primeroListo.etiqueta() + " vence antes (ciclo " + primeroListo.getCicloLimite()
                + " contra " + enEjecucion.getCicloLimite() + ")";
    }
    
}
