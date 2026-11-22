/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.hardware;

/**
 *
 * @author juandiegogodoy
 * Respuesta del planificador de disco cuando el disco queda libre: atender una solicitud,
 * moverse sin atender a nadie (SCAN y C-SCAN van hasta el extremo), saltar al bloque 0
 * (C-SCAN) o esperar.
 */
public final class DecisionDisco {
     public enum Tipo { ATENDER, MOVER, SALTAR_AL_INICIO, ESPERAR }

    private static final DecisionDisco ESPERAR = new DecisionDisco(Tipo.ESPERAR, null, -1, null);

    private final Tipo tipo;
    private final TrabajoDisco trabajo;
    private final int destino;
    private final DireccionCabezal direccion;

    private DecisionDisco(Tipo tipo, TrabajoDisco trabajo, int destino, DireccionCabezal direccion) {
        this.tipo = tipo;
        this.trabajo = trabajo;
        this.destino = destino;
        this.direccion = direccion;
    }

    //atender una solicitud; la direccion es la del barrido despues de elegirla
    public static DecisionDisco atender(TrabajoDisco trabajo, DireccionCabezal direccion) {
        return new DecisionDisco(Tipo.ATENDER, trabajo, -1, direccion);
    }

    //moverse hacia una posicion sin atender a nadie (por ejemplo, hasta el ultimo bloque en SCAN)
    public static DecisionDisco mover(int destino, DireccionCabezal direccion) {
        return new DecisionDisco(Tipo.MOVER, null, destino, direccion);
    }

    //saltar del extremo al bloque 0 (C-SCAN): cuesta un ciclo y no suma desplazamiento
    public static DecisionDisco saltarAlInicio() {
        return new DecisionDisco(Tipo.SALTAR_AL_INICIO, null, 0, DireccionCabezal.ARRIBA);
    }

    //no hay nada que hacer
    public static DecisionDisco esperar() {
        return ESPERAR;
    }

    public Tipo getTipo() { return tipo; }
    public TrabajoDisco getTrabajo() { return trabajo; }
    public int getDestino() { return destino; }
    public DireccionCabezal getDireccion() { return direccion; }
}
