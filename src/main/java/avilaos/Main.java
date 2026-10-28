package avilaos;

import avilaos.gui.VentanaPrincipal;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {
   /* public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            usarAparienciaNimbus();
            new VentanaPrincipal().setVisible(true);
        });
    }

    private static void usarAparienciaNimbus() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    return;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException e) {
            // Usa la apariencia por defecto si falla
        }
    }*/
    
    public static void main(String[] args) {
javax.swing.SwingUtilities.invokeLater(() -> {
avilaos.so.DatosProceso datos =
avilaos.gui.DialogoProceso.pedir(null, 2, new String[] {"B1"}, true);
System.out.println(datos == null ? "Cancelado" : datos);
});
}
}