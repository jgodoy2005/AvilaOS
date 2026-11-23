/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.so.disco;

import avilaos.hardware.Bloque;
import avilaos.hardware.Disco;
/**
 *
 * @author erick
 * Área de intercambio (swap): la zona del disco que va después de la de archivos. Un proceso
 * suspendido ocupa tantos bloques como MB de memoria tiene (1 MB = 1 bloque). Como en los
 * archivos, se toman los primeros bloques libres y quedan encadenados. Solo la usa el kernel.
 */
public class AreaIntercambio {

    private final Disco disco;

    public AreaIntercambio(Disco disco) {
        this.disco = disco;
    }

    //reserva los bloques para la imagen de un proceso; null si no caben
    public int[] reservar(String proceso, int cantidad) {
        int[] bloques = disco.primerosLibres(cantidad, true);
        if (bloques == null) {
            return null;
        }
        for (int i = 0; i < bloques.length; i++) {
            disco.ocupar(bloques[i], proceso + " (suspendido)", -1, i + 1 < bloques.length ? bloques[i + 1] : Bloque.FIN);
        }
        return bloques;
    }

    public void liberar(int[] bloques) {
        for (int b : bloques) {
            disco.liberar(b);
        }
    }

    public int libres() {
        return disco.libresEnZona(true);
    }

    public int capacidad() {
        return disco.getBloquesSwap();
    }
}
