/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.so;

/**
 *
 * @author juandiegogodoy
 */
public final class GeneradorId {
    private static int siguiente = 1;

    private GeneradorId() {
    }

    public static int nuevoId() {
        return siguiente++;
    }

    /* inicializar valores de nuevo en cada simulacion*/
    public static void reiniciar() {
        siguiente = 1;
    }
}
