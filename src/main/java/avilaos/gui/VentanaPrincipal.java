package avilaos.gui;

import java.awt.BorderLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

public class VentanaPrincipal extends JFrame {
    private final JTabbedPane pestanas = new JTabbedPane();

    public VentanaPrincipal() {
        super("ÁvilaOS - Simulador de un sistema operativo distribuido");
        pestanas.addTab("Configuración", aviso("Aquí irá la configuración."));
        pestanas.addTab("Simulación", aviso("Aquí irá la Vista 2."));
        pestanas.addTab("Métricas", aviso("Aquí irán las métricas y la gráfica."));

        add(pestanas, BorderLayout.CENTER);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 860);
        setLocationRelativeTo(null);
    }

    private static JPanel aviso(String texto) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel(texto, SwingConstants.CENTER), BorderLayout.CENTER);
        return panel;
    }
}