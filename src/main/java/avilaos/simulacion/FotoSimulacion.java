package avilaos.simulacion;

/** Todo lo que la interfaz necesita mostrar después de un ciclo. */
public record FotoSimulacion(
    long ciclo,
    boolean pausada,
    FotoComputador[] computadores,
    FotoBuffer[] buffers,
    FotoMetricas global) {

    /** Nombres de los buffers, para los menús de la interfaz. */
    public String[] nombresBuffers() {
        String[] nombres = new String[buffers.length];
        for (int i = 0; i < buffers.length; i++) {
            nombres[i] = buffers[i].nombre();
        }
        return nombres;
    }
}