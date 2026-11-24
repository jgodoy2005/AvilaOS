package avilaos.simulacion;

import avilaos.hardware.ModoCPU;
import avilaos.so.planificacion.TipoPolitica;

/** Copia inmutable del estado de un computador al final de un ciclo. */
public record FotoComputador(
    int numero,
    String nombre,
    TipoPolitica politica,
    FotoPCB enEjecucion,
    boolean cpuOciosa,
    ModoCPU modo,
    String descripcionCPU,
    int memoriaUsadaMB,
    int memoriaTotalMB,
    String estadoES,
    FotoPCB[] nuevos,
    FotoPCB[] listos,
    FotoPCB[] bloqueados,
    FotoPCB[] terminados,
    FotoMetricas metricas,
    FotoPCB[] listosSuspendidos,       //en el swap, listos para volver a la RAM
    FotoPCB[] bloqueadosSuspendidos,   //en el swap, esperando su evento
    int suspensiones,
    int reactivaciones) {

    public int memoriaLibreMB() {
        return memoriaTotalMB - memoriaUsadaMB;
    }
}