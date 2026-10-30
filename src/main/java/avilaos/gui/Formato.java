package avilaos.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

public final class Formato {
    private Formato() {}

    public static String decimal(double valor) {
        return Double.isNaN(valor) ? "-" : String.format("%.1f", valor);
    }

    public static String porcentaje(double valor) {
        return Double.isNaN(valor) ? "-" : String.format("%.1f %%", valor);
    }

    public static String tresDecimales(double valor) {
        return Double.isNaN(valor) ? "-" : String.format("%.3f", valor);
    }

    public static String lista(String[] elementos) {
        if (elementos.length == 0) return "-";
        StringBuilder texto = new StringBuilder();
        for (String e : elementos) {
            texto.append(texto.length() > 0 ? ", " : "").append(e);
        }
        return texto.toString();
    }

    public static JPanel formulario() {
        return new JPanel(new GridBagLayout());
    }

    public static void fila(JPanel formulario, int fila, String etiqueta, JComponent campo) {
        fila(formulario, fila, etiqueta, campo, new Insets(4, 6, 4, 6));
    }

    public static void filaCompacta(JPanel formulario, int fila, String etiqueta, JComponent campo) {
        fila(formulario, fila, etiqueta, campo, new Insets(1, 4, 1, 4));
    }

    private static void fila(JPanel formulario, int fila, String etiqueta, JComponent campo, Insets margen) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = fila;
        c.insets = margen;
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        formulario.add(new JLabel(etiqueta), c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        formulario.add(campo, c);
    }
}