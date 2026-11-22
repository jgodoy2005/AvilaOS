/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package avilaos.hardware;

/**
 *
 * @author juandiegogodoy
 * Lo que el disco necesita saber de una solicitud para atenderla. El hardware no conoce
 * archivos ni procesos: solo una lista de bloques en orden y si debe leerlos o escribirlos.
 * La implementa la Solicitud del sistema operativo.
 */
public interface TrabajoDisco {
    //bloques a visitar en el orden de la cadena; vacio si no toca bloques (operacion sobre un directorio)
    int[] bloques();

    //primer bloque (donde debe ir el cabezal), o -1 si no toca bloques
    default int primerBloque() {
        int[] b = bloques();
        return b.length == 0 ? -1 : b[0];
    }

    //si el disco escribe los bloques (si no, los lee)
    boolean esEscritura();

    //texto para el registro de eventos y la interfaz
    String descripcion();

    //ciclo en que la solicitud llego a la cola del disco
    long cicloLlegada();
}
