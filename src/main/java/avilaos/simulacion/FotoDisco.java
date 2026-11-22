/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Record.java to edit this template
 */
package avilaos.simulacion;
import avilaos.hardware.DireccionCabezal;
import avilaos.so.disco.TipoPoliticaDisco;

/**
 *
 * @author juandiegogodoy
* Copia de solo lectura del disco: mapa de bloques, cabezal, cola, métricas y la serie del
 * desplazamiento acumulado para la gráfica.
 */
public record FotoDisco(
    int bloquesArchivos,
    int bloquesSwap,
    int cabezal,
    DireccionCabezal direccion,
    int velocidad,
    boolean detenido,
    boolean deteniendo,
    TipoPoliticaDisco politica,
    FotoSolicitud enCurso,
    FotoSolicitud[] cola,
    String[] ocupantes,
    int[] colores,
    int[] siguientes,
    long desplazamientoTotal,
    double esperaPromedio,
    int[] atendidasPorPolitica,
    long[] serieCiclos,
    long[] serieDesplazamiento,
    int[] seriePolitica)  {
    
    public int totalBloques() {
        return bloquesArchivos + bloquesSwap;
    }

}
