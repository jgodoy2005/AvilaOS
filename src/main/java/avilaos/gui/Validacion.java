package avilaos.gui;

import java.awt.Color;
import java.awt.Component;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.UIManager;

public class Validacion {
    private static final Color COLOR_ERROR = new Color(255, 220, 220);
    private final StringBuilder errores = new StringBuilder();

    public int entero(JTextField campo, String nombre, int min, int max) {
        String texto = campo.getText().trim();
        try {
            int valor = Integer.parseInt(texto);
            if (valor < min || valor > max) {
                anotar(campo, nombre + " debe estar entre " + min + " y " + max + ".");
                return min;
            }
            limpiar(campo);
            return valor;
        } catch (NumberFormatException e) {
            anotar(campo, nombre + " debe ser un número entero" + (texto.isEmpty() ? "." : " (se escribió \"" + texto + "\")."));
            return min;
        }
    }

    public String texto(JTextField campo, String nombre, int maxLargo) {
        String texto = campo.getText().trim();
        if (texto.isEmpty()) {
            anotar(campo, nombre + " no puede estar vacío.");
        } else if (texto.length() > maxLargo) {
            anotar(campo, nombre + " admite hasta " + maxLargo + " caracteres.");
        } else {
            limpiar(campo);
        }
        return texto;
    }

    public void error(String mensaje) {
        errores.append("- ").append(mensaje).append('\n');
    }

    public boolean hayErrores() {
        return errores.length() > 0;
    }

    public void mostrar(Component padre) {
        JOptionPane.showMessageDialog(padre, "Revise estos datos:\n\n" + errores, "Datos inválidos", JOptionPane.WARNING_MESSAGE);
    }

    private void anotar(JTextField campo, String mensaje) {
        errores.append("- ").append(mensaje).append('\n');
        campo.setBackground(COLOR_ERROR);
    }

    private static void limpiar(JTextField campo) {
        campo.setBackground(UIManager.getColor("TextField.background"));
    }
}