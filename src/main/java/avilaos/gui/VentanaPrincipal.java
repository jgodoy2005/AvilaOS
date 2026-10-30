package avilaos.gui;

import avilaos.config.ConfiguracionSimulacion;
import avilaos.simulacion.FotoSimulacion;
import avilaos.simulacion.ObservadorSimulacion;
import avilaos.simulacion.Simulacion;
import avilaos.so.planificacion.TipoPolitica;
import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;



/*
 * Ventana principal: pestañas de Configuración, Simulación (Vista 2) y
 * Métricas. Observa a la simulación: en cada ciclo recibe un aviso desde el
 * hilo del reloj y redibuja en el hilo de Swing. La lógica de ÁvilaOS no vive
 * aquí.
 */
public class VentanaPrincipal extends JFrame implements ObservadorSimulacion {

    private final JTabbedPane pestanas = new JTabbedPane();
    private PanelSimulacion panelSimulacion;
    private PanelMetricas panelMetricas;
    private Simulacion simulacion;

    public VentanaPrincipal() {
        super("AvilaOS");
        pestanas.addTab("Configuración", new PanelConfiguracion(this::iniciarSimulacion));
        pestanas.addTab("Simulación", aviso(
        "Configure e inicie una simulación en la pestaña Configuración."));
pestanas.addTab("Métricas", aviso(
        "Las métricas aparecen cuando hay una simulación."));
add(pestanas, BorderLayout.CENTER);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cerrar();
            }
        });

        setSize(1280, 860);
        setLocationRelativeTo(null);
    }

    /*
     * Crea una simulación nueva con la configuración dada (si había otra, la
     * detiene).
     */
    private void iniciarSimulacion(ConfiguracionSimulacion config) {
        if (simulacion != null) {
            int respuesta = JOptionPane.showConfirmDialog(this,
                    "Ya hay una simulación. ¿Detenerla y empezar una nueva?", "Reiniciar",
                    JOptionPane.YES_NO_OPTION);
            if (respuesta != JOptionPane.YES_OPTION) {
                return;
            }
            simulacion.setObservador(null);
            simulacion.detener();
            simulacion = null;
        }
        Simulacion nueva;
        try {
            nueva = new Simulacion(config, false);
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "No se pudo iniciar", JOptionPane.WARNING_MESSAGE);
        return;
        }
        TipoPolitica[] politicas = new TipoPolitica[config.computadores().length];
        for (int i = 0; i < politicas.length; i++) {
            politicas[i] = config.computadores()[i].politica();
        }
        panelSimulacion = new PanelSimulacion(nueva, politicas);
        panelMetricas = new PanelMetricas(politicas.length);
        pestanas.setComponentAt(1, panelSimulacion);
        pestanas.setComponentAt(2, panelMetricas);
        simulacion = nueva;
        simulacion.setObservador(this);
        simulacion.iniciar(); // crea los hilos; queda en pausa
        refrescar(); // muestra el ciclo 0
        pestanas.setSelectedIndex(1);
    }

    /*
     * Lo llama el hilo del reloj al final de cada ciclo. Swing no permite
     * dibujar desde otro hilo, así que le pide el dibujo al hilo de Swing con
     * invokeAndWait y espera a que termine. Por eso el reloj nunca adelanta a
     * la pantalla.
     */
    @Override
    public void estadoActualizado(FotoSimulacion foto) {
        try {

            SwingUtilities.invokeAndWait(this::refrescar);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // detener() interrumpe al reloj:se deja el aviso para que termine
        } catch (InvocationTargetException e) {
            e.getCause().printStackTrace(); // un error al dibujar no detiene la simulación
        }
    }

    /*
     * Hilo de Swing: dibuja la última foto de la simulación actual y los
     * eventos nuevos.
     */
    private void refrescar() {
        if (simulacion == null) {
            return;
        }
        FotoSimulacion foto = simulacion.getUltimaFoto();
        panelSimulacion.mostrar(foto);
        panelSimulacion.agregarEventos(simulacion.extraerEventos());
        panelMetricas.mostrar(foto);
    }

    private void cerrar() {
        if (simulacion != null) {
            simulacion.setObservador(null);
            simulacion.detener();
        }
        dispose();
        System.exit(0);
    }

    private static JPanel aviso(String texto) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel(texto, SwingConstants.CENTER),
                BorderLayout.CENTER);
        return panel;
    }
}
