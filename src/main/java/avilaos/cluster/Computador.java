/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.cluster;

import avilaos.hardware.CPU;
import avilaos.hardware.ComponenteReloj;
import avilaos.hardware.DispositivoES;
import avilaos.hardware.Memoria;
import avilaos.so.Kernel;
import avilaos.so.Parametros;
import avilaos.so.RegistroEventos;
import avilaos.so.planificacion.TipoPolitica;

/**
 *
 * @author juandiegogodoy
 */
public class Computador implements ComponenteReloj {
    
    //cada parte de un computador por si solo 
    private final int numero; //numero identificador del computador dentro del cluster
    private final CPU cpu = new CPU(); // la cpu del computador
    private final Memoria memoria; //ram del computador
    private final DispositivoES dispositivo = new DispositivoES(); //dispositivo ES (dma) de este computador
    private final Kernel kernel; //el so que maneja a este computador

    public Computador(int numero, int ramMB, TipoPolitica politicaInicial,
                      Parametros parametros, RegistroEventos registro) {
        this.numero = numero;
        this.memoria = new Memoria(ramMB);
        this.kernel = new Kernel(numero, cpu, memoria, dispositivo,
                politicaInicial.crear(parametros), parametros, registro);
        //conexiones con el so para avisar de ciclos completados exitosamente o interrupciones/traps     
        cpu.conectar(kernel);     
        dispositivo.conectar(kernel);
    }
    
    //Lo que hace el computador (cada uno de sus componentes) en cada ciclo al ser llamada por el reloj global
    @Override
    public void tic(long ciclo) {
        dispositivo.tic(ciclo);            // el DMA avanza; puede interrumpir al kernel
        kernel.antesDeEjecutar(ciclo);     // el SO: red, desbloqueos, deadlines, admisión y despacho
        cpu.tic(ciclo);                    // la CPU ejecuta exactamente una instrucción (o despacha, o está ociosa)
        kernel.despuesDeEjecutar(ciclo);   // contabilidad del ciclo
    }
    
    public int getNumero() {
        return numero;
    }

    public String getNombre() {
        return kernel.getNombre();
    }

    public Kernel getKernel() {
        return kernel;
    }

    public Memoria getMemoria() {
        return memoria;
    }

    public CPU getCpu() {
        return cpu;
    }

    public DispositivoES getDispositivo() {
        return dispositivo;
    }
    
}
