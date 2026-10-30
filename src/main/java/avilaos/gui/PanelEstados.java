package avilaos.gui;

import avilaos.estructuras.Accion;
import avilaos.hardware.ModoCPU;
import avilaos.simulacion.FotoComputador;
import avilaos.simulacion.FotoPCB;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;

/**
 * Mapa de estados de un computador: cinco columnas (Nuevos, En CPU, Listos, Bloqueados y
 * Terminados) con una ficha por proceso. Se redibuja con cada foto, así que un cambio de
 * estado se ve como una ficha que salta de columna. Pasando el mouse se ve el detalle; con
 * doble clic, el PCB completo.
 */
public class PanelEstados extends JComponent {
    private static final int FICHAS_POR_COLUMNA = 3;
    private static final int ALTO_FICHA = 22;
    private static final int SEPARACION = 4;
    private static final String[] TITULOS = {"Nuevos", "En CPU", "Listos", "Bloqueados", "Terminados"};

    private static final Color NUEVO = new Color(222, 228, 238);
    private static final Color USUARIO = new Color(200, 240, 200);
    private static final Color SISTEMA = new Color(255, 225, 180);
    private static final Color LISTO = new Color(210, 225, 250);
    private static final Color BLOQUEADO = new Color(255, 214, 208);
    private static final Color COMPLETADO = new Color(214, 238, 214);
    private static final Color VENCIDO = new Color(232, 200, 200);
    private static final Color BORDE = new Color(150, 160, 175);

    private final Rectangle[] zonas = new Rectangle[TITULOS.length * FICHAS_POR_COLUMNA];
    private final FotoPCB[] fichas = new FotoPCB[zonas.length];
    private int dibujadas;
    private FotoComputador foto;

    public PanelEstados(Accion<FotoPCB> alHacerDobleClic) {
        ToolTipManager.sharedInstance().registerComponent(this);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                FotoPCB p = procesoEn(e.getPoint());
                if (e.getClickCount() == 2 && p != null) {
                    alHacerDobleClic.ejecutar(p);
                }
            }
        });
    }

    @Override
    public Dimension getPreferredSize() {
        Insets borde = getInsets();
        return new Dimension(520 + borde.left + borde.right,
            22 + FICHAS_POR_COLUMNA * (ALTO_FICHA + SEPARACION) + 14 + borde.top + borde.bottom);
    }

    public void mostrar(FotoComputador f) {
        foto = f;
        repaint();
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        FotoPCB p = procesoEn(e.getPoint());
        if (p == null) {
            return null;
        }
        return p.etiqueta() + " " + p.nombre() + (p.motivo().isEmpty() ? "" : " " + p.motivo())
                + " (doble clic: PCB completo)";
    }

    private FotoPCB procesoEn(Point punto) {
        for (int i = 0; i < dibujadas; i++) {
            if (zonas[i].contains(punto)) {
                return fichas[i];
            }
        }
        return null;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        dibujadas = 0;
        if (foto != null) {
            FotoPCB[] enCpu = foto.enEjecucion() == null ? new FotoPCB[0] : new FotoPCB[] {foto.enEjecucion()};
            FotoPCB[][] columnas = {foto.nuevos(), enCpu, foto.listos(), foto.bloqueados(), masRecientes(foto.terminados())};
            int[] totales = {foto.nuevos().length, enCpu.length, foto.listos().length, foto.bloqueados().length, foto.terminados().length};

            Insets borde = getInsets();
            g2.translate(borde.left, borde.top);
            int ancho = (getWidth() - borde.left - borde.right) / TITULOS.length;

            for (int c = 0; c < TITULOS.length; c++) {
                dibujarColumna(g2, c, c * ancho, ancho, columnas[c], totales[c], borde);
            }
        }
        g2.dispose();
    }

    private void dibujarColumna(Graphics2D g2, int columna, int x, int ancho, FotoPCB[] lista, int total, Insets borde) {
        Font base = UIManager.getFont("Label.font");
        Color texto = UIManager.getColor("Label.foreground");

        g2.setFont(base.deriveFont(Font.BOLD, 11.5f));
        g2.setColor(texto);
        g2.drawString(TITULOS[columna] + " (" + total + ")", x + 6, 14);

        if (columna < TITULOS.length - 1) {
            int fx = x + ancho - 8;
            g2.setColor(BORDE);
            g2.drawLine(fx - 10, 10, fx, 10);
            g2.fillPolygon(new int[] {fx, fx - 5, fx - 5}, new int[] {10, 6, 14}, 3);
        }

        g2.setFont(base.deriveFont(11f));
        FontMetrics medidas = g2.getFontMetrics();
        int cuantas = Math.min(lista.length, FICHAS_POR_COLUMNA);

        for (int i = 0; i < cuantas; i++) {
            FotoPCB p = lista[i];
            Rectangle r = new Rectangle(x + 4, 22 + i * (ALTO_FICHA + SEPARACION), ancho - 14, ALTO_FICHA);
            g2.setColor(color(columna, p));
            g2.fillRoundRect(r.x, r.y, r.width, r.height, 10, 10);
            g2.setColor(BORDE);
            g2.drawRoundRect(r.x, r.y, r.width, r.height, 10, 10);
            g2.setColor(texto);
            g2.drawString(recortar(textoFicha(columna, p), medidas, r.width - 10), r.x + 6, r.y + 15);

            zonas[dibujadas] = new Rectangle(r.x + borde.left, r.y + borde.top, r.width, r.height);
            fichas[dibujadas] = p;
            dibujadas++;
        }

        if (total > cuantas) {
            g2.setColor(UIManager.getColor("Label.disabledForeground"));
            g2.drawString("+" + (total - cuantas) + " más", x + 8, 22 + cuantas * (ALTO_FICHA + SEPARACION) + 10);
        }
    }

    private static FotoPCB[] masRecientes(FotoPCB[] terminados) {
        int n = Math.min(terminados.length, FICHAS_POR_COLUMNA);
        FotoPCB[] ultimos = new FotoPCB[n];
        for (int i = 0; i < n; i++) {
            ultimos[i] = terminados[terminados.length - 1 - i];
        }
        return ultimos;
    }

    private Color color(int columna, FotoPCB p) {
        return switch (columna) {
            case 0 -> NUEVO;
            case 1 -> foto.modo() == ModoCPU.USUARIO ? USUARIO : SISTEMA;
            case 2 -> LISTO;
            case 3 -> BLOQUEADO;
            default -> p.motivo().equals("Completado") ? COMPLETADO : VENCIDO;
        };
    }

    private static String textoFicha(int columna, FotoPCB p) {
        if (columna == 3 && p.motivoBloqueo() != null) {
            String donde = p.buffer().isEmpty() ? "" : " " + p.buffer();
            return p.etiqueta() + " " + switch (p.motivoBloqueo()) {
                case ES -> "E/S";
                case RED -> "red" + donde;
                case REGION_CRITICA -> "s" + donde;
                case BUFFER_VACIO -> "n" + donde;
                case BUFFER_LLENO -> "e" + donde;
            };
        }
        if (columna == 4 && !p.motivo().equals("Completado")) {
            return p.etiqueta() + " venció";
        }
        return p.etiqueta();
    }

    private static String recortar(String texto, FontMetrics medidas, int ancho) {
        if (medidas.stringWidth(texto) <= ancho) {
            return texto;
        }
        String corto = texto;
        while (corto.length() > 1 && medidas.stringWidth(corto + "...") > ancho) {
            corto = corto.substring(0, corto.length() - 1);
        }
        return corto + "...";
    }
}