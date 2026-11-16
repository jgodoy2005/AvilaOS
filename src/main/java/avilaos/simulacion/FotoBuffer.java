package avilaos.simulacion;

/** Copia inmutable del estado de un buffer: contenido, semáforos y quién espera en cada uno. */
public record FotoBuffer(
    String nombre,
    int anfitrion,
    int capacidad,
    int cantidad,
    String[] contenido,
    int valorS,
    int valorN,
    int valorE,
    String[] esperandoS,
    String[] esperandoN,
    String[] esperandoE,
    long producidos,
    long consumidos,
    int procesosQueLoUsan,
    double bloqueoPromedio) {
}