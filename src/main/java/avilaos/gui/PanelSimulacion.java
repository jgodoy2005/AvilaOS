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
import avilaos.so.ModoUsuario;
import java.awt.Dimension;
import javax.swing.JComboBox;

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
    private final JButton nuevoProceso = new JButton("Nuevo proceso…");
    private final JSpinner quantum;
    private final Runnable alCambiar;                   //muestra enseguida un cambio hecho en pausa
    private final Sesion sesion = new Sesion();         //modo de usuario activo
    private final ModeloLista<ModoUsuario> modeloModo = new ModeloLista<>(ModoUsuario.values());
    private final ModeloLista<String> modeloUsuario;
    private final JComboBox<String> selectorUsuario;
    private final PanelDisco disco;
    private final PanelArchivos archivos;

     public PanelSimulacion(Simulacion simulacion, TipoPolitica[] politicasIniciales, Runnable alCambiar) {
        super(new BorderLayout(6, 6));
        this.simulacion = simulacion;
        this.alCambiar = alCambiar;
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
      //  JButton nuevoProceso = new JButton("Nuevo proceso…");
      //  JButton nuevoBuffer = new JButton("Nuevo buffer…");
        nuevoProceso.addActionListener(e -> crearProceso());
     //   nuevoBuffer.addActionListener(e -> crearBuffer());
        JSpinner duracion = spinner(simulacion.getDuracionCicloMs(),
                Limites.DURACION_CICLO_MIN_MS,
                Limites.DURACION_CICLO_MAX_MS, 50);
        duracion.addChangeListener(e
                -> simulacion.setDuracionCicloMs((Integer) duracion.getValue()));
        quantum = spinner(simulacion.getQuantum(),
                Limites.QUANTUM_MIN, Limites.QUANTUM_MAX, 1);
        quantum.addChangeListener(e -> {
            if (sesion.esAdministrador()) {
                simulacion.setQuantum((Integer) quantum.getValue());
            }
        });        
        
 //       JSpinner latencia = spinner(simulacion.getLatenciaRed(),
   //             Limites.LATENCIA_MIN, Limites.LATENCIA_MAX, 1);
   //     latencia.addChangeListener(e -> simulacion.setLatenciaRed((Integer) latencia.getValue()));

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
      //  controles.add(new JLabel("Latencia de red (ciclos):"));
      //  controles.add(latencia);
        controles.add(nuevoProceso);
       // controles.add(nuevoBuffer);

       // ----- Modo de uso: administrador o usuario (y cual) -----
        modeloUsuario = new ModeloLista<>(simulacion.getUsuarios());
        selectorUsuario = new JComboBox<>(modeloUsuario);
        selectorUsuario.setEnabled(false);
        JComboBox<ModoUsuario> selectorModo = new JComboBox<>(modeloModo);
        selectorModo.addActionListener(e -> cambiarModo());
        selectorUsuario.addActionListener(e -> cambiarModo());
        controles.add(new JLabel("  Modo:"));
        controles.add(selectorModo);
        controles.add(selectorUsuario);
        
// ----- Un panel por computador -----
        computadores = new PanelComputador[politicasIniciales.length];
        JPanel fila = new JPanel(new GridLayout(1, computadores.length, 8,
                0));
        for (int i = 0; i < computadores.length; i++) {
            computadores[i] = new PanelComputador(i + 1,
                    politicasIniciales[i], simulacion, sesion);
            fila.add(computadores[i]);
        }
        JScrollPane scrollComputadores = new JScrollPane(fila);
        
        // ----- Disco y sistema de archivos, a la derecha del computador -----
        disco = new PanelDisco(simulacion, alCambiar, sesion);
        archivos = new PanelArchivos(simulacion, sesion, alCambiar, disco::resaltar);
        //izquierda: el computador y debajo el registro; derecha: el disco y debajo el sistema de archivos
        log.setPreferredSize(new Dimension(560, 200));
        JSplitPane izquierda = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollComputadores, log);
        izquierda.setResizeWeight(0.75);
        JSplitPane derecha = new JSplitPane(JSplitPane.VERTICAL_SPLIT, disco, archivos);
        derecha.setResizeWeight(0.60);
        derecha.setDividerLocation(380);
        izquierda.setMinimumSize(new Dimension(300, 200));
        derecha.setMinimumSize(new Dimension(300, 200));
        JSplitPane division = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, izquierda, derecha);
        division.setResizeWeight(0.45);
        division.setDividerLocation(590);   //el ancho del panel del computador
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
        disco.mostrar(f.disco());
        archivos.mostrar(f);
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
         if (!sesion.esAdministrador()) {
            return;
        }
        DatosProceso datos = DialogoProceso.pedir(this, computadores.length,
                nombresBuffers, true);
        if (datos == null) {
            return; // canceló
        }
        try {
            String creado = simulacion.crearProceso(datos);
            estado.setText("Se creó " + creado);
            alCambiar.run();
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
    
    //la misma sesión determina la cuenta de archivos y los controles habilitados
    private void cambiarModo() {
        ModoUsuario modo = (ModoUsuario) modeloModo.getSelectedItem();
        sesion.cambiar(modo, (String) modeloUsuario.getSelectedItem());
        selectorUsuario.setEnabled(modo == ModoUsuario.USUARIO);
        sesion.actualizarControlAdministrativo(nuevoProceso, true);
        sesion.actualizarControlAdministrativo(quantum, true);
        for (PanelComputador computador : computadores) {
            computador.actualizarPermisos();
        }
        disco.actualizarPermisos();
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
