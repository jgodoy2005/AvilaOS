/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Record.java to edit this template
 */
package avilaos.config;

import avilaos.Limites;
import avilaos.hardware.DireccionCabezal;
import avilaos.so.disco.TipoPoliticaDisco;

/**
 *
 * @author juandiegogodoy
 * Configuración del disco y de los usuarios que se agrega a la del Proyecto 1.
 * bloquesArchivos y cabezalInicial vienen del JSON del sistema de archivos; bloquesSwap y la
 * política inicial, del JSON de procesos; velocidad, dirección y usuarios, de la interfaz.
 */
public record ConfiguracionDisco(
    int bloquesArchivos,
    int bloquesSwap,
    int cabezalInicial,
    DireccionCabezal direccionInicial,
    int velocidad,
    TipoPoliticaDisco politicaInicial,
    int usuarios) {

    public static ConfiguracionDisco porDefecto() {
        return new ConfiguracionDisco(200, 100, 50, DireccionCabezal.ARRIBA,
                Limites.VELOCIDAD_CABEZAL_DEFECTO, TipoPoliticaDisco.FIFO, Limites.USUARIOS_DEFECTO);
    }

    //junta lo que traen los dos JSON con lo que se elige en la interfaz (velocidad, direccion y usuarios)
    public static ConfiguracionDisco de(DatosSistemaArchivos archivos, DatosProcesos procesos, int velocidad,
                                        DireccionCabezal direccion, int usuarios) {
        return new ConfiguracionDisco(archivos.totalBloques(), procesos.bloquesSwap(), archivos.cabezalInicial(),
                direccion, velocidad, procesos.politicaDisco(), usuarios);
    }

    //revisa todos los rangos y junta los errores en un solo mensaje
    public void validar() {
        StringBuilder errores = new StringBuilder();
        rango(errores, "Los bloques del disco", bloquesArchivos, Limites.BLOQUES_DISCO_MIN, Limites.BLOQUES_DISCO_MAX);
        rango(errores, "El tamaño del swap", bloquesSwap, Limites.SWAP_MIN, Limites.SWAP_MAX);
        rango(errores, "La velocidad del cabezal", velocidad, Limites.VELOCIDAD_CABEZAL_MIN, Limites.VELOCIDAD_CABEZAL_MAX);
        rango(errores, "La cantidad de usuarios", usuarios, Limites.USUARIOS_MIN, Limites.USUARIOS_MAX);
        if (cabezalInicial < 0 || cabezalInicial >= bloquesArchivos + bloquesSwap) {
            errores.append("- La posición inicial del cabezal debe estar entre 0 y ")
                   .append(bloquesArchivos + bloquesSwap - 1).append(".\n");
        }
        if (direccionInicial == null) {
            errores.append("- Falta la dirección inicial del cabezal.\n");
        }
        if (politicaInicial == null) {
            errores.append("- Falta la política de disco inicial.\n");
        }
        if (errores.length() > 0) {
            throw new IllegalArgumentException(errores.toString().trim());
        }
    }

    private static void rango(StringBuilder errores, String campo, int valor, int min, int max) {
        if (valor < min || valor > max) {
            errores.append("- ").append(campo).append(" debe estar entre ").append(min)
                   .append(" y ").append(max).append(".\n");
        }
    }
}
