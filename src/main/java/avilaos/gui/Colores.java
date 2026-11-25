/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.gui;

import java.awt.Color;
/**
 *
 * @author erick
 * Colores del mapa del disco. Cada archivo tiene un número de color (lo asigna el sistema de
 * archivos) y aquí se traduce a un color: el mismo en el mapa y en la tabla de asignación.
 * 
 */
final class Colores {

    private static final Color[] ARCHIVOS = {
        new Color(141, 211, 199), new Color(251, 128, 114), new Color(128, 177, 211), new Color(253, 180, 98),
        new Color(179, 222, 105), new Color(252, 205, 229), new Color(255, 237, 111), new Color(204, 235, 197),
        new Color(166, 206, 227), new Color(247, 129, 191), new Color(191, 129, 45), new Color(255, 255, 179)
    };

    static final Color LIBRE = Color.WHITE;
    static final Color ZONA_SWAP = new Color(235, 235, 245);   //bloque libre del swap
    static final Color SWAP = new Color(110, 110, 160);        //bloque del swap ocupado por un suspendido
    static final Color BORDE = new Color(170, 170, 180);
    static final Color CABEZAL = new Color(200, 30, 30);

    private Colores() {
    }

    static Color archivo(int indice) {
        return indice < 0 ? Color.LIGHT_GRAY : ARCHIVOS[indice % ARCHIVOS.length];
    }
}

