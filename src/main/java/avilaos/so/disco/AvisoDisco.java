/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Record.java to edit this template
 */
package avilaos.so.disco;

/**
 *
 * @author juandiegogodoy
 * Aviso que el disco le deja al kernel cuando termina una solicitud o cuando la saca de la
 * cola porque se pidió cancelarla. El kernel lo atiende en el ciclo siguiente.
 */
public record AvisoDisco(Solicitud solicitud, boolean cancelada) {
}
