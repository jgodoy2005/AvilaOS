/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.so.disco;

import avilaos.hardware.TrabajoDisco;
import avilaos.so.PCB;
import avilaos.so.TipoOperacionES;
/**
 *
 * @author juandiegogodoy
 * Una solicitud de E/S al disco: quién la pide, qué operación es y qué bloques hay que
 * visitar, en el orden de la cadena. La arma el sistema operativo y la atiende el disco.
 */
public class Solicitud implements TrabajoDisco {
    
     private final PCB proceso;
    private final TipoOperacionES operacion;
    private final String descripcion;
    private final int[] bloques;
    private long cicloLlegada = -1;

    public Solicitud(PCB proceso, TipoOperacionES operacion, String descripcion, int[] bloques) {
        this.proceso = proceso;
        this.operacion = operacion;
        this.descripcion = descripcion;
        this.bloques = bloques;
    }

    //el kernel la marca al mandarla a la cola del disco
    public void marcarLlegada(long ciclo) {
        this.cicloLlegada = ciclo;
    }

    @Override
    public int[] bloques() {
        return bloques;
    }

    @Override
    public boolean esEscritura() {
        return operacion.esEscritura();
    }

    @Override
    public String descripcion() {
        return descripcion;
    }

    @Override
    public long cicloLlegada() {
        return cicloLlegada;
    }

    //como se nombra en el registro: la etiqueta del proceso, o la descripcion si no tiene proceso
    public String etiqueta() {
        return proceso != null ? proceso.etiqueta() : descripcion;
    }

    public PCB getProceso() { return proceso; }
    public TipoOperacionES getOperacion() { return operacion; }
}
