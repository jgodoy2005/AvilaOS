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
 */
public class PoliticaFIFO implements PoliticaDisco {

    public TipoPoliticaDisco getTipo() {
        return TipoPoliticaDisco.FIFO;
    }

    @Override
    public DecisionDisco decidir(Solicitud[] pendientes, int cabezal, DireccionCabezal direccion, int ultimoBloque) {
        Solicitud primera = pendientes[0];
        int destino = PoliticaDisco.posicion(primera, cabezal);
        return DecisionDisco.atender(primera, DireccionCabezal.hacia(cabezal, destino, direccion));
    }
}
