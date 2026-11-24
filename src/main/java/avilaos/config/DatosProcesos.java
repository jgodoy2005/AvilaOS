/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Record.java to edit this template
 */
package avilaos.config;

import avilaos.config.ConfiguracionSimulacion.ConfigBuffer;
import avilaos.config.ConfiguracionSimulacion.ConfigComputador;
import avilaos.so.DatosProceso;
import avilaos.so.disco.TipoPoliticaDisco;
import avilaos.so.planificacion.TipoPolitica;


/**
 *
 * @author juandiegogodoy
 */

 // duracionCicloMs duración de cada ciclo del reloj
 // bloquesSwap     tamaño del área de intercambio
 // politicaDisco   política de disco inicial
 // ramMB           RAM del computador
 // politica        política de planificación de la CPU
 // quantum         quantum de Round Robin (si la política es otra, el del JSON no se usa)
 // procesos        procesos de E/S de disco, en el orden del JSON
 
public record DatosProcesos(
    int duracionCicloMs,
    int bloquesSwap,
    TipoPoliticaDisco politicaDisco,
    int ramMB,
    TipoPolitica politica,
    int quantum,
    DatosProceso[] procesos) {

    //configuracion del Proyecto 1 con un solo computador, sin buffers ni latencia de red
    public ConfiguracionSimulacion aConfiguracion() {
        return new ConfiguracionSimulacion(duracionCicloMs, quantum, 0,
                new ConfigComputador[] {new ConfigComputador(ramMB, politica)},
                new ConfigBuffer[0], procesos);
    }

}
