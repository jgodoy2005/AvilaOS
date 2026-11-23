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
 * C-SCAN: barre siempre hacia los bloques mayores; al no quedar solicitudes adelante llega
 * al extremo y salta al bloque 0 (el salto no cuenta como desplazamiento).
 */
public class PoliticaCSCAN implements PoliticaDisco {

    public TipoPoliticaDisco getTipo() {
        return TipoPoliticaDisco.C_SCAN;
    }

    public DecisionDisco decidir(Solicitud[] pendientes, int cabezal, DireccionCabezal direccion, int ultimoBloque) {
        Solicitud adelante = PoliticaSCAN.masCercanaEn(DireccionCabezal.ARRIBA, pendientes, cabezal);
        if (adelante != null) {
            return DecisionDisco.atender(adelante, DireccionCabezal.ARRIBA);
        }
        if (cabezal < ultimoBloque) {
            return DecisionDisco.mover(ultimoBloque, DireccionCabezal.ARRIBA);
        }
        return DecisionDisco.saltarAlInicio();
    }
}
