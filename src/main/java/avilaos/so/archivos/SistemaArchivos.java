package avilaos.so.archivos;

import avilaos.Limites;
import avilaos.estructuras.ListaEnlazada;
import avilaos.hardware.Bloque;
import avilaos.hardware.Disco;
import avilaos.simulacion.FotoArchivo;
import avilaos.simulacion.FotoNodo;
import avilaos.so.ModoUsuario;
import avilaos.so.OperacionArchivo;
import avilaos.so.PCB;
import avilaos.so.RegistroEventos;
import avilaos.so.TipoOperacionES;
import avilaos.so.disco.Solicitud;

import java.util.concurrent.Semaphore;

/**
 * Sistema de archivos jerárquico sobre el disco, con asignación encadenada:
 * cada archivo guarda su primer bloque y su cantidad, y cada bloque del disco
 * guarda cuál es el siguiente.
 *
 * Las operaciones tienen dos mitades. Al ejecutarse la instrucción del proceso
 * (preparar) se revisa todo, se reservan los bloques y se arma la solicitud;
 * cuando el disco termina (completar) se aplica el cambio en el árbol. Todo
 * ocurre en el hilo del kernel y el árbol se protege con un semáforo.
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
        String problemaNombre = revisarNombre(nombre, padre, null);
        if (problemaNombre != null) {
            return problemaNombre;
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

//PERMISOS
    /**
     * Para la interfaz en modo usuario: revisa el permiso antes de crear el
     * proceso. Devuelve null si puede (o si la ruta ya no existe: eso lo
     * resuelve el proceso al ejecutarse) o la razón. Para crear, la ruta es la
     * del directorio donde se crea.
     */
    public String puede(String usuario, TipoOperacionES operacion, String ruta) {
        mutex.acquireUninterruptibly();
        try {
            NodoFS nodo = buscarSinCandado(ruta, false);
            return nodo == null ? null : verificar(usuario, operacion, nodo);
        } finally {
            mutex.release();
        }
    }

    /**
     * Revisa si un usuario puede hacer una operación. Devuelve null si puede, o
     * la razón del rechazo. Para crear, 'nodo' es el directorio donde se crea.
     * Se llama con el árbol tomado.
     */
    private String verificar(String usuario, TipoOperacionES operacion, NodoFS nodo) {
        if (ModoUsuario.CUENTA_ADMIN.equals(usuario)) {
            return null;                                         //el administrador puede todo
        }
        boolean suyo = usuario.equals(nodo.getDueno());
        switch (operacion) {
            case CREAR:
                return suyo ? null
                        : "solo puede crear dentro de sus propios directorios y " + nodo.ruta() + " no es suyo";
            case LEER:
                if (suyo) {
                    return null;
                }
                NodoFS privado = directorioPrivadoAjeno(usuario, nodo.getPadre());
                if (privado != null) {
                    return "el directorio " + privado.ruta() + " es privado de " + privado.getDueno();
                }
                return nodo.getPermiso() == Permiso.PUBLICO ? null
                        : "el archivo es privado de " + nodo.getDueno();
            case ACTUALIZAR:
                return suyo ? null : "solo su dueño (" + nodo.getDueno() + ") o el administrador pueden renombrarlo";
            case ELIMINAR:
                if (!suyo) {
                    return "solo su dueño (" + nodo.getDueno() + ") o el administrador pueden eliminarlo";
                }
                NodoFS ajeno = primerElementoAjeno(usuario, nodo);
                return ajeno == null ? null
                        : "no puede eliminar " + ajeno.ruta() + ": pertenece a " + ajeno.getDueno();
            default:
                return "operación no permitida";
        }
    }

    //El borrado recursivo exige autorización sobre todos los elementos que desaparecerían.
    private static NodoFS primerElementoAjeno(String usuario, NodoFS nodo) {
        if (!usuario.equals(nodo.getDueno())) {
            return nodo;
        }
        for (NodoFS hijo : nodo.getHijos()) {
            NodoFS ajeno = primerElementoAjeno(usuario, hijo);
            if (ajeno != null) {
                return ajeno;
            }
        }
        return null;
    }

    //el primer directorio privado de otro usuario en el camino hasta la raiz (null si puede entrar)
    private static NodoFS directorioPrivadoAjeno(String usuario, NodoFS directorio) {
        for (NodoFS d = directorio; d != null; d = d.getPadre()) {
            if (d.getPermiso() == Permiso.PRIVADO && !usuario.equals(d.getDueno())) {
                return d;
            }
        }
        return null;
    }

    //OPERACIONES (hilo del kernel)
    /**
     * Primera mitad de una operación, cuando el proceso ejecuta su instrucción:
     * revisa que se pueda hacer, reserva lo necesario y arma la solicitud para
     * el disco.
     */
    public Solicitud preparar(PCB proceso, OperacionArchivo op) throws OperacionRechazada {
        mutex.acquireUninterruptibly();
        try {
            String usuario = op.usuario() == null ? ModoUsuario.CUENTA_ADMIN : op.usuario();
            if (op.operacion() == TipoOperacionES.CREAR) {
                return prepararCreacion(proceso, op, usuario);
            }
            NodoFS nodo = buscarSinCandado(op.ruta(), false);
            if (nodo == null) {
                throw new OperacionRechazada("no existe " + op.ruta());
            }
            if (nodo.tieneEliminacionPendienteArriba()) {
                throw new OperacionRechazada(op.ruta() + " tiene una eliminación pendiente");
            }
            if (nodo == raiz && op.operacion() != TipoOperacionES.LEER) {
                throw new OperacionRechazada("la raíz no se puede modificar");
            }
            if (op.operacion() == TipoOperacionES.LEER && nodo.esDirectorio()) {
                throw new OperacionRechazada("solo se pueden leer archivos y " + op.ruta() + " es un directorio");
            }
            String negado = verificar(usuario, op.operacion(), nodo);
            if (negado != null) {
                throw new OperacionRechazada(negado);
            }
            Solicitud s;
            switch (op.operacion()) {
                case LEER ->
                    s = new Solicitud(proceso, TipoOperacionES.LEER, "Leer " + nodo.ruta(), cadenaDe(nodo));
                case ACTUALIZAR -> {
                    String nuevo = op.nombreNuevo();
                    if (nuevo != null) {
                        String problema = revisarNombre(nuevo, nodo.getPadre(), nodo);
                        if (problema != null) {
                            throw new OperacionRechazada(problema);
                        }
                    }
                    //renombrar escribe la cabecera del archivo, que esta en su primer bloque
                    int[] cabecera = nodo.esDirectorio() ? new int[0] : new int[]{nodo.getPrimerBloque()};
                    s = new Solicitud(proceso, TipoOperacionES.ACTUALIZAR, "Actualizar " + nodo.ruta()
                            + (nuevo != null ? " a " + nuevo : " (sin nombre nuevo)"), cabecera);
                    s.setNombreNuevo(nuevo);
                }
                default -> {   //ELIMINAR
                    if (nodo.tienePendientesEnSuArbol()) {
                        throw new OperacionRechazada(op.ruta() + " tiene operaciones pendientes");
                    }
                    s = new Solicitud(proceso, TipoOperacionES.ELIMINAR, "Eliminar " + nodo.ruta(), bloquesDeSuArbol(nodo));
                    nodo.setEliminacionPendiente(true);
                }
            }
            nodo.sumarPendiente(1);
            s.setNodo(nodo);
            return s;
        } finally {
            mutex.release();
        }
    }

    private Solicitud prepararCreacion(PCB proceso, OperacionArchivo op, String usuario) throws OperacionRechazada {
        String ruta = op.ruta();
        if (ruta == null || !ruta.startsWith("/") || ruta.endsWith("/")) {
            throw new OperacionRechazada("la ruta " + ruta + " no es válida");
        }
        int corte = ruta.lastIndexOf('/');
        String nombre = ruta.substring(corte + 1);
        NodoFS padre = buscarSinCandado(corte == 0 ? "/" : ruta.substring(0, corte), false);
        if (padre == null || !padre.esDirectorio()) {
            throw new OperacionRechazada("no existe el directorio " + (corte == 0 ? "/" : ruta.substring(0, corte)));
        }
        if (padre.tieneEliminacionPendienteArriba()) {
            throw new OperacionRechazada(padre.ruta() + " tiene una eliminación pendiente");
        }
        String problema = revisarNombre(nombre, padre, null);
        if (problema != null) {
            throw new OperacionRechazada(problema);
        }
        String negado = verificar(usuario, TipoOperacionES.CREAR, padre);
        if (negado != null) {
            throw new OperacionRechazada(negado);
        }
        //el administrador puede dar el elemento nuevo a otro usuario; un usuario crea para si mismo
        String dueno = op.dueno() != null ? op.dueno() : usuario;
        if (!usuarioValido(dueno)) {
            throw new OperacionRechazada("el dueño " + dueno + " no existe");
        }
        if (!dueno.equals(usuario) && !ModoUsuario.CUENTA_ADMIN.equals(usuario)) {
            throw new OperacionRechazada("solo el administrador puede crear elementos para otro usuario");
        }
        Permiso permiso = op.permiso() != null ? op.permiso() : Permiso.PUBLICO;
        NodoFS nuevo = new NodoFS(nombre, op.tipoNodo(), dueno, permiso, proceso == null ? "-" : proceso.toString());
        int[] cadena = new int[0];
        if (op.tipoNodo() == TipoNodo.ARCHIVO) {
            if (op.bloques() < Limites.TAMANO_ARCHIVO_MIN) {
                throw new OperacionRechazada("un archivo ocupa al menos un bloque");
            }
            cadena = disco.primerosLibres(op.bloques(), false);
            if (cadena == null) {
                throw new OperacionRechazada("no hay bloques libres suficientes: necesita " + op.bloques()
                        + " y hay " + disco.libresEnZona(false));
            }
            nuevo.ubicar(cadena[0], cadena.length);
            nuevo.setColor(proximoColor++);
        }
        nuevo.setPendiente(true);            //no se muestra hasta que el disco termine de escribirlo
        padre.agregarHijo(nuevo);
        padre.sumarPendiente(1);             //mientras tanto, el directorio no se puede eliminar
        ocuparCadena(cadena, nuevo.ruta(), nuevo.getColor());
        Solicitud s = new Solicitud(proceso, TipoOperacionES.CREAR, "Crear " + nuevo.ruta()
                + (cadena.length > 0 ? " (" + bloques(cadena.length) + ")" : " (directorio)"), cadena);
        s.setNodo(nuevo);
        return s;
    }

    //nombre no vacio, sin '/', con largo maximo y sin repetirse en el directorio
    private static String revisarNombre(String nombre, NodoFS directorio, NodoFS mismo) {
        if (nombre == null || nombre.isBlank()) {
            return "el nombre no puede estar vacío";
        }
        if (nombre.contains("/")) {
            return "el nombre no puede tener '/'";
        }
        if (nombre.length() > Limites.NOMBRE_ARCHIVO_MAX) {
            return "el nombre admite hasta " + Limites.NOMBRE_ARCHIVO_MAX + " caracteres";
        }
        NodoFS otro = directorio.hijo(nombre, true);   //el mismo elemento no cuenta como repetido
        if (otro != null && otro != mismo) {
            return "ya existe " + nombre + " en " + directorio.ruta();
        }
        return null;
    }

    //todos los bloques de los archivos de un arbol, en orden (eliminar un directorio los recorre todos)
    private int[] bloquesDeSuArbol(NodoFS nodo) {
        ListaEnlazada<NodoFS> archivos = new ListaEnlazada<>();
        juntarArchivos(nodo, archivos);
        int total = 0;
        for (NodoFS a : archivos.aArreglo(new NodoFS[archivos.tamano()])) {
            total += a.getCantidadBloques();
        }
        int[] r = new int[total];
        int i = 0;
        for (NodoFS a : archivos.aArreglo(new NodoFS[archivos.tamano()])) {
            for (int b : cadenaDe(a)) {
                r[i++] = b;
            }
        }
        return r;
    }

    private static void juntarArchivos(NodoFS nodo, ListaEnlazada<NodoFS> destino) {
        if (!nodo.esDirectorio()) {
            destino.agregarAlFinal(nodo);
            return;
        }
        for (NodoFS h : nodo.getHijos()) {
            juntarArchivos(h, destino);
        }
    }

    /**
     * Aplica la solicitud terminada. Devuelve false si el cambio ya no puede
     * hacerse.
     */
    public boolean completar(Solicitud s, long ciclo) {
        mutex.acquireUninterruptibly();
        try {
            NodoFS nodo = s.getNodo();
            switch (s.getOperacion()) {
                case CREAR -> {
                    nodo.setPendiente(false);
                    nodo.getPadre().sumarPendiente(-1);
                    registro.registrar(ciclo, "Se crea " + nodo.ruta()
                            + (nodo.esDirectorio() ? " (directorio)" : " en los bloques " + textoCadena(s.bloques())));
                }
                case LEER -> {
                    nodo.sumarPendiente(-1);
                    registro.registrar(ciclo, "Se leyó " + nodo.ruta() + " (" + bloques(s.bloques().length) + ")");
                }
                case ACTUALIZAR -> {
                    nodo.sumarPendiente(-1);
                    String nuevo = s.getNombreNuevo();
                    if (nuevo == null) {
                        registro.registrar(ciclo, "Se actualiza " + nodo.ruta()
                                + " sin cambiar el nombre: la solicitud no trae nombre nuevo");
                    } else if (nuevo.equals(nodo.getNombre())) {
                        registro.registrar(ciclo, "Se actualiza " + nodo.ruta()
                                + " sin cambiar el nombre: el nombre nuevo es igual al actual");
                    } else {
                        String problema = revisarNombre(nuevo, nodo.getPadre(), nodo);
                        if (problema != null) {
                            registro.registrar(ciclo, "No se renombra " + nodo.ruta() + ": " + problema);
                            return false;
                        }
                        String antes = nodo.ruta();
                        nodo.renombrar(nuevo);
                        reetiquetar(nodo);
                        registro.registrar(ciclo, antes + " se renombra a " + nodo.ruta());
                    }
                }
                case ELIMINAR -> {
                    for (int b : s.bloques()) {
                        disco.liberar(b);
                    }
                    String ruta = nodo.ruta();
                    nodo.getPadre().quitarHijo(nodo);
                    nodo.setEliminacionPendiente(false);
                    nodo.sumarPendiente(-1);
                    registro.registrar(ciclo, "Se elimina " + ruta + " y " + seLiberan(s.bloques().length));
                }
                default -> {
                }
            }
            return true;
        } finally {
            mutex.release();
        }
    }

    /**
     * La solicitud se canceló antes de que el disco la empezara: se deshacen
     * las reservas.
     */
    public void cancelar(Solicitud s, long ciclo) {
        mutex.acquireUninterruptibly();
        try {
            NodoFS nodo = s.getNodo();
            switch (s.getOperacion()) {
                case CREAR -> {
                    for (int b : s.bloques()) {
                        disco.liberar(b);
                    }
                    String ruta = nodo.ruta();
                    nodo.getPadre().sumarPendiente(-1);
                    nodo.getPadre().quitarHijo(nodo);
                    registro.registrar(ciclo, "Se cancela la creación de " + ruta
                            + (s.bloques().length > 0 ? " y " + seLiberan(s.bloques().length) : ""));
                }
                case ELIMINAR -> {
                    nodo.setEliminacionPendiente(false);
                    nodo.sumarPendiente(-1);
                }
                default ->
                    nodo.sumarPendiente(-1);
            }
        } finally {
            mutex.release();
        }
    }

    //al renombrar, las rutas cambian: se actualiza el ocupante de los bloques de los archivos de adentro
    private void reetiquetar(NodoFS nodo) {
        ListaEnlazada<NodoFS> archivos = new ListaEnlazada<>();
        juntarArchivos(nodo, archivos);
        archivos.paraCada(a -> {
            int[] cadena = cadenaDe(a);
            ocuparCadena(cadena, a.ruta(), a.getColor());
        });
    }

    private static String textoCadena(int[] cadena) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cadena.length; i++) {
            sb.append(i == 0 ? "" : ", ").append(cadena[i]);
        }
        return sb.toString();
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

    //"se libera 1 bloque" o "se liberan n bloques"
    private static String seLiberan(int n) {
        return (n == 1 ? "se libera " : "se liberan ") + bloques(n);
    }
}
