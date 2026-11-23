/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.so.disco;

import avilaos.estructuras.ListaEnlazada;
/**
 *
 * @author erick
 * 
 * Métricas del disco: solicitudes atendidas por cada política, tiempo de espera en la cola
 * y la serie del desplazamiento acumulado del cabezal para la gráfica.
 */
public class MetricasDisco {

    private final int[] atendidasPorPolitica = new int[TipoPoliticaDisco.values().length];
    private long esperaTotal;
    private int atendidas;
    private final ListaEnlazada<long[]> serie = new ListaEnlazada<>();   //{ciclo, desplazamiento, politica}

    //una solicitud empieza a atenderse despues de esperar 'espera' ciclos en la cola
    void registrarAtencion(TipoPoliticaDisco politica, long espera) {
        atendidasPorPolitica[politica.ordinal()]++;
        esperaTotal += espera;
        atendidas++;
    }

    //un punto de la grafica: el desplazamiento acumulado al final de ese ciclo y la politica activa
    void registrarPunto(long ciclo, long desplazamiento, TipoPoliticaDisco politica) {
        serie.agregarAlFinal(new long[] {ciclo, desplazamiento, politica.ordinal()});
    }

    public int[] getAtendidasPorPolitica() {
        return atendidasPorPolitica.clone();
    }

    public int getAtendidas() {
        return atendidas;
    }

    //promedio de ciclos de espera en la cola; NaN si todavia no se atendio ninguna
    public double getEsperaPromedio() {
        return atendidas == 0 ? Double.NaN : (double) esperaTotal / atendidas;
    }

    //columna 0: ciclos, 1: desplazamiento acumulado, 2: politica (indice del enum)
    public long[] serie(int columna) {
        long[] r = new long[serie.tamano()];
        int[] i = {0};
        serie.paraCada(p -> r[i[0]++] = p[columna]);
        return r;
    }
}
