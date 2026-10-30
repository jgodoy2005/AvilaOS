/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.gui;

import avilaos.simulacion.FotoBuffer;
import avilaos.simulacion.FotoMetricas;
import avilaos.simulacion.FotoSimulacion;
import avilaos.so.MetricasComputador;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 *
 * @author erick
 */
  

    public class PanelMetricas extends JPanel {

        /**
         * Una fila de la tabla: un computador o el sistema completo.
         */
        private record FilaMetricas(String nombre, FotoMetricas m) {

        }
        private static final String[] COLUMNAS = {"", "Throughput (por 100 ciclos)", "Utilización CPU","Uso útil (usuario)", "Respuesta prom.", "Retorno prom.", "Espera prom.", "Deadlines cumplidos",
"Equidad (Jain)", "Completados", "Abortados", "Murieron sin CPU"};
private static final String[] COLUMNAS_PC = {"Buffer", "Producidos",
        "Consumidos",
        "Procesos que lo usan", "Bloqueo prom. en semáforos (ciclos)"};
    private static final int PUNTOS_VISIBLES = 600;
    private final ModeloTabla<FilaMetricas> modelo = new ModeloTabla<>(COLUMNAS, PanelMetricas::valor);
    private final ModeloTabla<FotoBuffer> modeloPC = new ModeloTabla<>(COLUMNAS_PC, PanelMetricas::valorPC);
    private final XYSeries[] series;
    private long ultimoCicloGraficado = 0; // el ciclo 0 no se grafica:todavía no pasó nada

    public PanelMetricas(int cantidadComputadores) {
        super(new BorderLayout(6, 6));
        JTable tabla = new JTable(modelo);
        tabla.setPreferredScrollableViewportSize(new Dimension(900,
                (cantidadComputadores + 1) * tabla.getRowHeight()));
        JScrollPane scrollTabla = new JScrollPane(tabla);
        scrollTabla.setBorder(BorderFactory.createTitledBorder(
        "Por computador y del sistema completo"));
JTable tablaPC = new JTable(modeloPC);
        tablaPC.setPreferredScrollableViewportSize(new Dimension(900, 4
                * tablaPC.getRowHeight()));
        JScrollPane scrollPC = new JScrollPane(tablaPC);
        scrollPC.setBorder(BorderFactory.createTitledBorder("Productorconsumidor por buffer"));
        JPanel tablas = new JPanel(new GridLayout(2, 1, 6, 6));
        tablas.add(scrollTabla);
        tablas.add(scrollPC);
// Gráfica: una serie por computador, todas en el mismo gráfico
        XYSeriesCollection datos = new XYSeriesCollection();
        series = new XYSeries[cantidadComputadores];
        for (int i = 0; i < cantidadComputadores; i++) {
            series[i] = new XYSeries("Computador " + (i + 1));
            series[i].setMaximumItemCount(PUNTOS_VISIBLES); // ventana móvil:se ven los últimos ciclos datos.addSeries(series[i]);
        }
        JFreeChart grafica = ChartFactory.createXYLineChart(
                "Utilización de la CPU (promedio de los últimos "
                + MetricasComputador.VENTANA_UTILIZACION + " ciclos)",
                "Ciclo", "Utilización (%)", datos, PlotOrientation.VERTICAL,
                true, true, false);
        grafica.getXYPlot().getRangeAxis().setRange(0, 105);
        ChartPanel panelGrafica = new ChartPanel(grafica);
        panelGrafica.setPreferredSize(new Dimension(900, 380));
        JSplitPane division = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                tablas, panelGrafica);
        division.setResizeWeight(0.4);
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        add(division, BorderLayout.CENTER);
    }

    /**
     * Actualiza las tablas y agrega el punto del ciclo a la gráfica. Siempre en
     * el hilo de Swing.
     */
    public void mostrar(FotoSimulacion f) {
        FilaMetricas[] filas = new FilaMetricas[f.computadores().length + 1];
        for (int i = 0; i < f.computadores().length; i++) {
            filas[i] = new FilaMetricas(f.computadores()[i].nombre(),
                    f.computadores()[i].metricas());
        }
        filas[filas.length - 1] = new FilaMetricas("Sistema completo",
                f.global());
        modelo.setFilas(filas);
        modeloPC.setFilas(f.buffers());
        if (f.ciclo() > ultimoCicloGraficado) { // cada ciclo se grafica una sola vez ultimoCicloGraficado = f.ciclo();
            for (int i = 0; i < series.length; i++) {
                series[i].add(f.ciclo(), f.computadores()[i].metricas().utilizacionVentana());
            }
        }
    }

    private static Object valor(FilaMetricas fila, int columna) {
        FotoMetricas m = fila.m();
        return switch (columna) {
            case 0 ->
                fila.nombre();
            case 1 ->
                Formato.decimal(m.throughput());
            case 2 ->
                Formato.porcentaje(m.utilizacion());
            case 3 ->
                Formato.porcentaje(m.utilizacionUsuario());
            case 4 ->
                Formato.decimal(m.respuestaPromedio());
            case 5 ->
                Formato.decimal(m.retornoPromedio());
            case 6 ->
                Formato.decimal(m.esperaPromedio());
            case 7 ->
                Formato.porcentaje(m.tasaDeadlines());
            case 8 ->
                Formato.tresDecimales(m.jain());
            case 9 ->
                m.completados();
            case 10 ->
                m.abortados();
            default ->
                m.sinCPU();
        };
    }

    private static Object valorPC(FotoBuffer b, int columna) {
        return switch (columna) {
            case 0 ->
                b.nombre();
            case 1 ->
                b.producidos();
            case 2 ->
                b.consumidos();
            case 3 ->
                b.procesosQueLoUsan();
            default ->
                Formato.decimal(b.bloqueoPromedio());
        };
    }
}
