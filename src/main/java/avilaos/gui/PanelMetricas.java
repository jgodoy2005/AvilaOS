/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.gui;

import avilaos.simulacion.FotoDisco;
import avilaos.simulacion.FotoMetricas;
import avilaos.simulacion.FotoPCB;
import avilaos.simulacion.FotoSimulacion;
import avilaos.so.MetricasComputador;
import avilaos.so.MotivoTerminacion;
import avilaos.so.disco.TipoPoliticaDisco;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.general.DefaultPieDataset;
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

    //una fila con las metricas del disco y de la suspension
    private record FilaDisco(FotoDisco disco, int suspensiones, int reactivaciones) {

    }

    private static final String[] COLUMNAS = {"", "Throughput (por 100 ciclos)", "Utilización CPU", "Uso útil (usuario)", "Respuesta prom.", "Retorno prom.", "Espera prom.", "Deadlines cumplidos",
        "Equidad (Jain)", "Completados", "Abortados", "Murieron sin CPU"};
    private static final String[] COLUMNAS_DISCO = {"Desplazamiento total (bloques)", "Espera prom. en la cola (ciclos)",
        "Atendidas con FIFO", "Con SSTF", "Con SCAN", "Con C-SCAN", "Suspensiones", "Reactivaciones"};
    private static final int PUNTOS_VISIBLES = 600;
    private final ModeloTabla<FilaMetricas> modelo = new ModeloTabla<>(COLUMNAS, PanelMetricas::valor);
    private final ModeloTabla<FilaDisco> modeloDisco = new ModeloTabla<>(COLUMNAS_DISCO, PanelMetricas::valorDisco);
    private final XYSeries[] series;
    private final XYSeries[] seriesDisco = new XYSeries[TipoPoliticaDisco.values().length];   //una por politica de disco
    private int puntosDisco;   //puntos de la serie del disco ya graficados
    private long ultimoCicloGraficado = 0; // el ciclo 0 no se grafica:todavía no pasó nada
    //Lo que se puede ver abajo: el desplazamiento del cabezal, la utilización en el tiempo o las causas de terminación
    private static final String[] VISTAS = {"Desplazamiento acumulado del cabezal", "Utilización de la CPU en el tiempo",
        "Causas de terminación"};
    private final ModeloLista<String> modeloVista = new ModeloLista<>(VISTAS);
    private final CardLayout tarjetas = new CardLayout();
    private final JPanel zonaGraficas = new JPanel(tarjetas);

    //Los datos de un pastel. Hay uno por computador y, al final, uno del sistema completo. 
    private record Pastel(DefaultPieDataset<String> datos) {

    }
    private final Pastel[] pasteles;

    public PanelMetricas(int cantidadComputadores) {
        super(new BorderLayout(6, 6));
        JTable tabla = new JTable(modelo);
        tabla.setPreferredScrollableViewportSize(new Dimension(900,
                (cantidadComputadores + 1) * tabla.getRowHeight()));
        JScrollPane scrollTabla = new JScrollPane(tabla);
        scrollTabla.setBorder(BorderFactory.createTitledBorder(
                "Por computador y del sistema completo"));
        JTable tablaDisco = new JTable(modeloDisco);
        tablaDisco.setPreferredScrollableViewportSize(new Dimension(900, 2 * tablaDisco.getRowHeight()));
        JScrollPane scrollDisco = new JScrollPane(tablaDisco);
        scrollDisco.setBorder(BorderFactory.createTitledBorder("Disco y suspensión"));
        JPanel tablas = new JPanel(new GridLayout(2, 1, 6, 6));
        tablas.add(scrollTabla);
        tablas.add(scrollDisco);

        // Gráfica: una serie por computador, todas en el mismo gráfico
        XYSeriesCollection datos = new XYSeriesCollection();
        series = new XYSeries[cantidadComputadores];
        for (int i = 0; i < cantidadComputadores; i++) {
            series[i] = new XYSeries("Computador " + (i + 1));
            series[i].setMaximumItemCount(PUNTOS_VISIBLES); // ventana móvil: se ven los últimos ciclos
            datos.addSeries(series[i]);
        }
        JFreeChart grafica = ChartFactory.createXYLineChart(
                "Utilización de la CPU (promedio de los últimos "
                + MetricasComputador.VENTANA_UTILIZACION + " ciclos)",
                "Ciclo", "Utilización (%)", datos, PlotOrientation.VERTICAL,
                true, true, false);
        grafica.getXYPlot().getRangeAxis().setRange(0, 105);

        Color[] colores = {new Color(214, 39, 40), new Color(31, 119, 180), new Color(44, 160, 44), new Color(255, 127, 14),
            new Color(148, 103, 189), new Color(140, 86, 75), new Color(227, 119, 194), new Color(23, 190, 207)};
        for (int i = 0; i < cantidadComputadores; i++) {   // un color fácil de distinguir para cada computador
            grafica.getXYPlot().getRenderer().setSeriesPaint(i, colores[i % colores.length]);
        }
        ChartPanel panelGrafica = new ChartPanel(grafica);
        panelGrafica.setPreferredSize(new Dimension(900, 380));

        // Desplazamiento acumulado del cabezal: una serie por política, para ver con cuál se recorrió cada tramo
        XYSeriesCollection datosDisco = new XYSeriesCollection();
        for (TipoPoliticaDisco p : TipoPoliticaDisco.values()) {
            seriesDisco[p.ordinal()] = new XYSeries(p.toString());
            datosDisco.addSeries(seriesDisco[p.ordinal()]);
        }
        JFreeChart graficaDisco = ChartFactory.createXYLineChart("Desplazamiento acumulado del cabezal",
                "Ciclo", "Bloques recorridos", datosDisco, PlotOrientation.VERTICAL, true, true, false);
        for (int i = 0; i < seriesDisco.length; i++) {
            graficaDisco.getXYPlot().getRenderer().setSeriesPaint(i, colores[i % colores.length]);
        }
        ChartPanel panelDisco = new ChartPanel(graficaDisco);
        panelDisco.setPreferredSize(new Dimension(900, 380));

        // Pasteles de causas de terminación: uno por computador y uno del sistema completo
        pasteles = new Pastel[cantidadComputadores + 1];
        int columnas = (int) Math.ceil(Math.sqrt(pasteles.length));
        JPanel panelPasteles = new JPanel(new GridLayout(0, columnas, 6, 6));
        for (int i = 0; i < pasteles.length; i++) {
            String titulo = i < cantidadComputadores ? "Computador " + (i + 1) : "Sistema completo";
            pasteles[i] = new Pastel(new DefaultPieDataset<>());
            panelPasteles.add(crearPastel(titulo, pasteles[i].datos()));
        }

        // Selector: cada vista es una "tarjeta" del CardLayout y solo se ve una
        zonaGraficas.add(panelDisco, VISTAS[0]);
        zonaGraficas.add(panelGrafica, VISTAS[1]);
        zonaGraficas.add(panelPasteles, VISTAS[2]);
        JComboBox<String> selector = new JComboBox<>(modeloVista);
        selector.addActionListener(e -> tarjetas.show(zonaGraficas, (String) modeloVista.getSelectedItem()));
        JPanel barraVista = new JPanel(new FlowLayout(FlowLayout.LEFT));
        barraVista.add(new JLabel("Ver en la gráfica:"));
        barraVista.add(selector);
        JPanel zonaInferior = new JPanel(new BorderLayout());
        zonaInferior.add(barraVista, BorderLayout.NORTH);
        zonaInferior.add(zonaGraficas, BorderLayout.CENTER);
        JSplitPane division = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                tablas, zonaInferior);

        division.setResizeWeight(0.0);
        division.setDividerLocation(220);
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
        modeloDisco.setFilas(new FilaDisco[]{new FilaDisco(f.disco(), f.computadores()[0].suspensiones(),
            f.computadores()[0].reactivaciones())});
        graficarDisco(f.disco());
        if (f.ciclo() > ultimoCicloGraficado) { // cada ciclo se grafica una sola vez
            ultimoCicloGraficado = f.ciclo();
            for (int i = 0; i < series.length; i++) {
                series[i].add(f.ciclo(), f.computadores()[i].metricas().utilizacionVentana());
            }
        }
        actualizarPasteles(f);
    }

    /**
     * Agrega los puntos nuevos del desplazamiento acumulado. Cada punto va a la serie de la
     * política con la que se recorrió; cuando la política cambia, el punto anterior también se
     * agrega a la serie nueva para que la línea siga unida.
     */
    private void graficarDisco(FotoDisco d) {
        long[] ciclos = d.serieCiclos();
        long[] desplazamiento = d.serieDesplazamiento();
        int[] politica = d.seriePolitica();
        for (int i = puntosDisco; i < ciclos.length; i++) {
            if (i > 0 && politica[i] != politica[i - 1]) {
                seriesDisco[politica[i]].add(ciclos[i - 1], desplazamiento[i - 1]);
            }
            seriesDisco[politica[i]].add(ciclos[i], desplazamiento[i]);
        }
        puntosDisco = ciclos.length;
    }

    /**
     * Crea un pastel con una porción por cada causa de terminación (las del
     * enum MotivoTerminacion).
     */
    private static ChartPanel crearPastel(String titulo, DefaultPieDataset<String> datos) {
        for (MotivoTerminacion m : MotivoTerminacion.values()) {
            datos.setValue(m.toString(), 0);
        }
        JFreeChart pastel = ChartFactory.createPieChart(titulo, datos, true, true, false);
        PiePlot<?> plot = (PiePlot<?>) pastel.getPlot();
        for (MotivoTerminacion m : MotivoTerminacion.values()) {
            plot.setSectionPaint(m.toString(), colorDe(m));
        }
        plot.setLabelGenerator(new StandardPieSectionLabelGenerator("{1} ({2})"));   // cantidad (porcentaje)
        plot.setIgnoreZeroValues(true);
        plot.setNoDataMessage("Todavía no terminó ningún proceso");
        ChartPanel panel = new ChartPanel(pastel);
        panel.setPreferredSize(new Dimension(300, 260));
        return panel;
    }

    /**
     * Cuenta los terminados de cada computador según su causa y actualiza los
     * pasteles.
     */
    private void actualizarPasteles(FotoSimulacion f) {
        MotivoTerminacion[] causas = MotivoTerminacion.values();
        int[] total = new int[causas.length];
        for (int i = 0; i < f.computadores().length; i++) {
            int[] cuenta = new int[causas.length];
            for (FotoPCB p : f.computadores()[i].terminados()) {
                for (int c = 0; c < causas.length; c++) {
                    if (p.motivo().startsWith(causas[c].toString())) {   // puede terminar en " (sin CPU)"
                        cuenta[c]++;
                        total[c]++;
                        break;
                    }
                }
            }
            actualizar(pasteles[i].datos(), causas, cuenta);
        }
        actualizar(pasteles[pasteles.length - 1].datos(), causas, total);
    }

    /**
     * Cambia solo las porciones que cambiaron, para no redibujar los pasteles
     * en cada ciclo.
     */
    private static void actualizar(DefaultPieDataset<String> datos, MotivoTerminacion[] causas, int[] cuenta) {
        for (int c = 0; c < causas.length; c++) {
            Number actual = datos.getValue(causas[c].toString());
            if (actual == null || actual.intValue() != cuenta[c]) {
                datos.setValue(causas[c].toString(), cuenta[c]);
            }
        }
    }

    /**
     * El color de cada causa: verde si terminó, rojo si venció, naranja si la
     * holgura lo terminó antes.
     */
    private static Color colorDe(MotivoTerminacion m) {
        return switch (m) {
            case COMPLETADO ->
                new Color(44, 160, 44);
            case DEADLINE ->
                new Color(214, 39, 40);
            case INALCANZABLE ->
                new Color(255, 127, 14);
            case RECHAZADO ->
                new Color(148, 103, 189);
        };
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

    private static Object valorDisco(FilaDisco fila, int columna) {
        FotoDisco d = fila.disco();
        return switch (columna) {
            case 0 ->
                d.desplazamientoTotal();
            case 1 ->
                Formato.decimal(d.esperaPromedio());
            case 2, 3, 4, 5 ->
                d.atendidasPorPolitica()[columna - 2];
            case 6 ->
                fila.suspensiones();
            default ->
                fila.reactivaciones();
        };
    }
}