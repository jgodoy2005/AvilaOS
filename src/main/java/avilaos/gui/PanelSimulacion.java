/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.gui;

import avilaos.Limites;
import avilaos.config.ConfiguracionSimulacion.ConfigBuffer;
import avilaos.estructuras.Cola;
import avilaos.simulacion.FotoMetricas;
import avilaos.simulacion.FotoSimulacion;
import avilaos.simulacion.Simulacion;
import avilaos.so.DatosProceso;
import avilaos.so.planificacion.TipoPolitica;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.text.DefaultFormatter;

/**
 *
 * @author erick
 */
public class PanelSimulacion extends JPanel {

    private final Simulacion simulacion;
    private final PanelComputador[] computadores;
    private final PanelBuffers buffers = new PanelBuffers();
    private final PanelLog log = new PanelLog();
    private final JLabel ciclo = new JLabel("Ciclo 0");
    private final JLabel estado = new JLabel("En pausa");
    private final JLabel resumen = new JLabel(" ");
    private final JButton reanudar = new JButton("Reanudar");
    private final JButton pausar = new JButton("Pausar");
    private final JButton paso = new JButton("Avanzar 1 ciclo");
    private String[] nombresBuffers = new String[0];

    public PanelSimulacion(Simulacion simulacion, TipoPolitica[] politicasIniciales) {
        super(new BorderLayout(6, 6));
        this.simulacion = simulacion;
// ----- Controles del reloj -----
        ciclo.setFont(ciclo.getFont().deriveFont(Font.BOLD, 20f));
        reanudar.addActionListener(e -> {
            simulacion.reanudar();
            actualizarBotones();
        });
        pausar.addActionListener(e -> {
            simulacion.pausar();
            actualizarBotones();
        });
        paso.addActionListener(e -> simulacion.avanzarUnCiclo());
        JButton nuevoProceso = new JButton("Nuevo proceso…");
        JButton nuevoBuffer = new JButton("Nuevo buffer…");
        nuevoProceso.addActionListener(e -> crearProceso());
        nuevoBuffer.addActionListener(e -> crearBuffer());
        JSpinner duracion = spinner(simulacion.getDuracionCicloMs(),
                Limites.DURACION_CICLO_MIN_MS,
                Limites.DURACION_CICLO_MAX_MS, 50);
        duracion.addChangeListener(e
                -> simulacion.setDuracionCicloMs((Integer) duracion.getValue()));
        JSpinner quantum = spinner(simulacion.getQuantum(),
                Limites.QUANTUM_MIN, Limites.QUANTUM_MAX, 1);
        quantum.addChangeListener(e -> simulacion.setQuantum((Integer) quantum.getValue()));
        JSpinner latencia = spinner(simulacion.getLatenciaRed(),
                Limites.LATENCIA_MIN, Limites.LATENCIA_MAX, 1);
        latencia.addChangeListener(e -> simulacion.setLatenciaRed((Integer) latencia.getValue()));

        JPanel controles = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        controles.add(ciclo);
        controles.add(estado);
        controles.add(reanudar);
        controles.add(pausar);
        controles.add(paso);
        controles.add(new JLabel(" Duración del ciclo (ms):"));
        controles.add(duracion);
        controles.add(new JLabel("Quantum RR:"));
        controles.add(quantum);
        controles.add(new JLabel("Latencia de red (ciclos):"));
        controles.add(latencia);
        controles.add(nuevoProceso);
        controles.add(nuevoBuffer);

// ----- Un panel por computador -----
        computadores = new PanelComputador[politicasIniciales.length];
        JPanel fila = new JPanel(new GridLayout(1, computadores.length, 8,
                0));
        for (int i = 0; i < computadores.length; i++) {
            computadores[i] = new PanelComputador(i + 1,
                    politicasIniciales[i], simulacion);
            fila.add(computadores[i]);
        }
        JScrollPane scrollComputadores = new JScrollPane(fila);
        scrollComputadores.getVerticalScrollBar().setUnitIncrement(16);
        scrollComputadores.getHorizontalScrollBar().setUnitIncrement(16);
        JSplitPane abajo = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buffers, log);
        abajo.setResizeWeight(0.55);
        JSplitPane division = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                scrollComputadores, abajo);
        division.setResizeWeight(0.8);
        resumen.setBorder(BorderFactory.createEmptyBorder(0, 8, 4, 8));
        JPanel norte = new JPanel(new BorderLayout());
        norte.add(controles, BorderLayout.NORTH);
        norte.add(resumen, BorderLayout.SOUTH);
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        add(norte, BorderLayout.NORTH);
        add(division, BorderLayout.CENTER);
    }

    /**
     * Dibuja una foto completa. Siempre en el hilo de Swing.
     */
    public void mostrar(FotoSimulacion f) {
        ciclo.setText("Ciclo " + f.ciclo());
        actualizarBotones();
        for (int i = 0; i < computadores.length; i++) {
            computadores[i].mostrar(f.computadores()[i]);
        }
        buffers.mostrar(f.buffers());
        nombresBuffers = f.nombresBuffers();
        FotoMetricas g = f.global();
        resumen.setText("Sistema completo: utilización "
                + Formato.porcentaje(g.utilizacion())
                + " (útil " + Formato.porcentaje(g.utilizacionUsuario()) + ") · completados " + g.completados()
                + " · abortados " + g.abortados() + " · deadlines cumplidos " + Formato.porcentaje(g.tasaDeadlines())
                + " · throughput " + Formato.decimal(g.throughput()) + " por 100 ciclos"
                + " · respuesta promedio "
                + (Double.isNaN(g.respuestaPromedio()) ? "—" : Formato.decimal(g.respuestaPromedio()) + " ciclos"));
    }

    public void agregarEventos(Cola<String> eventos) {
        log.agregar(eventos);
    }

    /**
     * Habilita los botones según el estado real del reloj.
     */
    private void actualizarBotones() {
        boolean pausada = simulacion.isPausada();
        estado.setText(pausada ? "En pausa" : "Corriendo");
        reanudar.setEnabled(pausada);
        paso.setEnabled(pausada);
        pausar.setEnabled(!pausada);
    }

    private void crearProceso() {
        DatosProceso datos = DialogoProceso.pedir(this, computadores.length,
                nombresBuffers, true);
        if (datos == null) {
            return; // canceló
        }
        try {
            String creado = simulacion.crearProceso(datos);
            estado.setText("Se creó " + creado);
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "No se pudo crear el proceso",
                    JOptionPane.WARNING_MESSAGE
            );

        }
    }

    private void crearBuffer() {
        ConfigBuffer datos = DialogoBuffer.pedir(this, computadores.length);
        if (datos == null) {
            return;
        }
        try {
            String nombre = simulacion.crearBuffer(datos.capacidad(),
                    datos.anfitrion());
// se agrega ya a la lista, para poder usarlo aunque la simulación esté en pausa
            String[] nuevos = new String[nombresBuffers.length + 1];
            System.arraycopy(nombresBuffers, 0, nuevos, 0,
                    nombresBuffers.length);
            nuevos[nombresBuffers.length] = nombre;
            nombresBuffers = nuevos;
            estado.setText("Se creó el buffer " + nombre);
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "No se pudo crear el buffer",
                    JOptionPane.WARNING_MESSAGE
            );
        }
    }

    /**
     * Un JSpinner numérico: solo acepta enteros dentro del rango (validación de
     * tipo y rango).
     */
    static JSpinner spinner(int valor, int min, int max, int paso) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(valor, min,
                max, paso));
        JFormattedTextField campo = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
        campo.setColumns(5);
        ((DefaultFormatter) campo.getFormatter()).setCommitsOnValidEdit(true); // aplica al escribir,si es válido
        return spinner;
    }
}
