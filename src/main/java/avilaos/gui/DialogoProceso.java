package avilaos.gui;

import avilaos.Limites;
import avilaos.so.DatosProceso;
import avilaos.so.Kernel;
import avilaos.so.TipoProceso;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Window;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Vista 1: formulario para crear un proceso. Según el tipo, muestra los campos
 * de cálculo/E-S o los de productor/consumidor. Valida todo antes de aceptar.
 */
public class DialogoProceso extends JDialog {

    private static final String AUTOMATICO = "Automático (menor carga)";

    /**
     * El deadline sugerido es este múltiplo del mínimo, para dejar margen si el
     * proceso espera turno.
     */
    private static final int VECES_EL_MINIMO = 3;

    private final JTextField nombre = new JTextField("Proceso", 14);
    private final ModeloLista<TipoProceso> modeloTipo = new ModeloLista<>(TipoProceso.values());
    private final JComboBox<TipoProceso> tipo = new JComboBox<>(modeloTipo);
    private final ModeloLista<String> modeloComputador;
    private final JTextField memoria = new JTextField("64", 6);
    private final JTextField prioridad = new JTextField("5", 6);
    private final JTextField deadline = new JTextField("300", 6);
    private final JLabel deadlineSugerido = new JLabel();

    // CPU bound / I/O bound
    private final JTextField instrucciones = new JTextField("40", 6);
    private final JTextField ciclosEntreES = new JTextField("0", 6);
    private final JTextField duracionES = new JTextField("3", 6);

    // Productor / consumidor
    private final ModeloLista<String> modeloBuffer;
    private final JTextField ciclosPorElemento = new JTextField("2", 6);
    private final JTextField elementos = new JTextField("10", 6);
    private final JLabel totalCalculado = new JLabel();

    private final CardLayout tarjetas = new CardLayout();
    private final JPanel panelSegunTipo = new JPanel(tarjetas);
    private final boolean permitirAutomatico;
    private DatosProceso resultado;

    private DialogoProceso(Window padre, int cantidadComputadores, String[] buffers, boolean permitirAutomatico) {
        super(padre, "Nuevo proceso (Vista 1)", ModalityType.APPLICATION_MODAL);
        this.permitirAutomatico = permitirAutomatico;

        String[] opcionesComputador = new String[cantidadComputadores + (permitirAutomatico ? 1 : 0)];
        int idx = 0;
        if (permitirAutomatico) {
            opcionesComputador[idx++] = AUTOMATICO;
        }
        for (int c = 1; c <= cantidadComputadores; c++) {
            opcionesComputador[idx++] = "Computador " + c;
        }

        modeloComputador = new ModeloLista<>(opcionesComputador);
        modeloBuffer = new ModeloLista<>(buffers.length == 0 ? new String[]{"(no hay buffers)"} : buffers);

        JPanel comunes = Formato.formulario();
        Formato.fila(comunes, 0, "Nombre:", nombre);
        Formato.fila(comunes, 1, "Tipo:", tipo);
        Formato.fila(comunes, 2, "Computador:", new JComboBox<>(modeloComputador));
        Formato.fila(comunes, 3, "Memoria (MB):", memoria);
        Formato.fila(comunes, 4, "Prioridad (1 = la más alta, hasta 10):", prioridad);
        Formato.fila(comunes, 5, "Deadline (ciclos):", deadline);
        Formato.fila(comunes, 6, "Deadline sugerido:", deadlineSugerido);
        deadlineSugerido.setToolTipText("El mínimo es lo que tarda el proceso con la CPU y el dispositivo libres. "
                + "El sugerido es " + VECES_EL_MINIMO + " veces el mínimo, para dejar margen si comparte la CPU.");
        comunes.setBorder(BorderFactory.createTitledBorder("Datos del PCB"));

        JPanel calculo = Formato.formulario();
        Formato.fila(calculo, 0, "Cantidad de instrucciones:", instrucciones);
        Formato.fila(calculo, 1, "Pide E/S cada (instrucciones, 0 = nunca):", ciclosEntreES);
        Formato.fila(calculo, 2, "Ciclos para satisfacer cada E/S:", duracionES);
        calculo.setBorder(BorderFactory.createTitledBorder("CPU bound / I/O bound"));

        JPanel productorConsumidor = Formato.formulario();
        Formato.fila(productorConsumidor, 0, "Buffer:", new JComboBox<>(modeloBuffer));
        Formato.fila(productorConsumidor, 1, "Ciclos por elemento (k):", ciclosPorElemento);
        Formato.fila(productorConsumidor, 2, "Elementos para terminar (M):", elementos);
        Formato.fila(productorConsumidor, 3, "Instrucciones totales:", totalCalculado);
        productorConsumidor.setBorder(BorderFactory.createTitledBorder("Productor / consumidor"));

        panelSegunTipo.add(calculo, "calculo");
        panelSegunTipo.add(productorConsumidor, "buffer");

        tipo.addActionListener(e -> cambiarTipo());

        DocumentListener recalcular = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                actualizarCalculos();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                actualizarCalculos();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                actualizarCalculos();
            }
        };

        for (JTextField campo : new JTextField[]{instrucciones, ciclosEntreES, duracionES, ciclosPorElemento, elementos}) {
            campo.getDocument().addDocumentListener(recalcular);
        }

        cambiarTipo();

        JButton crear = new JButton("Crear");
        JButton cancelar = new JButton("Cancelar");
        crear.addActionListener(e -> aceptar());
        cancelar.addActionListener(e -> dispose());

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        botones.add(cancelar);
        botones.add(crear);
        getRootPane().setDefaultButton(crear);

        JPanel contenido = new JPanel(new BorderLayout(8, 8));
        contenido.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        contenido.add(comunes, BorderLayout.NORTH);
        contenido.add(panelSegunTipo, BorderLayout.CENTER);
        contenido.add(botones, BorderLayout.SOUTH);

        setContentPane(contenido);
        pack();
        setLocationRelativeTo(padre);
    }

    /**
     * Muestra el diálogo y espera.
     *
     * @return los datos del proceso, o null si el usuario canceló
     */
    public static DatosProceso pedir(Component padre, int cantidadComputadores, String[] buffers, boolean permitirAutomatico) {
        Window ventana = padre == null ? null : SwingUtilities.getWindowAncestor(padre);
        DialogoProceso dialogo = new DialogoProceso(ventana, cantidadComputadores, buffers, permitirAutomatico);
        dialogo.setVisible(true);
        return dialogo.resultado;
    }

    private void cambiarTipo() {
        TipoProceso t = (TipoProceso) modeloTipo.getSelectedItem();
        tarjetas.show(panelSegunTipo, t.usaBuffer() ? "buffer" : "calculo");
        switch (t) {
            case CPU_BOUND -> {
                ciclosEntreES.setText("0");
                duracionES.setText("3");
                instrucciones.setText("40");
            }
            case IO_BOUND -> {
                ciclosEntreES.setText("3");
                duracionES.setText("5");
                instrucciones.setText("30");
            }
            case PRODUCTOR -> {
                ciclosPorElemento.setText("2");
                elementos.setText("10");
            }
            case CONSUMIDOR -> {
                ciclosPorElemento.setText("3");
                elementos.setText("10");
            }
        }
        boolean pideES = t == TipoProceso.IO_BOUND;   // un CPU bound no pide E/S
        ciclosEntreES.setEnabled(pideES);
        duracionES.setEnabled(pideES);
        actualizarCalculos();
    }

    private void actualizarCalculos() {
        actualizarTotal();
        actualizarSugerido();
    }

    private void actualizarTotal() {
        try {
            int k = Integer.parseInt(ciclosPorElemento.getText().trim());
            int m = Integer.parseInt(elementos.getText().trim());
            totalCalculado.setText(m + " × (" + k + " + " + DatosProceso.OPERACIONES_POR_ELEMENTO + ") = "
                    + (m * (k + DatosProceso.OPERACIONES_POR_ELEMENTO)));
        } catch (NumberFormatException e) {
            totalCalculado.setText("-");
        }
    }

    private void actualizarSugerido() {
        TipoProceso t = (TipoProceso) modeloTipo.getSelectedItem();
        int minimo;
        try {
            if (t.usaBuffer()) {
                int k = Integer.parseInt(ciclosPorElemento.getText().trim());
                int m = Integer.parseInt(elementos.getText().trim());
                if (k < Limites.CICLOS_POR_ELEMENTO_MIN || k > Limites.CICLOS_POR_ELEMENTO_MAX
                        || m < Limites.ELEMENTOS_MIN || m > Limites.ELEMENTOS_MAX) {
                    throw new NumberFormatException();
                }
                minimo = Kernel.ciclosMinimos(t, 0, m * (k + DatosProceso.OPERACIONES_POR_ELEMENTO), 0, 1, false);
            } else {
                int ins = Integer.parseInt(instrucciones.getText().trim());
                int cada = Integer.parseInt(ciclosEntreES.getText().trim());
                int dur = Integer.parseInt(duracionES.getText().trim());
                if (ins < Limites.INSTRUCCIONES_MIN || ins > Limites.INSTRUCCIONES_MAX
                        || cada < Limites.CICLOS_ENTRE_ES_MIN || cada > Limites.CICLOS_ENTRE_ES_MAX
                        || dur < Limites.DURACION_ES_MIN || dur > Limites.DURACION_ES_MAX) {
                    throw new NumberFormatException();
                }
                minimo = Kernel.ciclosMinimos(t, 0, ins, cada, dur, false);
            }
        } catch (NumberFormatException e) {
            deadlineSugerido.setText("-");
            return;
        }

        if (minimo > Limites.DEADLINE_MAX) {
            deadlineSugerido.setText("no hay: el mínimo (" + minimo + ") supera el máximo permitido");
            return;
        }

        long sugerido = Math.min((long) VECES_EL_MINIMO * minimo, Limites.DEADLINE_MAX);
        deadlineSugerido.setText(sugerido + " (el mínimo es " + minimo + ")");
    }

    private void aceptar() {
        Validacion v = new Validacion();
        String n = v.texto(nombre, "El nombre", Limites.NOMBRE_MAX);
        TipoProceso t = (TipoProceso) modeloTipo.getSelectedItem();
        int mem = v.entero(memoria, "La memoria", Limites.MEMORIA_PROCESO_MIN_MB, Limites.MEMORIA_PROCESO_MAX_MB);
        int prio = v.entero(prioridad, "La prioridad", Limites.PRIORIDAD_MIN, Limites.PRIORIDAD_MAX);
        int dl = v.entero(deadline, "El deadline", Limites.DEADLINE_MIN, Limites.DEADLINE_MAX);
        int indice = modeloComputador.indiceSeleccionado();
        int computador = permitirAutomatico ? indice : indice + 1;

        DatosProceso datos;
        if (t.usaBuffer()) {
            String buffer = (String) modeloBuffer.getSelectedItem();
            if (buffer == null || buffer.startsWith("(")) {
                v.error("No hay buffers: cree uno antes de crear productores o consumidores.");
            }
            int k = v.entero(ciclosPorElemento, "La cantidad de ciclos por elemento",
                    Limites.CICLOS_POR_ELEMENTO_MIN, Limites.CICLOS_POR_ELEMENTO_MAX);
            int m = v.entero(elementos, "La cantidad de elementos", Limites.ELEMENTOS_MIN, Limites.ELEMENTOS_MAX);
            datos = DatosProceso.deBuffer(n, t, buffer, k, m, mem, prio, dl, computador);
        } else {
            int ins = v.entero(instrucciones, "La cantidad de instrucciones", Limites.INSTRUCCIONES_MIN, Limites.INSTRUCCIONES_MAX);
            int minimoES = t == TipoProceso.IO_BOUND ? 1 : Limites.CICLOS_ENTRE_ES_MIN;   // un I/O bound debe pedir E/S
            int cada = v.entero(ciclosEntreES, "La frecuencia de E/S", minimoES, Limites.CICLOS_ENTRE_ES_MAX);
            int dur = v.entero(duracionES, "La duración de la E/S", Limites.DURACION_ES_MIN, Limites.DURACION_ES_MAX);
            datos = DatosProceso.deCalculo(n, t, ins, mem, prio, dl, cada, dur, computador);
        }

        if (v.hayErrores()) {
            v.mostrar(this);
            return;
        }

        if (!deadlineAlcanza(datos)) {
            return;
        }

        resultado = datos;
        dispose();
    }

    private boolean deadlineAlcanza(DatosProceso datos) {
        int total = datos.totalInstrucciones();
        int minimo = Kernel.ciclosMinimos(datos.tipo(), 0, total, datos.ciclosEntreES(), datos.duracionES(), false);
        if (datos.deadline() >= minimo) {
            return true;
        }

        boolean conES = !datos.tipo().usaBuffer() && datos.ciclosEntreES() > 0 && total >= datos.ciclosEntreES();
        int respuesta = JOptionPane.showConfirmDialog(this,
                "Con un deadline de " + datos.deadline() + " ciclos, este proceso no alcanza a terminar aunque tenga\n"
                + "la CPU" + (conES ? " y el dispositivo libres" : " libre") + ": necesita al menos " + minimo + " ciclos\n"
                + "(el despacho, sus " + total + " instrucciones" + (conES ? " y sus E/S" : "") + ").\n"
                + "El sistema lo va a terminar apenas llegue, por deadline inalcanzable. ¿Crearlo igual?",
                "Deadline inalcanzable", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        return respuesta == JOptionPane.YES_OPTION;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            DatosProceso datos = DialogoProceso.pedir(null, 2, new String[]{"B1"}, true);
            System.out.println(datos == null ? "Cancelado" : datos);
        });
    }
}
