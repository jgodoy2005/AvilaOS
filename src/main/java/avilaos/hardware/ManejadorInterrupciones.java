/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package avilaos.hardware;

/**
 *
 * @author juandiegogodoy
 */
public interface ManejadorInterrupciones {
    
    void instruccionCompletada(long ciclo);
    
    void llamadaAlSistema(long ciclo);
    
    void finDeES(ContextoEjecucion proceso, long ciclo);
}
