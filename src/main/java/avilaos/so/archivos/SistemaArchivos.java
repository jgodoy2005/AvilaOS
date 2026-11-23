package avilaos.so.archivos;

import avilaos.Limites;
import avilaos.estructuras.ListaEnlazada;
import avilaos.hardware.Bloque;
import avilaos.hardware.Disco;
import avilaos.simulacion.FotoArchivo;
import avilaos.simulacion.FotoNodo;
import avilaos.so.ModoUsuario;
import avilaos.so.RegistroEventos;
import java.util.concurrent.Semaphore;

/**
 * Sistema de archivos jerárquico sobre el disco, con asignación encadenada: cada archivo
 * guarda su primer bloque y su cantidad, y cada bloque del disco guarda cuál es el siguiente.
 *
 * Este paso arma el árbol, su carga inicial y sus fotos. Los permisos llegan en Archivos 3
 * y las operaciones en Archivos 4. El árbol se protege con un semáforo.
 */
public class SistemaArchivos {

    public static final String CARGA_INICIAL = "Carga inicial";

    private final Disco disco;
    private final int usuarios;
    private final RegistroEventos registro;
    private final NodoFS raiz = new NodoFS("", TipoNodo.DIRECTORIO, ModoUsuario.CUENTA_ADMIN, Permiso.PUBLICO, CARGA_INICIAL);
    private final Semaphore mutex = new Semaphore(1, true);
    private int proximoColor;

    public SistemaArchivos(Disco disco, int usuarios, RegistroEventos registro) {
        this.disco = disco;
        this.usuarios = usuarios;
        this.registro = registro;
    }

    //USUARIOS

    //"admin" o user1..userN
    public boolean usuarioValido(String nombre) {
        if (ModoUsuario.CUENTA_ADMIN.equals(nombre)) {
            return true;
        }
        for (int i = 1; i <= usuarios; i++) {
            if (("user" + i).equals(nombre)) {
                return true;
            }
        }
        return false;
    }

    //los usuarios normales, para el selector de la interfaz
    public String[] nombresUsuarios() {
        String[] r = new String[usuarios];
        for (int i = 0; i < usuarios; i++) {
            r[i] = "user" + (i + 1);
        }
        return r;
    }

    //CARGA INICIAL (JSON del sistema de archivos)

    //crea el arbol y ocupa los bloques; junta todos los errores en un solo mensaje
    public void cargar(EntradaFS[] entradas) {
        StringBuilder errores = new StringBuilder();
        mutex.acquireUninterruptibly();
        try {
            for (EntradaFS e : entradas) {
                String problema = cargarEntrada(e);
                if (problema != null) {
                    errores.append("- ").append(e.ruta()).append(": ").append(problema).append("\n");
                }
            }
        } finally {
            mutex.release();
        }
        if (errores.length() > 0) {
            throw new IllegalArgumentException("El JSON del sistema de archivos tiene errores:\n" + errores.toString().trim());
        }
    }

    private String cargarEntrada(EntradaFS e) {
        String ruta = e.ruta();
        if (ruta == null || !ruta.startsWith("/") || ruta.equals("/") || ruta.endsWith("/") || ruta.contains("//")) {
            return "la ruta debe empezar con / y no terminar en /";
        }
        int corte = ruta.lastIndexOf('/');
        String nombre = ruta.substring(corte + 1);
        NodoFS padre = buscarSinCandado(corte == 0 ? "/" : ruta.substring(0, corte), true);
        if (padre == null || !padre.esDirectorio()) {
            return "el directorio que lo contiene no existe (debe aparecer antes en el JSON)";
        }
        if (padre.hijo(nombre, true) != null) {
            return "está repetido";
        }
        if (!usuarioValido(e.dueno())) {
            return "el dueño '" + e.dueno() + "' no existe (usuarios: admin y user1 a user" + usuarios + ")";
        }
        NodoFS nodo = new NodoFS(nombre, e.tipo(), e.dueno(), e.permiso(), CARGA_INICIAL);
        if (e.tipo() == TipoNodo.ARCHIVO) {
            if (e.bloques() < Limites.TAMANO_ARCHIVO_MIN) {
                return "un archivo ocupa al menos un bloque";
            }
            if (e.bloques() > disco.getBloquesArchivos()) {
                return "pide " + bloques(e.bloques()) + " y la zona de archivos tiene " + disco.getBloquesArchivos();
            }
            int[] cadena = e.cadena() != null ? e.cadena() : consecutivos(e.primerBloque(), e.bloques());
            String problema = revisarCadena(e, cadena);
            if (problema != null) {
                return problema;
            }
            nodo.ubicar(cadena[0], cadena.length);
            nodo.setColor(proximoColor++);
            ocuparCadena(cadena, ruta, nodo.getColor());
        }
        padre.agregarHijo(nodo);
        return null;
    }

    //sin "chain" en el JSON, los bloques del archivo se toman seguidos desde el primero
    private static int[] consecutivos(int primero, int cantidad) {
        int[] r = new int[Math.max(0, cantidad)];
        for (int i = 0; i < r.length; i++) {
            r[i] = primero + i;
        }
        return r;
    }

    private String revisarCadena(EntradaFS e, int[] cadena) {
        if (cadena.length != e.bloques()) {
            return "la cadena tiene " + cadena.length + " bloques y 'blocks' dice " + e.bloques();
        }
        if (cadena[0] != e.primerBloque()) {
            return "la cadena empieza en " + cadena[0] + " y 'startBlock' dice " + e.primerBloque();
        }
        for (int i = 0; i < cadena.length; i++) {
            int b = cadena[i];
            if (b < 0 || b >= disco.getBloquesArchivos()) {
                return "el bloque " + b + " está fuera de la zona de archivos (0 a " + (disco.getBloquesArchivos() - 1) + ")";
            }
            if (!disco.estaLibre(b)) {
                return "el bloque " + b + " ya lo usa otro archivo";
            }
            for (int j = 0; j < i; j++) {
                if (cadena[j] == b) {
                    return "el bloque " + b + " se repite en la cadena";
                }
            }
        }
        return null;
    }

    //marca los bloques en el disco: cada uno apunta al siguiente y el ultimo a FIN
    private void ocuparCadena(int[] cadena, String ocupante, int color) {
        for (int i = 0; i < cadena.length; i++) {
            disco.ocupar(cadena[i], ocupante, color, i + 1 < cadena.length ? cadena[i + 1] : Bloque.FIN);
        }
    }

    //BUSQUEDA

    public NodoFS buscar(String ruta) {
        mutex.acquireUninterruptibly();
        try {
            return buscarSinCandado(ruta, false);
        } finally {
            mutex.release();
        }
    }

    private NodoFS buscarSinCandado(String ruta, boolean incluirPendientes) {
        if (ruta == null || !ruta.startsWith("/")) {
            return null;
        }
        NodoFS actual = raiz;
        for (String parte : ruta.substring(1).split("/")) {
            if (parte.isEmpty()) {
                continue;
            }
            actual = actual.esDirectorio() ? actual.hijo(parte, incluirPendientes) : null;
            if (actual == null) {
                return null;
            }
        }
        return actual;
    }

    //recorre la cadena de un archivo en el mapa del disco
    private int[] cadenaDe(NodoFS archivo) {
        int[] r = new int[archivo.getCantidadBloques()];
        int b = archivo.getPrimerBloque();
        for (int i = 0; i < r.length && b != Bloque.FIN; i++) {
            r[i] = b;
            b = disco.siguienteDe(b);
        }
        return r;
    }

    //FOTOS Y GUARDADO

    public FotoNodo fotografiar() {
        mutex.acquireUninterruptibly();
        try {
            return foto(raiz);
        } finally {
            mutex.release();
        }
    }

    private static FotoNodo foto(NodoFS n) {
        ListaEnlazada<FotoNodo> hijos = new ListaEnlazada<>();
        for (NodoFS h : n.getHijos()) {
            if (!h.isPendiente()) {
                hijos.agregarAlFinal(foto(h));
            }
        }
        return new FotoNodo(n.getPadre() == null ? "/" : n.getNombre(), n.ruta(), n.getTipo(),
                n.getDueno(), n.getPermiso(), n.getPrimerBloque(), n.getCantidadBloques(), n.getCreador(),
                n.getColor(), hijos.aArreglo(new FotoNodo[hijos.tamano()]));
    }

    //tabla de asignacion: los archivos terminados, en el orden del arbol
    public FotoArchivo[] tablaAsignacion() {
        mutex.acquireUninterruptibly();
        try {
            ListaEnlazada<NodoFS> archivos = new ListaEnlazada<>();
            juntarArchivosVisibles(raiz, archivos);
            FotoArchivo[] r = new FotoArchivo[archivos.tamano()];
            int[] i = {0};
            archivos.paraCada(a -> r[i[0]++] = new FotoArchivo(a.getNombre(), a.ruta(), a.getCantidadBloques(),
                    a.getPrimerBloque(), a.getDueno(), a.getCreador(), a.getColor()));
            return r;
        } finally {
            mutex.release();
        }
    }

    private static void juntarArchivosVisibles(NodoFS nodo, ListaEnlazada<NodoFS> destino) {
        for (NodoFS h : nodo.getHijos()) {
            if (h.isPendiente()) {
                continue;
            }
            if (h.esDirectorio()) {
                juntarArchivosVisibles(h, destino);
            } else {
                destino.agregarAlFinal(h);
            }
        }
    }

    //todo el arbol (sin la raiz ni lo pendiente), padres antes que hijos, para guardar el JSON
    public EntradaFS[] exportar() {
        mutex.acquireUninterruptibly();
        try {
            ListaEnlazada<EntradaFS> r = new ListaEnlazada<>();
            exportar(raiz, r);
            return r.aArreglo(new EntradaFS[r.tamano()]);
        } finally {
            mutex.release();
        }
    }

    private void exportar(NodoFS nodo, ListaEnlazada<EntradaFS> destino) {
        for (NodoFS h : nodo.getHijos()) {
            if (h.isPendiente()) {
                continue;
            }
            int[] cadena = h.esDirectorio() ? null : cadenaDe(h);
            destino.agregarAlFinal(new EntradaFS(h.ruta(), h.getTipo(), h.getPrimerBloque(), h.getCantidadBloques(),
                    h.getDueno(), h.getPermiso(), cadena));
            if (h.esDirectorio()) {
                exportar(h, destino);
            }
        }
    }

    public int getUsuarios() {
        return usuarios;
    }

    //"1 bloque" o "n bloques", para los mensajes
    public static String bloques(int n) {
        return n == 1 ? "1 bloque" : n + " bloques";
    }
}