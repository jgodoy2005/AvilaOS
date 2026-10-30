package avilaos.simulacion;
import avilaos.so.MetricasComputador;
/**
 * Métricas de un computador (o del clúster completo) en un instante. Guarda
 * sumas y conteos, y calcula los promedios en sus métodos: así combinar varios
 * computadores es sumar campo por campo.
 */
public record FotoMetricas(
    long ciclos,
    long ciclosCpu,
    long ciclosOcupada,
    long ciclosUsuario,
    long ciclosSistema,
    int ocupadosVentana,
    int tamanoVentana,
    int completados,
    int abortados,
    int sinCPU,
    long sumaRespuesta,
    int conRespuesta,
    long sumaRetorno,
    long sumaEspera,
    double sumaX,
    double sumaX2,
    int conX,
    long sumaBloqueoSemaforo,
    int terminadosPC) {

    /** Métricas del sistema completo: se suman las de todos los computadores. */
    public static FotoMetricas combinar(FotoMetricas[] partes) {
        long ciclos = 0, ciclosCpu = 0, ocupada = 0, usuario = 0, sistema = 0;
        int ventanaOcupados = 0, ventana = 0, completados = 0, abortados = 0, sinCPU = 0, conRespuesta = 0, conX = 0;
        int terminadosPC = 0;
        long sumaRespuesta = 0, sumaRetorno = 0, sumaEspera = 0, sumaBloqueo = 0;
        double sumaX = 0, sumaX2 = 0;
        for (FotoMetricas m : partes) {
            ciclos = Math.max(ciclos, m.ciclos);
            ciclosCpu += m.ciclosCpu;
            ocupada += m.ciclosOcupada;
            usuario += m.ciclosUsuario;
            sistema += m.ciclosSistema;
            ventanaOcupados += m.ocupadosVentana;
            ventana += m.tamanoVentana;
            completados += m.completados;
            abortados += m.abortados;
            sinCPU += m.sinCPU;
            sumaRespuesta += m.sumaRespuesta;
            conRespuesta += m.conRespuesta;
            sumaRetorno += m.sumaRetorno;
            sumaEspera += m.sumaEspera;
            sumaX += m.sumaX;
            sumaX2 += m.sumaX2;
            conX += m.conX;
            sumaBloqueo += m.sumaBloqueoSemaforo;
            terminadosPC += m.terminadosPC;
        }

        return new FotoMetricas(ciclos, ciclosCpu, ocupada, usuario, sistema,
            ventanaOcupados, ventana, completados, abortados, sinCPU, sumaRespuesta, conRespuesta,
            sumaRetorno, sumaEspera, sumaX, sumaX2, conX, sumaBloqueo, terminadosPC);
    }
    
    public static FotoMetricas de(MetricasComputador m) {
        return new FotoMetricas(m.getCiclos(), m.getCiclos(),
        m.getCiclosOcupada(), m.getCiclosUsuario(),
        m.getCiclosSistema(), m.getOcupadosEnVentana(),
        m.getTamanoVentanaUsada(),
        m.getCompletados(), m.getAbortados(), m.getSinCPU(),
        m.getSumaRespuesta(), m.getConRespuesta(),
        m.getSumaRetorno(), m.getSumaEspera(), m.getSumaX(),
        m.getSumaX2(), m.getConX(),
        m.getSumaBloqueoSemaforo(), m.getTerminadosPC());
    }

    public int terminados() {
        return completados + abortados;
    }

    /** % de ciclos con la CPU ocupada (usuario + SO). */
    public double utilizacion() {
        return ciclosCpu == 0 ? 0 : 100.0 * ciclosOcupada / ciclosCpu;
    }

    /** % de ciclos ejecutando programas de usuario. */
    public double utilizacionUsuario() {
        return ciclosCpu == 0 ? 0 : 100.0 * ciclosUsuario / ciclosCpu;
    }

    /** % de utilización en los últimos ciclos (lo que dibuja la gráfica). */
    public double utilizacionVentana() {
        return tamanoVentana == 0 ? 0 : 100.0 * ocupadosVentana / tamanoVentana;
    }

    /** Procesos completados por cada 100 ciclos. */
    public double throughput() {
        return ciclos == 0 ? 0 : 100.0 * completados / ciclos;
    }

    public double respuestaPromedio() {
        return conRespuesta == 0 ? Double.NaN : (double) sumaRespuesta / conRespuesta;
    }

    public double retornoPromedio() {
        return completados == 0 ? Double.NaN : (double) sumaRetorno / completados;
    }

    public double esperaPromedio() {
        return terminados() == 0 ? Double.NaN : (double) sumaEspera / terminados();
    }

    /** % de procesos terminados que cumplieron su deadline (los demás murieron por él). */
    public double tasaDeadlines() {
        return terminados() == 0 ? Double.NaN : 100.0 * completados / terminados();
    }

    /** Índice de Jain: 1 = reparto perfectamente parejo. */
    public double jain() {
        return conX == 0 || sumaX2 == 0 ? Double.NaN : (sumaX * sumaX) / (conX * sumaX2);
    }

    public double bloqueoSemaforoPromedio() {
        return terminadosPC == 0 ? Double.NaN : (double) sumaBloqueoSemaforo / terminadosPC;
    }
}