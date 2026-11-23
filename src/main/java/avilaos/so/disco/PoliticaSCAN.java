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
 * SCAN (algoritmo del ascensor): el cabezal barre en una dirección atendiendo lo que
 * encuentra; al no quedar solicitudes adelante llega hasta el extremo del disco y regresa.
 */
public class PoliticaSCAN implements PoliticaDisco {

    public TipoPoliticaDisco getTipo() {
        return TipoPoliticaDisco.SCAN;
    }

    public DecisionDisco decidir(Solicitud[] pendientes, int cabezal, DireccionCabezal direccion, int ultimoBloque) {
        Solicitud adelante = masCercanaEn(direccion, pendientes, cabezal);
        if (adelante != null) {
            return DecisionDisco.atender(adelante, direccion);
        }
        //nada adelante: llegar hasta el extremo antes de dar la vuelta
        if (direccion == DireccionCabezal.ARRIBA && cabezal < ultimoBloque) {
            return DecisionDisco.mover(ultimoBloque, DireccionCabezal.ARRIBA);
        }
        if (direccion == DireccionCabezal.ABAJO && cabezal > 0) {
            return DecisionDisco.mover(0, DireccionCabezal.ABAJO);
        }
        //ya esta en el extremo: invierte la direccion y sigue barriendo
        DireccionCabezal vuelta = direccion.contraria();
        return DecisionDisco.atender(masCercanaEn(vuelta, pendientes, cabezal), vuelta);
    }

    //la solicitud mas cercana en esa direccion (incluida la posicion actual); empate: la que llego primero
    static Solicitud masCercanaEn(DireccionCabezal direccion, Solicitud[] pendientes, int cabezal) {
        Solicitud mejor = null;
        int menor = Integer.MAX_VALUE;
        for (Solicitud s : pendientes) {
            int distancia = (PoliticaDisco.posicion(s, cabezal) - cabezal) * direccion.getPaso();
            if (distancia >= 0 && distancia < menor) {
                menor = distancia;
                mejor = s;
            }
        }
        return mejor;
    }
}
