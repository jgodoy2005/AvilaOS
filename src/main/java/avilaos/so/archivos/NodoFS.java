package avilaos.so.archivos;

import avilaos.estructuras.ListaEnlazada;

/**
 * Un elemento del sistema de archivos: un directorio (con hijos, sin bloques) o un archivo
 * (con su primer bloque y su cantidad de bloques; el resto de la cadena está en el disco).
 */
public class NodoFS {

    private String nombre;
    private final TipoNodo tipo;
    private NodoFS padre;
    private final ListaEnlazada<NodoFS> hijos = new ListaEnlazada<>();
    private final String dueno;
    private final Permiso permiso;
    private int primerBloque = -1;
    private int cantidadBloques;
    private final String creador;            //proceso que lo creo ("Carga inicial" si vino del JSON)
    private int color = -1;

    private boolean pendiente;               //su creacion todavia no termina: aun no se muestra
    private int operacionesPendientes;       //solicitudes en cola o en curso que lo usan
    private boolean eliminacionPendiente;    //tiene una solicitud de eliminar en cola o en curso

    public NodoFS(String nombre, TipoNodo tipo, String dueno, Permiso permiso, String creador) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.dueno = dueno;
        this.permiso = permiso;
        this.creador = creador;
    }

    //ARBOL

    void agregarHijo(NodoFS hijo) {
        hijo.padre = this;
        hijos.agregarAlFinal(hijo);
    }

    void quitarHijo(NodoFS hijo) {
        hijos.eliminar(hijo);
        hijo.padre = null;
    }

    //busca un hijo por nombre; con 'incluirPendientes' tambien ve los que se estan creando
    NodoFS hijo(String nombreHijo, boolean incluirPendientes) {
        return hijos.buscar(h -> h.nombre.equals(nombreHijo) && (incluirPendientes || !h.pendiente));
    }

    //ruta completa: "/" para la raiz, "/sistema/boot.bin" para los demas
    public String ruta() {
        if (padre == null) {
            return "/";
        }
        String rutaPadre = padre.ruta();
        return (rutaPadre.equals("/") ? "" : rutaPadre) + "/" + nombre;
    }

    //si este nodo esta dentro de 'otro' (o es el mismo)
    boolean estaDentroDe(NodoFS otro) {
        for (NodoFS n = this; n != null; n = n.padre) {
            if (n == otro) {
                return true;
            }
        }
        return false;
    }

    //si este nodo o algo debajo tiene operaciones pendientes
    boolean tienePendientesEnSuArbol() {
        if (operacionesPendientes > 0) {
            return true;
        }
        boolean[] hay = {false};
        hijos.paraCada(h -> hay[0] |= h.tienePendientesEnSuArbol());
        return hay[0];
    }

    //si este nodo o alguno de sus directorios padres tiene una eliminacion pendiente
    boolean tieneEliminacionPendienteArriba() {
        for (NodoFS n = this; n != null; n = n.padre) {
            if (n.eliminacionPendiente) {
                return true;
            }
        }
        return false;
    }

    public boolean esDirectorio() {
        return tipo == TipoNodo.DIRECTORIO;
    }

    public NodoFS[] getHijos() {
        return hijos.aArreglo(new NodoFS[hijos.tamano()]);
    }

    //CAMBIOS (solo los hace el sistema de archivos)

    void renombrar(String nuevo) { nombre = nuevo; }
    void ubicar(int primerBloque, int cantidadBloques) { this.primerBloque = primerBloque; this.cantidadBloques = cantidadBloques; }
    void setColor(int color) { this.color = color; }
    void setPendiente(boolean pendiente) { this.pendiente = pendiente; }
    void sumarPendiente(int delta) { operacionesPendientes += delta; }
    void setEliminacionPendiente(boolean valor) { eliminacionPendiente = valor; }

    public String getNombre() { return nombre; }
    public TipoNodo getTipo() { return tipo; }
    public NodoFS getPadre() { return padre; }
    public String getDueno() { return dueno; }
    public Permiso getPermiso() { return permiso; }
    public int getPrimerBloque() { return primerBloque; }
    public int getCantidadBloques() { return cantidadBloques; }
    public String getCreador() { return creador; }
    public int getColor() { return color; }
    public boolean isPendiente() { return pendiente; }
    public int getOperacionesPendientes() { return operacionesPendientes; }
    public boolean isEliminacionPendiente() { return eliminacionPendiente; }
}