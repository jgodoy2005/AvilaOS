package avilaos.so.archivos;

/**
 * Un elemento del JSON del sistema de archivos ya convertido a los tipos del simulador:
 * lo que se carga al iniciar y lo que se guarda para la próxima ejecución.
 *
 * @param ruta         ruta completa, por ejemplo /sistema/boot.bin
 * @param tipo         archivo o directorio
 * @param primerBloque primer bloque del archivo (-1 en los directorios)
 * @param bloques      cantidad de bloques del archivo (0 en los directorios)
 * @param dueno        "admin" o userN
 * @param permiso      público o privado
 * @param cadena       bloques del archivo en orden; null si el JSON no la trae (entonces son seguidos)
 */
public record EntradaFS(
    String ruta,
    TipoNodo tipo,
    int primerBloque,
    int bloques,
    String dueno,
    Permiso permiso,
    int[] cadena) {
}