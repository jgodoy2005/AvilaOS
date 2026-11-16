package avilaos.simulacion;

/**
 * Quien quiere enterarse de cada ciclo nuevo (la interfaz). Patrón Observer: la
 * simulación no sabe nada de Swing; solo avisa.
 */
public interface ObservadorSimulacion {
    /**
     * Se llama desde el hilo del reloj al final de cada ciclo, sin el candado del
     * ciclo. El reloj no sigue hasta que este método termina.
     */
    void estadoActualizado(FotoSimulacion foto);
}