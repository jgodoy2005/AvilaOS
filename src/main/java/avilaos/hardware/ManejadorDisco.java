/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package avilaos.hardware;

/**
 *
 * @author juandiegogodoy
 * Avisos que recibe el sistema operativo sobre el disco (lo implementa el Kernel).
 * Se llaman desde el hilo del disco, así que el Kernel los guarda en un buzón y los
 * procesa en el ciclo siguiente.
 */
public interface ManejadorDisco {
     //el disco termino de atender un trabajo (interrupcion de fin de E/S)
    void trabajoTerminado(TrabajoDisco trabajo, long ciclo);

    //el planificador saco de la cola un trabajo que todavia no empezaba (por ejemplo, por deadline)
    void trabajoCancelado(TrabajoDisco trabajo, long ciclo);
}
