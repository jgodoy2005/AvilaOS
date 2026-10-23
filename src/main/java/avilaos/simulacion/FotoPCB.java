package avilaos.simulacion;

import avilaos.so.EstadoProceso;
import avilaos.so.MotivoBloqueo;
import avilaos.so.PCB;
import avilaos.so.TipoProceso;

/**
 * Copia inmutable de un PCB en un instante. La interfaz solo ve estas copias,
 * nunca los PCB reales: así puede leerlas desde su hilo sin candados.
 */
public record FotoPCB(
    int id,
    String etiqueta,
    String nombre,
    int computador,
    EstadoProceso estado,
    TipoProceso tipo,
    int prioridad,
    int ciclosEsperando,
    int memoriaMB,
    int deadline,
    long cicloLimite,
    long deadlineRestante,
    int pc,
    int mar,
    int instruccionesRestantes,
    int totalInstrucciones,
    String instruccion,
    String motivo,
    String buffer,
    int elementosHechos,
    int elementosRequeridos,
    int ciclosEjecutados,
    int ciclosBloqueadoSemaforo,
    int ciclosBloqueadoRed,
    MotivoBloqueo motivoBloqueo) {

    /** Foto del proceso al terminar el ciclo {@code ciclo}. */
    public static FotoPCB de(PCB p, long ciclo) {
        boolean terminado = p.getEstado() == EstadoProceso.TERMINADO;
        return new FotoPCB(
            p.getId(),
            p.etiqueta(),
            p.getNombre(),
            p.getNumeroComputador(),
            p.getEstado(),
            p.getTipo(),
            p.getPrioridad(),
            p.getCiclosEsperando(),
            p.getMemoriaMB(),
            p.getDeadline(),
            p.getCicloLimite(),
            p.getDeadlineRestante(ciclo),
            p.getPc(),
            p.getMar(),
            p.getInstruccionesRestantes(),
            p.getTotalInstrucciones(),
            terminado ? "" : p.descripcionInstruccionActual(),
            describirMotivo(p),
            p.getBuffer() == null ? "" : p.getBuffer().getNombre(),
            p.getElementosHechos(),
            p.getElementosRequeridos(),
            p.getCiclosEjecutados(),
            p.getCiclosBloqueadoSemaforo(),
            p.getCiclosBloqueadoRed(),
            p.getEstado() == EstadoProceso.BLOQUEADO ? p.getMotivoBloqueo() : null
        );
    }

    private static String describirMotivo(PCB p) {
        if (p.getEstado() == EstadoProceso.TERMINADO) {
            boolean nuncaCorrio = p.getCicloPrimeraEjecucion() < 0;
            return p.getMotivoTerminacion() + (nuncaCorrio ? " (sin CPU)" : "");
        }
        if (p.getEstado() != EstadoProceso.BLOQUEADO || p.getMotivoBloqueo() == null) {
            return "";
        }
        MotivoBloqueo m = p.getMotivoBloqueo();
        return switch (m) {
            case ES -> m.toString();
            case RED -> m + ": " + p.getOperacionRemota() + " en " + p.getBuffer().getNombre() + " (faltan " + p.getCiclosRedRestantes() + ")";
            default -> m + " en " + p.getBuffer().getNombre();
        };
    }

    /** Productores y consumidores: "3/10"; los demás: "". */
    public String progresoElementos() {
        return tipo.usaBuffer() ? elementosHechos + "/" + elementosRequeridos : "";
    }
}