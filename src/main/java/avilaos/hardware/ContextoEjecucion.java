/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package avilaos.hardware;

/**
 *
 * @author juandiegogodoy
 */
public interface ContextoEjecucion {
   
    int getPc();

    void avanzarPc();

    void setMar(int direccion);

    ModoCPU modoInstruccionActual();

    String descripcionInstruccionActual();

    String etiqueta();
    
}
