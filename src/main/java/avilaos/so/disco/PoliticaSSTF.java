/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.so.disco;

import avilaos.hardware.DecisionDisco;
import avilaos.hardware.DireccionCabezal;
/**
 *
 * @author erick
 * 
 * SSTF (Shortest Seek Time First): atiende la solicitud cuyo primer bloque está más cerca
 * del cabezal. Si hay empate, gana la que llegó primero.
 */
public class PoliticaSSTF implements PoliticaDisco {

    public TipoPoliticaDisco getTipo() {
        return TipoPoliticaDisco.SSTF;
    }

    public DecisionDisco decidir(Solicitud[] pendientes, int cabezal, DireccionCabezal direccion, int ultimoBloque) {
        Solicitud mejor = null;
        int menor = Integer.MAX_VALUE;
        for (Solicitud s : pendientes) {     //en orden de llegada: con '<' el empate lo gana la primera
            int distancia = Math.abs(PoliticaDisco.posicion(s, cabezal) - cabezal);
            if (distancia < menor) {
                menor = distancia;
                mejor = s;
            }
        }
        int destino = PoliticaDisco.posicion(mejor, cabezal);
        return DecisionDisco.atender(mejor, DireccionCabezal.hacia(cabezal, destino, direccion));
    }
}
