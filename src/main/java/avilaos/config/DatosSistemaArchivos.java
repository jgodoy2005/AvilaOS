package avilaos.config;

import avilaos.so.archivos.EntradaFS;

/**
 * Lo que trae el JSON del sistema de archivos, ya revisado y convertido.
 *
 * @param totalBloques   bloques de la zona de archivos del disco
 * @param cabezalInicial posición inicial del cabezal
 * @param entradas       directorios y archivos, en el orden del JSON
 */
public record DatosSistemaArchivos(int totalBloques, int cabezalInicial, EntradaFS[] entradas) {
}