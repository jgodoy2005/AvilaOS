/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.so;

import avilaos.Limites;
import avilaos.so.archivos.TipoNodo;

/**
 *
 * @author juandiegogodoy
 */
public record DatosProceso(
        String nombre,
        TipoProceso tipo,
        int instrucciones,
        int memoriaMB,
        int prioridad,
        int deadline,
        int ciclosEntreES,
        int duracionES,
        String buffer,
        int ciclosPorElemento,
        int elementos,
        int computador,
        OperacionArchivo archivo
        ) {

    public static final int OPERACIONES_POR_ELEMENTO = 5;

    /*Crea y configura procesos de CPU o E/S, recibe los parametros del proceso (los validos segun su tipo),
       desactiva internamente todo lo relacionado a buffers ya que no se usan en este tipo de procesos*/
    public static DatosProceso deCalculo(String nombre, TipoProceso tipo, int instrucciones, int memoriaMB, int prioridad, int deadline, int ciclosEntreES, int duracionES, int computador) {
        return new DatosProceso(nombre, tipo, instrucciones, memoriaMB, prioridad, deadline, ciclosEntreES, duracionES, null, 0, 0, computador,null);
    }

    /*Crea y configura los procesos productores o consumidores (que usen buffer), recibe la informacion del buffer, 
      los elementos y los ciclos para crear c/u de esos elementos, deja en 0 instrucciones ya que esos datos se calculan de otra forma en los P/C */
    public static DatosProceso deBuffer(String nombre, TipoProceso tipo, String buffer, int ciclosPorElemento, int elementos, int memoriaMB, int prioridad, int deadline, int computador) {
        return new DatosProceso(nombre, tipo, 0, memoriaMB, prioridad, deadline, 0, 0, buffer, ciclosPorElemento, elementos, computador, null);
    }

    /*Crea un proceso de E/S de disco (Proyecto 2): su primera instruccion es la operacion de archivo
      (en modo sistema operativo) y las demas son de calculo; no usa el dispositivo de E/S ni buffers*/
    public static DatosProceso deDisco(String nombre, OperacionArchivo archivo, int instrucciones, int memoriaMB, int prioridad, int deadline, int computador) {
        return new DatosProceso(nombre, TipoProceso.ES_DISCO, instrucciones, memoriaMB, prioridad, deadline, 0, 0, null, 0, 0, computador, archivo);
    }

    /*calcular el total de instrucciones de un proceso, en caso de ser P/C aplica una formula, 
     sino, coloca las dadas por el usuario*/
    public int totalInstrucciones() {
        if (tipo != null && tipo.usaBuffer()) {
            return elementos * (ciclosPorElemento + OPERACIONES_POR_ELEMENTO);
        }
        return instrucciones;
    }

    /* validar que todos los datos pedidos sean correctos/esten dentro de los limites,
        si hay multiples errores en los datos se devuelve la lista completa como un error (excepcion)*/
    public void validar() {
        StringBuilder errores = new StringBuilder();
        if (nombre == null || nombre.isBlank()) {
            errores.append("- El nombre no puede estar vacío.\n");
        } else if (nombre.length() > Limites.NOMBRE_MAX) {
            errores.append("- El nombre admite hasta ").append(Limites.NOMBRE_MAX).append(" caracteres.\n");
        }
        if (tipo == null) {
            errores.append("- Falta el tipo de proceso.\n");
        }
        rango(errores, "La memoria (MB)", memoriaMB, Limites.MEMORIA_PROCESO_MIN_MB, Limites.MEMORIA_PROCESO_MAX_MB);
        rango(errores, "La prioridad", prioridad, Limites.PRIORIDAD_MIN, Limites.PRIORIDAD_MAX);
        rango(errores, "El deadline", deadline, Limites.DEADLINE_MIN, Limites.DEADLINE_MAX);
        if (tipo != null && tipo.usaBuffer()) {
            if (buffer == null || buffer.isBlank()) {
                errores.append("- Un productor o consumidor necesita un buffer.\n");
            }
            rango(errores, "La cantidad de ciclos por elemento", ciclosPorElemento,
                    Limites.CICLOS_POR_ELEMENTO_MIN, Limites.CICLOS_POR_ELEMENTO_MAX);
            rango(errores, "La cantidad de elementos", elementos, Limites.ELEMENTOS_MIN, Limites.ELEMENTOS_MAX);
        } else if (tipo != null) {
            rango(errores, "La cantidad de instrucciones", instrucciones, Limites.INSTRUCCIONES_MIN, Limites.INSTRUCCIONES_MAX);
            rango(errores, "La frecuencia de E/S", ciclosEntreES, Limites.CICLOS_ENTRE_ES_MIN, Limites.CICLOS_ENTRE_ES_MAX);
            if (tipo == TipoProceso.IO_BOUND && ciclosEntreES == 0) {
                errores.append("- Un proceso I/O bound debe pedir E/S: indique cada cuántas instrucciones (1 o más).\n");
            }
            if (tipo == TipoProceso.CPU_BOUND && ciclosEntreES > 0) {
                errores.append("- Un proceso CPU bound no pide E/S: deje la frecuencia de E/S en 0.\n");
            }
            if (ciclosEntreES > 0) {
                rango(errores, "La duración de la E/S", duracionES, Limites.DURACION_ES_MIN, Limites.DURACION_ES_MAX);
            }
            if (tipo == TipoProceso.ES_DISCO) {
                validarArchivo(errores);
            }
        }
        if (computador < 0) {
            errores.append("- El computador no es válido.\n");
        }
        if (errores.length() > 0) {
            throw new IllegalArgumentException(errores.toString().trim());
        }
    }
    
    //un proceso de disco necesita su operacion completa; los permisos y el espacio se revisan al ejecutarla/
    private void validarArchivo(StringBuilder errores) {
        if (archivo == null || archivo.operacion() == null || !archivo.operacion().esDeArchivo()) {
            errores.append("- Un proceso de disco necesita una operación: crear, leer, actualizar o eliminar.\n");
            return;
        }
        if (archivo.ruta() == null || !archivo.ruta().startsWith("/")) {
            errores.append("- La ruta del archivo debe empezar con /.\n");
        }
        if (archivo.operacion() == TipoOperacionES.CREAR) {
            if (archivo.tipoNodo() == null) {
                errores.append("- Para crear hay que indicar si es archivo o directorio.\n");
            } else if (archivo.tipoNodo() == TipoNodo.ARCHIVO && archivo.bloques() < Limites.TAMANO_ARCHIVO_MIN) {
                errores.append("- Un archivo ocupa al menos ").append(Limites.TAMANO_ARCHIVO_MIN).append(" bloque.\n");
            }
        }
        if (archivo.usuario() == null) {
            errores.append("- Falta el usuario que lanza el proceso.\n");
        }
    }

    /*verifica si un valor se encuentra en un rango, se usa de apoyo para validar()*/
    private static void rango(StringBuilder errores, String campo, int valor, int min, int max) {
        if (valor < min || valor > max) {
            errores.append("- ").append(campo).append(" debe estar entre ").append(min)
                    .append(" y ").append(max).append(".\n");
        }
    }

}
