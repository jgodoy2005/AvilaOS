package avilaos.gui;

import avilaos.hardware.ModoCPU;
import avilaos.simulacion.FotoComputador;
import avilaos.simulacion.FotoPCB;
import avilaos.simulacion.Simulacion;
import avilaos.so.planificacion.TipoPolitica;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;

/**
 * Vista 2, la parte de UN computador: su CPU (proceso, PC, MAR, modo y uso
 * reciente), su memoria, su dispositivo de E/S, el mapa de estados y sus cuatro colas
 * con los datos del PCB de cada proceso. Se crea una instancia por computador.
 */
public class PanelComputador extends JPanel {
    private static final String[] COLUMNAS_COLA =
        {"Proceso", "Nombre", "Tipo", "Prio.", "Espera", "MB", "Vence", "Faltan", "Restantes", "PC", "MAR"};
    private static final String[] COLUMNAS_BLOQUEADOS =
        {"Proceso", "Nombre", "Prio.", "Faltan", "PC", "Motivo"};
    private static final String[] COLUMNAS_TERMINADOS =
        {"Proceso", "Nombre", "Tipo", "Ejecutó", "Elementos", "Resultado"};

    private static final Color VERDE = new Color(200, 240, 200);
    private static final Color NARANJA = new Color(255, 225, 180);
    private static final Color GRIS = new Color(225, 225, 225);

    private final TitledBorder borde;
    private final JLabel politicaEnUso = new JLabel();
    private final JLabel proceso = new JLabel("-");
    private final JLabel modo = new JLabel("Ociosa", SwingConstants.CENTER);
    private final JLabel pc = new JLabel("-");
    private final JLabel mar = new JLabel("-");
    private final JLabel instruccion = new JLabel("-");
    private final JLabel prioridad = new JLabel("-");
    private final JLabel deadline = new JLabel("-");
    private final JProgressBar memoria = new JProgressBar();
    private final JProgressBar usoCpu = new JProgressBar(0, 100);
    private final JLabel dispositivo = new JLabel("Libre");

    private final PanelEstados mapa = new PanelEstados(this::mostrarPCB);

    private final ModeloTabla<FotoPCB> nuevos = new ModeloTabla<>(COLUMNAS_COLA, this::valorCola);
    private final ModeloTabla<FotoPCB> listos = new ModeloTabla<>(COLUMNAS_COLA, this::valorCola);
    private final ModeloTabla<FotoPCB> bloqueados = new ModeloTabla<>(COLUMNAS_BLOQUEADOS, this::valorBloqueado);
    private final ModeloTabla<FotoPCB> terminados = new ModeloTabla<>(COLUMNAS_TERMINADOS, PanelComputador::valorTerminado);

    private final TitledBorder bordeNuevos = BorderFactory.createTitledBorder("Nuevos");
    private final TitledBorder bordeListos = BorderFactory.createTitledBorder("Listos");
    private final TitledBorder bordeBloqueados = BorderFactory.createTitledBorder("Bloqueados");
    private final TitledBorder bordeTerminados = BorderFactory.createTitledBorder("Terminados");

    private FotoPCB enEjecucion;

    public PanelComputador(int numero, TipoPolitica politicaInicial, Simulacion simulacion) {
        super(new BorderLayout(4, 4));
        borde = BorderFactory.createTitledBorder("Computador " + numero);
        borde.setTitleFont(borde.getTitleFont() == null ? new Font(Font.SANS_SERIF, Font.BOLD, 14)
                : borde.getTitleFont().deriveFont(Font.BOLD, 14f));
        setBorder(borde);

        // Selector de política: el cambio rige desde el próximo ciclo
        ModeloLista<TipoPolitica> modeloPolitica = new ModeloLista<>(TipoPolitica.values());
        modeloPolitica.setSelectedItem(politicaInicial);
        JComboBox<TipoPolitica> selector = new JComboBox<>(modeloPolitica);
        selector.addActionListener(e -> {
            if (simulacion != null) {
                simulacion.cambiarPolitica(numero, (TipoPolitica) modeloPolitica.getSelectedItem());
            }
        });

        JPanel barraPolitica = new JPanel(new BorderLayout(6, 0));
        barraPolitica.add(new JLabel("Politica:"), BorderLayout.WEST);
        barraPolitica.add(selector, BorderLayout.CENTER);
        barraPolitica.add(politicaEnUso, BorderLayout.EAST);

        // Procesador
        modo.setOpaque(true);
        modo.setFont(modo.getFont().deriveFont(Font.BOLD));

        JPanel cpu = Formato.formulario();
        Formato.filaCompacta(cpu, 0, "Ejecutando:", modo);
        Formato.filaCompacta(cpu, 1, "Proceso:", proceso);
        Formato.filaCompacta(cpu, 2, "Instrucción:", instruccion);

        JPanel registros = new JPanel(new GridLayout(1, 3, 8, 0));
        registros.add(pc);
        registros.add(mar);
        registros.add(prioridad);

        Formato.filaCompacta(cpu, 3, "Registros:", registros);
        Formato.filaCompacta(cpu, 4, "Deadline:", deadline);

        memoria.setStringPainted(true);
        Formato.filaCompacta(cpu, 5, "Memoria:", memoria);

        usoCpu.setStringPainted(true);
        Formato.filaCompacta(cpu, 6, "Uso de la CPU:", usoCpu);
        Formato.filaCompacta(cpu, 7, "Dispositivo E/S:", dispositivo);

        cpu.setBorder(BorderFactory.createTitledBorder("CPU"));
        mapa.setBorder(BorderFactory.createTitledBorder("Mapa de estados (mouse encima: detalle; doble clic: PCB)"));

        JPanel arriba = new JPanel(new BorderLayout(4, 4));
        arriba.add(barraPolitica, BorderLayout.NORTH);
        arriba.add(cpu, BorderLayout.CENTER);
        arriba.add(mapa, BorderLayout.SOUTH);

        JPanel colas = new JPanel();
        colas.setLayout(new BoxLayout(colas, BoxLayout.Y_AXIS));
        colas.add(tabla(nuevos, bordeNuevos, 2, 0));
        colas.add(tabla(listos, bordeListos, 3, 0));
        colas.add(tabla(bloqueados, bordeBloqueados, 3, 230));
        colas.add(tabla(terminados, bordeTerminados, 2, 0));

        add(arriba, BorderLayout.NORTH);
        add(colas, BorderLayout.CENTER);
        setPreferredSize(new Dimension(560, 800));
    }

    /** Dibuja la foto de este computador. Siempre en el hilo de Swing. */
    public void mostrar(FotoComputador f) {
        politicaEnUso.setText("en uso: " + f.politica());
        enEjecucion = f.enEjecucion();
        if (f.cpuOciosa()) {
            modo.setText("CPU ociosa");
            modo.setBackground(GRIS);
        } else if (f.modo() == ModoCPU.USUARIO) {
            modo.setText(ModoCPU.USUARIO.toString());
            modo.setBackground(VERDE);
        } else {
            modo.setText(ModoCPU.SISTEMA_OPERATIVO.toString());
            modo.setBackground(NARANJA);
        }
        instruccion.setText(f.descripcionCPU());

        if (enEjecucion == null) {
            proceso.setText("-");
            pc.setText("PC: -");
            mar.setText("MAR: -");
            prioridad.setText("Prio.: -");
            deadline.setText("-");
        } else {
            proceso.setText(enEjecucion.etiqueta() + " " + enEjecucion.nombre() + " " + enEjecucion.tipo());
            pc.setText("PC: " + enEjecucion.pc());
            mar.setText("MAR: " + enEjecucion.mar());
            prioridad.setText("Prio.: " + enEjecucion.prioridad());
            deadline.setText("vence en el ciclo " + enEjecucion.cicloLimite() + " faltan " + enEjecucion.deadlineRestante());
        }

        memoria.setMaximum(f.memoriaTotalMB());
        memoria.setValue(f.memoriaUsadaMB());
        memoria.setString(f.memoriaUsadaMB() + " MB usados / " + f.memoriaLibreMB() + " MB libres");

        int uso = (int) Math.round(f.metricas().utilizacionVentana());
        usoCpu.setValue(uso);
        usoCpu.setString(uso + "% en los últimos " + f.metricas().tamanoVentana() + " ciclos");

        dispositivo.setText(f.estadoES());
        mapa.mostrar(f);

        nuevos.setFilas(f.nuevos());
        listos.setFilas(f.listos());
        bloqueados.setFilas(f.bloqueados());
        terminados.setFilas(f.terminados());

        bordeNuevos.setTitle("Nuevos (" + f.nuevos().length + ")");
        bordeListos.setTitle("Listos (" + f.listos().length + ") - en el orden de la política");
        bordeBloqueados.setTitle("Bloqueados (" + f.bloqueados().length + ")");
        bordeTerminados.setTitle("Terminados (" + f.terminados().length + ")");

        repaint();
    }

    private JScrollPane tabla(ModeloTabla<FotoPCB> modelo, TitledBorder titulo, int filasVisibles, int anchoUltima) {
        JTable tabla = new JTable(modelo);
        tabla.setFillsViewportHeight(true);
        if (anchoUltima > 0) {
            tabla.getColumnModel().getColumn(modelo.getColumnCount() - 1).setPreferredWidth(anchoUltima);
        }
        tabla.setPreferredScrollableViewportSize(new Dimension(520, filasVisibles * tabla.getRowHeight()));
        tabla.setToolTipText("Doble clic para ver el PCB completo");
        tabla.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int fila = tabla.rowAtPoint(e.getPoint());
                if (e.getClickCount() == 2 && fila >= 0) {
                    mostrarPCB(modelo.getFila(tabla.convertRowIndexToModel(fila)));
                }
            }
        });

        JScrollPane panel = new JScrollPane(tabla);
        panel.setBorder(titulo);
        return panel;
    }

    /** Todos los campos del PCB en una ventanita. */
    private void mostrarPCB(FotoPCB p) {
        String texto = "ID: " + p.id() + " (" + p.etiqueta() + ")\n"
                + "Nombre: " + p.nombre() + "\n"
                + "Computador: " + p.computador() + "\n"
                + "Estado: " + p.estado() + (p.motivo().isEmpty() ? "" : " " + p.motivo()) + "\n"
                + "Tipo: " + p.tipo() + "\n"
                + "Prioridad: " + p.prioridad() + " (lleva " + p.ciclosEsperando() + " ciclos esperando)\n"
                + "Memoria asignada: " + p.memoriaMB() + " MB\n"
                + "Deadline: " + p.deadline() + " ciclos. vence en el ciclo " + p.cicloLimite() + " faltan " + p.deadlineRestante() + "\n"
                + "PC: " + p.pc() + " MAR: " + p.mar() + "\n"
                + "Instrucción en el PC: " + (p.instruccion().isEmpty() ? "-" : p.instruccion()) + "\n"
                + "Tiempo restante: " + p.instruccionesRestantes() + " de " + p.totalInstrucciones() + " instrucciones\n"
                + (p.buffer().isEmpty() ? "" : "Buffer: " + p.buffer() + " elementos: " + p.progresoElementos() + "\n")
                + "Ciclos ejecutados: " + p.ciclosEjecutados() + "\n"
                + "Ciclos bloqueado en semáforos: " + p.ciclosBloqueadoSemaforo() + " en la red: " + p.ciclosBloqueadoRed();

        JTextArea area = new JTextArea(texto);
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JOptionPane.showMessageDialog(this, area, "PCB de " + p.etiqueta(), JOptionPane.INFORMATION_MESSAGE);
    }

    private Object valorCola(FotoPCB p, int columna) {
        return switch (columna) {
            case 0 -> p.etiqueta();
            case 1 -> p.nombre();
            case 2 -> p.tipo();
            case 3 -> p.prioridad();
            case 4 -> p.ciclosEsperando();
            case 5 -> p.memoriaMB();
            case 6 -> p.cicloLimite();
            case 7 -> p.deadlineRestante();
            case 8 -> p.instruccionesRestantes();
            case 9 -> p.pc();
            default -> p.mar();
        };
    }

    private Object valorBloqueado(FotoPCB p, int columna) {
        return switch (columna) {
            case 0 -> p.etiqueta();
            case 1 -> p.nombre();
            case 2 -> p.prioridad();
            case 3 -> p.deadlineRestante();
            case 4 -> p.pc();
            default -> p.motivo();
        };
    }

    private static Object valorTerminado(FotoPCB p, int columna) {
        return switch (columna) {
            case 0 -> p.etiqueta();
            case 1 -> p.nombre();
            case 2 -> p.tipo();
            case 3 -> p.ciclosEjecutados() + " ciclos";
            case 4 -> p.progresoElementos();
            default -> p.motivo();
        };
    }
}