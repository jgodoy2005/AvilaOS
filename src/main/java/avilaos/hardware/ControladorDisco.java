/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package avilaos.hardware;

/**
 *
 * @author juandiegogodoy
* Lo que el disco le pide al planificador de disco del sistema operativo (lo implementa
 * PlanificadorDisco). Igual que la CPU con ManejadorInterrupciones: el hardware no depende
 * de las clases del sistema operativo.
 */

public interface ControladorDisco {
    //al empezar cada ciclo del disco: recibir las solicitudes y cancelaciones de ciclos anteriores
    void recibir(long ciclo);

    //el disco esta libre: ¿que hace en este ciclo?
    DecisionDisco siguiente(int cabezal, DireccionCabezal direccion, int ultimoBloque, long ciclo);
    
    void finalizarCiclo(long ciclo);
}
