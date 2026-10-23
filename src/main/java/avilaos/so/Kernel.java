/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.so;

/**
 *
 * @author juandiegogodoy
 */

/*
ESQUELETO PARA KERNEL, no definitivo
ahora mismo unicamente se declara el identificador del cpu 
y avisar cuando un proceso se desbloquea para volver a listos
*/

public class Kernel {
    private final int numero;
    public Kernel(int numero) { this.numero = numero; }
    public int getNumero() { return numero; }
    public void notificarDesbloqueo(PCB proceso) { }
}
