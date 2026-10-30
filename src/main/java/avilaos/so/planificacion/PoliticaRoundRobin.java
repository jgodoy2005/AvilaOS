/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.so.planificacion;
import avilaos.estructuras.Comparador;
import avilaos.so.PCB;
import avilaos.so.Parametros;
/**
 *
 * @author juandiegogodoy
 */
public class PoliticaRoundRobin implements PoliticaPlanificacion {
    private final Parametros parametros;
    
    public PoliticaRoundRobin(Parametros parametros) {
        this.parametros = parametros;
    }
    
    
    @Override
    public TipoPolitica getTipo() {
        return TipoPolitica.ROUND_ROBIN;
    }
    
    
    //ordenar las rondas por orden de llegada
    @Override
    public Comparador<PCB> criterio() {
        return PoliticaPlanificacion::porLlegada;
    }

    //expulsar al acabar el quantum de la ronda
    @Override
    public boolean debeExpulsar(PCB enEjecucion, PCB primeroListo, int ciclosEnRafaga) {
        int quantum = parametros.getQuantum();
        return ciclosEnRafaga > 0 && ciclosEnRafaga % quantum == 0;   // justo al cumplir un quantum
    }

    //etiqueta para registrar a quien se expulso
    @Override
    public String razonExpulsion(PCB enEjecucion, PCB primeroListo) {
        return "se le acabó el quantum de " + parametros.getQuantum() + " ciclos";
    }
}
