package avilaos;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame ventana = new JFrame("ÁvilaOS");
            ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            ventana.add(new JLabel("ÁvilaOS arrancó con Java " + System.getProperty("java.version"),
                    SwingConstants.CENTER));
            ventana.setSize(420, 160);
            ventana.setLocationRelativeTo(null);
            ventana.setVisible(true);
        });
    }
}