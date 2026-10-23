/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.so;
import avilaos.Limites;
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
        int computador
        ) {
    
    
    public static final int OPERACIONES_POR_ELEMENTO = 5;
    
    
    
    /*Crea y configura procesos de CPU o E/S, recibe los parametros del proceso (los validos segun su tipo),
       desactiva internamente todo lo relacionado a buffers ya que no se usan en este tipo de procesos*/
    public static DatosProceso deCalculo(String nombre, TipoProceso tipo, int instrucciones, int memoriaMB, int prioridad, int deadline, int ciclosEntreES, int duracionES, int computador) {
        return new DatosProceso(nombre, tipo, instrucciones, memoriaMB, prioridad, deadline, ciclosEntreES, duracionES, null, 0, 0, computador);
    }
    
    /*Crea y configura los procesos productores o consumidores (que usen buffer), recibe la informacion del buffer, 
      los elementos y los ciclos para crear c/u de esos elementos, deja en 0 instrucciones ya que esos datos se calculan de otra forma en los P/C */
    public static DatosProceso deBuffer(String nombre, TipoProceso tipo, String buffer, int ciclosPorElemento, int elementos, int memoriaMB, int prioridad, int deadline, int computador) {
        return new DatosProceso(nombre, tipo, 0, memoriaMB, prioridad, deadline, 0, 0, buffer, ciclosPorElemento, elementos, computador);
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
            if (ciclosEntreES > 0) {
                rango(errores, "La duración de la E/S", duracionES, Limites.DURACION_ES_MIN, Limites.DURACION_ES_MAX);
            }
        }
        if (computador < 0) {
            errores.append("- El computador no es válido.\n");
        }
        if (errores.length() > 0) {
            throw new IllegalArgumentException(errores.toString().trim());
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

