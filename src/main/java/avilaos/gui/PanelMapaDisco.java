/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.gui;

import avilaos.hardware.DireccionCabezal;
import avilaos.simulacion.FotoDisco;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;
import javax.swing.ToolTipManager;
/**
 *
 * @author erick
 * 
 * * Mapa de bloques del disco: un cuadrito por bloque, en orden (de izquierda a derecha y de
 * arriba abajo). Blanco = libre; color = el archivo que lo ocupa; morado oscuro = un proceso
 * suspendido en el swap; los bloques libres del swap tienen un fondo gris claro. El cabezal se
 * marca con un borde rojo y la flecha del último movimiento observado.
 */
public class PanelMapaDisco extends JComponent {

    private static final int ALTO = 13;

    private FotoDisco disco;
    private DireccionCabezal ultimoMovimiento;
    private int primerResaltado = -1;   //primer bloque del archivo seleccionado (su cadena se marca)

    public PanelMapaDisco() {
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    public void mostrar(FotoDisco d) {
        boolean cambioTamano = disco == null || disco.totalBloques() != d.totalBloques();
        if (disco != null && d.cabezal() != disco.cabezal()) {
            ultimoMovimiento = d.cabezal() > disco.cabezal() ? DireccionCabezal.ARRIBA : DireccionCabezal.ABAJO;
        }
        disco = d;
        if (cambioTamano) {
            revalidate();
        }
        repaint();
    }

    DireccionCabezal ultimoMovimiento() {
        return ultimoMovimiento;
    }

    //marca la cadena de un archivo (-1: ninguno)
    public void resaltar(int primerBloque) {
        primerResaltado = primerBloque;
        repaint();
    }

    private int columnas() {
        int total = disco == null ? 300 : disco.totalBloques();
        return total <= 400 ? 25 : total <= 800 ? 40 : 50;
    }

    @Override
    public Dimension getPreferredSize() {
        Insets b = getInsets();
        int total = disco == null ? 300 : disco.totalBloques();
        int filas = (total + columnas() - 1) / columnas();
        return new Dimension(600 + b.left + b.right, filas * ALTO + b.top + b.bottom + 1);
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (disco == null) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Insets b = getInsets();
        int columnas = columnas();
        int ancho = Math.max(4, (getWidth() - b.left - b.right - 1) / columnas);
        boolean[] resaltado = cadenaResaltada();
        for (int i = 0; i < disco.totalBloques(); i++) {
            int x = b.left + (i % columnas) * ancho;
            int y = b.top + (i / columnas) * ALTO;
            g2.setColor(colorDe(i));
            g2.fillRect(x, y, ancho, ALTO);
            g2.setColor(Colores.BORDE);
            g2.drawRect(x, y, ancho, ALTO);
            if (resaltado[i]) {
                g2.setColor(Color.BLACK);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRect(x + 1, y + 1, ancho - 2, ALTO - 2);
                g2.setStroke(new BasicStroke(1f));
            }
        }
        //cabezal: borde rojo y una flecha hacia los bloques mayores (derecha) o menores (izquierda)
        int c = disco.cabezal();
        int x = b.left + (c % columnas) * ancho;
        int y = b.top + (c / columnas) * ALTO;
        g2.setColor(Colores.CABEZAL);
        g2.setStroke(new BasicStroke(2.5f));
        g2.drawRect(x, y, ancho, ALTO);
        int medio = y + ALTO / 2;
        if (ultimoMovimiento == DireccionCabezal.ARRIBA) {
            g2.fillPolygon(new int[] {x + 3, x + ancho - 3, x + 3}, new int[] {y + 3, medio, y + ALTO - 3}, 3);
        } else if (ultimoMovimiento == DireccionCabezal.ABAJO) {
            g2.fillPolygon(new int[] {x + ancho - 3, x + 3, x + ancho - 3}, new int[] {y + 3, medio, y + ALTO - 3}, 3);
        }
        g2.dispose();
    }

    private Color colorDe(int bloque) {
        boolean swap = bloque >= disco.bloquesArchivos();
        if (disco.ocupantes()[bloque] == null) {
            return swap ? Colores.ZONA_SWAP : Colores.LIBRE;
        }
        return swap ? Colores.SWAP : Colores.archivo(disco.colores()[bloque]);
    }

    //los bloques de la cadena del archivo seleccionado, siguiendo el "siguiente" de cada uno
    private boolean[] cadenaResaltada() {
        boolean[] r = new boolean[disco.totalBloques()];
        for (int b = primerResaltado, pasos = 0; b >= 0 && b < r.length && !r[b] && pasos < r.length; pasos++) {
            r[b] = true;
            b = disco.siguientes()[b];
        }
        return r;
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        if (disco == null) {
            return null;
        }
        Insets b = getInsets();
        int columnas = columnas();
        int ancho = Math.max(4, (getWidth() - b.left - b.right - 1) / columnas);
        int col = (e.getX() - b.left) / ancho;
        int fila = (e.getY() - b.top) / ALTO;
        int bloque = fila * columnas + col;
        if (col < 0 || col >= columnas || fila < 0 || bloque >= disco.totalBloques()) {
            return null;
        }
        String zona = bloque >= disco.bloquesArchivos() ? "swap" : "archivos";
        String quien = disco.ocupantes()[bloque];
        int siguiente = disco.siguientes()[bloque];
        return "Bloque " + bloque + " (zona de " + zona + "): "
                + (quien == null ? "libre" : quien + ", siguiente: " + (siguiente < 0 ? "fin de la cadena" : siguiente))
                + (bloque == disco.cabezal() ? " · aquí está el cabezal" : "");
    }
}
