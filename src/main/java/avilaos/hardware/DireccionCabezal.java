/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Enum.java to edit this template
 */
package avilaos.hardware;

/**
 *
 * @author juandiegogodoy
 * Dirección en la que se mueve (o barre) el cabezal del disco.
 */
public enum DireccionCabezal {
    ARRIBA("↑ hacia los bloques mayores", 1),
    ABAJO("↓ hacia los bloques menores", -1);

    private final String descripcion;
    private final int paso;

    DireccionCabezal(String descripcion, int paso) {
        this.descripcion = descripcion;
        this.paso = paso;
    }

    //+1 si sube, -1 si baja
    public int getPaso() {
        return paso;
    }

    public DireccionCabezal contraria() {
        return this == ARRIBA ? ABAJO : ARRIBA;
    }

    //direccion para ir de una posicion a otra (si son iguales, se mantiene la actual)
    public static DireccionCabezal hacia(int desde, int hasta, DireccionCabezal actual) {
        if (hasta > desde) {
            return ARRIBA;
        }
        if (hasta < desde) {
            return ABAJO;
        }
        return actual;
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
