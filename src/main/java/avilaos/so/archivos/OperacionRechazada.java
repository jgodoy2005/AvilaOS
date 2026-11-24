package avilaos.so.archivos;

/**
 * El sistema de archivos no puede hacer la operación (permiso, espacio, nombre repetido...).
 * El mensaje explica por qué; el proceso termina con "Operación rechazada".
 */
public class OperacionRechazada extends Exception {

    public OperacionRechazada(String motivo) {
        super(motivo);
    }
}