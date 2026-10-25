/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.so.sincronizacion;

import avilaos.estructuras.Cola;
import avilaos.so.MotivoBloqueo;
import avilaos.so.PCB;
/**
 *
 * @author juandiegogodoy
 * 
 */

/**
 * 
 * @author erick
 * Semaforo de AvilaOS, un contador y una cola FIFO de procesos(semaforo fuerte)
 * Si el valor es negativo su valor absoluto es cuantos procesos esperan.
 */

public class SemaforoSO {
    
    private final String nombre;               // "s" , "n" o "e"
    private final String descripcion;          // "mutex", "llenos" o "vacios"
    private final MotivoBloqueo motivo;     // motivo de bloqueo de quien espera aqui
    private int valor;
    private final Cola<PCB> cola = new Cola<>();
    
    public SemaforoSO(String nombre, String descripcion, int valorInicial, 
MotivoBloqueo motivo ) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.valor = valorInicial;
        this.motivo = motivo;
    }
    
    /** semWait: devuelve true si el proceso debe bloquearse.*/
    boolean semWait(PCB proceso){
        valor--;
        if (valor < 0) {
            cola.encolar(proceso);
            proceso.setEsperandoEn(this);
            return true;
        }
        return false;
    }
   
    /** devuelve el proceso que despierta, o null si no habia nadie esperando*/
    PCB semSignal(){
        valor++;
        if (valor <=0 ) {
        PCB despertado = cola.desencolar();
        if (despertado != null) {
            despertado.setEsperandoEn(null);
        }
        return despertado;
    }
    return null;
}

    /** Saca de la cola a un proceso que ,urio esperando y deshace su semWait*/
    boolean retirar (PCB proceso) {
        if (cola.eliminar(proceso)) {
            valor++;
            proceso.setEsperandoEn(null);
            return true;
        }
        return false;
    }
    
    public String getNombre() {
        return nombre;
    }
    
    public String getDescripcion() {
        return descripcion;
    }
    
    public MotivoBloqueo getMotivo() {
        return motivo;
    }
    
    public int getValor() {
        return valor;
    }
    
    
    /**Etiquetas de los procesos que esperan, en orden*/
    public String[] procesosEnEspera() {
        PCB[] enCola = cola.aArreglo(new PCB[cola.tamano()]);
        String[] etiquetas = new String[enCola.length];
        for (int i = 0; i < enCola.length; i++) {
            etiquetas[i] = enCola[i].etiqueta();
        }
        return etiquetas;
    }
    
    @Override
    public String toString() {
        return nombre + "(" + descripcion + ")";
    }
}





