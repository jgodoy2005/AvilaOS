package avilaos.gui;

import avilaos.estructuras.Cola;
import java.awt.BorderLayout;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;

/** Registro de eventos: cada decisión importante del sistema, con su ciclo. */
public class PanelLog extends JPanel {
    private static final int MAX_LINEAS = 3000;
    private final JTextArea area = new JTextArea();
    private final JCheckBox seguirFinal = new JCheckBox("Seguir el final", true);

    public PanelLog() {
        super(new BorderLayout());
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JButton limpiar = new JButton("Limpiar");
        limpiar.addActionListener(e -> area.setText(""));

        JPanel barra = new JPanel(new BorderLayout());
        barra.add(seguirFinal, BorderLayout.WEST);
        barra.add(limpiar, BorderLayout.EAST);

        JScrollPane scroll = new JScrollPane(area);
        setBorder(BorderFactory.createTitledBorder("Registro de eventos"));
        add(scroll, BorderLayout.CENTER);
        add(barra, BorderLayout.SOUTH);
    }

    /** Agrega las líneas nuevas (y se las lleva de la cola). */
    public void agregar(Cola<String> lineas) {
        if (lineas.estaVacia()) {
            return;
        }
        StringBuilder texto = new StringBuilder();
        while (!lineas.estaVacia()) {
            texto.append(lineas.desencolar()).append('\n');
        }
        area.append(texto.toString());
        recortar();
        if (seguirFinal.isSelected()) {
            area.setCaretPosition(area.getDocument().getLength());
        }
    }

    private void recortar() {
        int sobran = area.getLineCount() - MAX_LINEAS;
        if (sobran > 0) {
            try {
                area.getDocument().remove(0, area.getLineEndOffset(sobran - 1));
            } catch (BadLocationException e) {
                area.setText("");
            }
        }
    }
}