package avilaos.gui;

import avilaos.simulacion.FotoBuffer;
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

/** Estado de cada buffer: contenido, valores de s, n y e, y quién espera en cada semáforo. */
public class PanelBuffers extends JPanel {
    private static final String[] COLUMNAS = {"Buffer", "Host", "Ocup.", "Contenido",
        "s", "n", "e", "Cola de s", "Cola de n", "Cola de e"};

    private final ModeloTabla<FotoBuffer> modelo = new ModeloTabla<>(COLUMNAS, PanelBuffers::valor);

    public PanelBuffers() {
        super(new BorderLayout());
        JTable tabla = new JTable(modelo);
        tabla.setFillsViewportHeight(true);
        tabla.setPreferredScrollableViewportSize(new Dimension(700, 5 * tabla.getRowHeight()));

        int[] anchos = {45, 50, 50, 220, 35, 35, 35, 90, 90, 90};
        for (int i = 0; i < anchos.length; i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }

        JScrollPane scroll = new JScrollPane(tabla);
        JLabel leyenda = new JLabel("s = mutex | n = llenos (elementos) | e = vacíos (espacios) | un valor negativo indica cuántos procesos esperan en su cola");

        setBorder(BorderFactory.createTitledBorder("Buffers y semáforos"));
        add(scroll, BorderLayout.CENTER);
        add(leyenda, BorderLayout.SOUTH);
    }

    public void mostrar(FotoBuffer[] buffers) {
        modelo.setFilas(buffers);
    }

    private static Object valor(FotoBuffer b, int columna) {
        return switch (columna) {
            case 0 -> b.nombre();
            case 1 -> "C" + b.anfitrion();
            case 2 -> b.cantidad() + "/" + b.capacidad();
            case 3 -> Formato.lista(b.contenido());
            case 4 -> b.valorS();
            case 5 -> b.valorN();
            case 6 -> b.valorE();
            case 7 -> Formato.lista(b.esperandoS());
            case 8 -> Formato.lista(b.esperandoN());
            default -> Formato.lista(b.esperandoE());
        };
    }
}