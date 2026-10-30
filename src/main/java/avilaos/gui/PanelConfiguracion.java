/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.gui;

import avilaos.Limites;
import avilaos.config.ArchivoConfiguracion;
import avilaos.config.ConfiguracionSimulacion;
import avilaos.config.ConfiguracionSimulacion.ConfigBuffer;
import avilaos.config.ConfiguracionSimulacion.ConfigComputador;
import avilaos.estructuras.Accion;
import avilaos.estructuras.ListaEnlazada;
import avilaos.so.DatosProceso;
import avilaos.so.planificacion.TipoPolitica;
import avilaos.so.sincronizacion.BufferAcotado;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.File;
import java.io.IOException;
import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;

/**
 *
 * @author erick
 */
public class PanelConfiguracion extends JPanel {

    private final JSpinner cantidad = PanelSimulacion.spinner(2,
            Limites.COMPUTADORES_MIN, Limites.COMPUTADORES_MAX, 1);
    private final JSpinner duracion = PanelSimulacion.spinner(500,
            Limites.DURACION_CICLO_MIN_MS, Limites.DURACION_CICLO_MAX_MS, 50);
    private final JSpinner quantum = PanelSimulacion.spinner(4,
            Limites.QUANTUM_MIN, Limites.QUANTUM_MAX, 1);
    private final JSpinner latencia = PanelSimulacion.spinner(3,
            Limites.LATENCIA_MIN, Limites.LATENCIA_MAX, 1);
    private final ModeloComputadores modeloComputadores = new ModeloComputadores();
    private final ListaEnlazada<ConfigBuffer> buffers = new ListaEnlazada<>();
    private final ListaEnlazada<DatosProceso> procesos = new ListaEnlazada<>();
    private final ModeloTabla<ConfigBuffer> modeloBuffers = new ModeloTabla<>(
            new String[]{"Buffer", "Capacidad", "Anfitrión", "Memoria"},
            this::valorBuffer);
    private final ModeloTabla<DatosProceso> modeloProcesos = new ModeloTabla<>(
            new String[]{"Nombre", "Tipo", "Computador", "MB", "Prio.",
                "Deadline", "Instrucciones", "Buffer"},
            PanelConfiguracion::valorProceso);
    private final JTable tablaProcesos = new JTable(modeloProcesos);
    private final Accion<ConfiguracionSimulacion> alIniciar;

    public PanelConfiguracion(Accion<ConfiguracionSimulacion> alIniciar) {
        super(new BorderLayout(8, 8));
        this.alIniciar = alIniciar;

        JPanel parametros = Formato.formulario();
        Formato.fila(parametros, 0, "Número de computadores:", cantidad);
        Formato.fila(parametros, 1, "Duración del ciclo (ms):", duracion);
        Formato.fila(parametros, 2, "Quantum de Round Robin (ciclos):",
                quantum);
        Formato.fila(parametros, 3, "Latencia de red (ciclos):", latencia);

        parametros.setBorder(BorderFactory.createTitledBorder("Parámetros generales"));
        cantidad.addChangeListener(e
                -> modeloComputadores.redimensionar((Integer) cantidad.getValue()));

        JTable tablaComputadores = new JTable(modeloComputadores);
        JComboBox<TipoPolitica> editorPolitica = new JComboBox<>(new ModeloLista<>(TipoPolitica.values()));
        tablaComputadores.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(editorPolitica));
        tablaComputadores.setPreferredScrollableViewportSize(new Dimension(420, 8 * tablaComputadores.getRowHeight()));
        JScrollPane scrollComputadores = new JScrollPane(tablaComputadores);

        scrollComputadores.setBorder(BorderFactory.createTitledBorder(
                "Computadores (doble clic para editar)"));

        JPanel izquierda = new JPanel(new BorderLayout(8, 8));
        izquierda.add(parametros, BorderLayout.NORTH);
        izquierda.add(scrollComputadores, BorderLayout.CENTER);

// Buffers iniciales
        JTable tablaBuffers = new JTable(modeloBuffers);
        tablaBuffers.setPreferredScrollableViewportSize(new Dimension(420, 5
                * tablaBuffers.getRowHeight()));
        JButton agregarBuffer = new JButton("Agregar buffer…");
        JButton quitarBuffer = new JButton("Quitar el último");
        agregarBuffer.addActionListener(e -> agregarBuffer());
        quitarBuffer.addActionListener(e -> quitarUltimoBuffer());
        JPanel panelBuffers = seccion("Buffers iniciales", new JScrollPane(tablaBuffers), agregarBuffer, quitarBuffer);
// Procesos iniciales
        tablaProcesos.setPreferredScrollableViewportSize(new Dimension(600,
                10 * tablaProcesos.getRowHeight()));
        JButton agregarProceso = new JButton("Agregar proceso…");
        JButton quitarProceso = new JButton("Quitar seleccionado");
        agregarProceso.addActionListener(e -> agregarProceso());
        quitarProceso.addActionListener(e -> quitarProceso());
        JPanel panelProcesos = seccion("Carga inicial de procesos", new JScrollPane(tablaProcesos),
                agregarProceso, quitarProceso);

        JPanel derecha = new JPanel(new GridLayout(2, 1, 8, 8));
        derecha.add(panelBuffers);
        derecha.add(panelProcesos);
        JPanel centro = new JPanel(new GridLayout(1, 2, 12, 0));
        centro.add(izquierda);
        centro.add(derecha);
        JButton cargar = new JButton("Cargar JSON…");
        JButton guardar = new JButton("Guardar JSON…");
        JButton iniciar = new JButton("Iniciar simulación");
        iniciar.setFont(iniciar.getFont().deriveFont(java.awt.Font.BOLD));
        cargar.addActionListener(e -> cargarArchivo());
        guardar.addActionListener(e -> guardarArchivo());
        iniciar.addActionListener(e -> iniciar());
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        botones.add(new JLabel(
                "La simulación arranca en pausa: use \"Reanudar\" en la pestaña Simulación. "));
        botones.add(cargar);
        botones.add(guardar);
        botones.add(iniciar);

        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(centro, BorderLayout.CENTER);
        add(botones, BorderLayout.SOUTH);
        mostrarConfiguracion(ConfiguracionSimulacion.porDefecto());
    }

    /**
     * Arma la configuración con lo que hay en pantalla y la valida.
     */
    public ConfiguracionSimulacion leer() {
        ConfiguracionSimulacion config = new ConfiguracionSimulacion(
                (Integer) duracion.getValue(),
                (Integer) quantum.getValue(),
                (Integer) latencia.getValue(),
                modeloComputadores.aConfiguracion(),
                buffers.aArreglo(new ConfigBuffer[buffers.tamano()]),
                procesos.aArreglo(new DatosProceso[procesos.tamano()]));
        config.validar();
        return config;
    }

    /**
     * Muestra una configuración en pantalla (al cargar un JSON).
     */
    public void mostrarConfiguracion(ConfiguracionSimulacion config) {
        cantidad.setValue(config.computadores().length);
        duracion.setValue(config.duracionCicloMs());
        quantum.setValue(config.quantum());
        latencia.setValue(config.latenciaRed());
        modeloComputadores.cargar(config.computadores());
        buffers.vaciar();
        if (config.buffers() != null) {
            for (ConfigBuffer b : config.buffers()) {
                buffers.agregarAlFinal(b);
            }
        }
        procesos.vaciar();
        if (config.procesos() != null) {
            for (DatosProceso p : config.procesos()) {
                procesos.agregarAlFinal(p);
            }
        }
        refrescarTablas();
    }

    private void iniciar() {
        try {
            alIniciar.ejecutar(leer());
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(),
                    "Configuración inválida", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void agregarBuffer() {
        ConfigBuffer b = DialogoBuffer.pedir(this, (Integer) cantidad.getValue());
        if (b != null) {
            buffers.agregarAlFinal(b);
            refrescarTablas();
        }
    }

    private void quitarUltimoBuffer() {
        if (buffers.estaVacia()) {
            return;
        }
        buffers.eliminar(buffers.obtener(buffers.tamano() - 1));
        refrescarTablas();
    }

    private void agregarProceso() {
        String[] nombres = new String[buffers.tamano()];
        for (int i = 0; i < nombres.length; i++) {
            nombres[i] = "B" + (i + 1);
        }
        DatosProceso p = DialogoProceso.pedir(this, (Integer) cantidad.getValue(), nombres, true);
        if (p != null) {
            procesos.agregarAlFinal(p);
            refrescarTablas();
        }
    }

    private void quitarProceso() {
        int fila = tablaProcesos.getSelectedRow();
        if (fila >= 0) {
            procesos.eliminar(modeloProcesos.getFila(fila));
            refrescarTablas();
        }
    }

    private void refrescarTablas() {
        modeloBuffers.setFilas(buffers.aArreglo(new ConfigBuffer[buffers.tamano()]));
        modeloProcesos.setFilas(procesos.aArreglo(new DatosProceso[procesos.tamano()]));
    }

    private void cargarArchivo() {
        JFileChooser selector = selectorDeArchivos();
        if (selector.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            mostrarConfiguracion(ArchivoConfiguracion.cargar(selector.getSelectedFile().toPath()));
        } catch (IOException | IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "No se pudo cargar el archivo",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    private void guardarArchivo() {
        ConfiguracionSimulacion config;
        try {
            config = leer();
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(),
                    "Configuración inválida", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JFileChooser selector = selectorDeArchivos();
        if (selector.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File archivo = selector.getSelectedFile();
        if (!archivo.getName().toLowerCase().endsWith(".json")) {
            archivo = new File(archivo.getParentFile(), archivo.getName()
                    + ".json");
        }
        try {
            ArchivoConfiguracion.guardar(config, archivo.toPath());
            JOptionPane.showMessageDialog(this, "Configuración guardada en\n"
                    + archivo.getAbsolutePath());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "No se pudo guardar", JOptionPane.WARNING_MESSAGE);
        }
    }

    private JFileChooser selectorDeArchivos() {
        File carpeta = new File("configuraciones");
        JFileChooser selector = new JFileChooser(carpeta.isDirectory()
                ? carpeta : new File("."));
        selector.setFileFilter(new FileNameExtensionFilter("Configuracion de AvilaOS (*.json)", "json"));
        return selector;
    }

    private static JPanel seccion(String titulo, JScrollPane tabla,
            JButton... botones) {
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        for (JButton b : botones) {
            barra.add(b);
        }
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(BorderFactory.createTitledBorder(titulo));
        panel.add(tabla, BorderLayout.CENTER);
        panel.add(barra, BorderLayout.SOUTH);
        return panel;
    }

    private Object valorBuffer(ConfigBuffer b, int columna) {
        return switch (columna) {
            case 0 ->
                nombreDeBuffer(b);
            case 1 ->
                b.capacidad();
            case 2 ->
                "Computador " + b.anfitrion();
            default ->
                (b.capacidad() * BufferAcotado.MB_POR_ESPACIO) + " MB";
        };
    }

    private String nombreDeBuffer(ConfigBuffer b) {
        ConfigBuffer[] todos = buffers.aArreglo(new ConfigBuffer[buffers.tamano()]);
        for (int i = 0; i < todos.length; i++) {
            if (todos[i] == b) {
                return "B" + (i + 1);
            }
        }
        return "?";
    }

    private static Object valorProceso(DatosProceso p, int columna) {
        return switch (columna) {
            case 0 ->
                p.nombre();
            case 1 ->
                p.tipo();
            case 2 ->
                p.computador() == 0 ? "Automático" : "Computador "
                + p.computador();
            case 3 ->
                p.memoriaMB();
            case 4 ->
                p.prioridad();
            case 5 ->
                p.deadline();
            case 6 ->
                p.totalInstrucciones();
            default ->
                p.buffer() == null ? "" : p.buffer();
        };
    }

    /**
     * Tabla editable de computadores: RAM y política inicial de cada uno.
     */
    private final class ModeloComputadores extends AbstractTableModel {

        private int[] ram = new int[0];
        private TipoPolitica[] politica = new TipoPolitica[0];

        void cargar(ConfigComputador[] computadores) {
            ram = new int[computadores.length];
            politica = new TipoPolitica[computadores.length];
            for (int i = 0; i < computadores.length; i++) {
                ram[i] = computadores[i].ramMB();
                politica[i] = computadores[i].politica();
            }
            fireTableDataChanged();
        }

        /**
         * Cambia la cantidad de computadores conservando lo ya escrito.
         */
        void redimensionar(int n) {
            int[] nuevaRam = new int[n];
            TipoPolitica[] nuevaPolitica = new TipoPolitica[n];
            for (int i = 0; i < n; i++) {
                nuevaRam[i] = i < ram.length ? ram[i] : 1024;
                nuevaPolitica[i] = i < politica.length ? politica[i]
                        : TipoPolitica.FCFS;
            }
            ram = nuevaRam;
            politica = nuevaPolitica;
            fireTableDataChanged();
        }

        ConfigComputador[] aConfiguracion() {
            ConfigComputador[] c = new ConfigComputador[ram.length];
            for (int i = 0; i < c.length; i++) {
                c[i] = new ConfigComputador(ram[i], politica[i]);
            }
            return c;
        }

        @Override
        public int getRowCount() {
            return ram.length;
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public String getColumnName(int columna) {
            return switch (columna) {
                case 0 ->
                    "Computador";
                case 1 ->
                    "RAM (MB)";
                default ->
                    "Política inicial";
            };
        }

        @Override
        public Class<?> getColumnClass(int columna) {
            return switch (columna) {
                case 1 ->
                    Integer.class; // JTable solo acepta enteros en esta columna
                case 2 ->
                    TipoPolitica.class;
                default ->
                    String.class;
            };
        }

        @Override
        public boolean isCellEditable(int fila, int columna) {
            return columna > 0;
        }

        @Override
        public Object getValueAt(int fila, int columna) {
            return switch (columna) {
                case 0 ->
                    "Computador " + (fila + 1);
                case 1 ->
                    ram[fila];
                default ->
                    politica[fila];
            };
        }

        @Override
        public void setValueAt(Object valor, int fila, int columna) {
            if (columna == 1) {
                int mb = (Integer) valor;
                if (mb < Limites.RAM_MIN_MB || mb > Limites.RAM_MAX_MB) {
                    JOptionPane.showMessageDialog(PanelConfiguracion.this,
                            "La RAM debe estar entre "
                            + Limites.RAM_MIN_MB + " y " + Limites.RAM_MAX_MB
                            + " MB.", "Dato inválido",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }
                ram[fila] = mb;
            } else if (columna == 2 && valor != null) {
                politica[fila] = (TipoPolitica) valor;
            }
            fireTableCellUpdated(fila, columna);
        }
    }
}
