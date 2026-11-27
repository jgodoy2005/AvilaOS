/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.hardware;

import java.util.concurrent.Semaphore;

/**
 *
 * @author erick
 */
public class Disco implements ComponenteReloj {

    private final Bloque[] bloques;
    private final int bloquesArchivos;
    private final int bloquesSwap;
    private final Semaphore mapa = new Semaphore(1, true);

    private ControladorDisco controlador;
    private ManejadorDisco manejador;

    private int cabezal;
    private DireccionCabezal direccion;
    private int velocidad;                  //posiciones que recorre el cabezal por ciclo

    private TrabajoDisco actual;            //trabajo en curso (null si el disco esta libre)
    private int indice;                     //posicion, dentro de la cadena, del proximo bloque a visitar
    private boolean detenido;               //detenido por el usuario: no toma trabajos nuevos
    private boolean deteniendo;             //se pidio detener: termina el trabajo actual y se detiene
    private long desplazamiento;

    public Disco(int bloquesArchivos, int bloquesSwap, int cabezalInicial, DireccionCabezal direccionInicial, int velocidad) {
        if (bloquesArchivos < 1) {
            throw new IllegalArgumentException("Debe haber al menos un bloque para archivos");
        }
        if (bloquesSwap < 0) {
            throw new IllegalArgumentException("Los bloques de swap no pueden ser negativos");
        }
        int total = bloquesArchivos + bloquesSwap;
        if (cabezalInicial < 0 || cabezalInicial >= total) {
            throw new IllegalArgumentException("El cabezal inicial debe estar entre 0 y " + (total - 1));
        }
        if (direccionInicial == null) {
            throw new IllegalArgumentException("Falta la dirección inicial del cabezal");
        }
        if (velocidad < 1) {
            throw new IllegalArgumentException("La velocidad del cabezal debe ser al menos 1");
        }
        this.bloquesArchivos = bloquesArchivos;
        this.bloquesSwap = bloquesSwap;
        this.bloques = new Bloque[total];
        for (int i = 0; i < total; i++) {
            this.bloques[i] = new Bloque(i, i >= bloquesArchivos);
        }

        this.cabezal = cabezalInicial;
        this.direccion = direccionInicial;
        this.velocidad = velocidad;
    }

    // conecta el disco con el planificador de disco y con el kernel
    public void conectar(ControladorDisco controlador, ManejadorDisco manejador) {
        this.controlador = controlador;
        this.manejador = manejador;
    }

    //un ciclo de trabajo del disco: o se mueve (hasta 'velocidad' posiciones) o lee/escribe un bloque
    @Override
    public void tic(long ciclo) {
        if (controlador != null) {
            controlador.recibir(ciclo);
        }

        avanzarCiclo(ciclo);

        if (controlador != null) {
            controlador.finalizarCiclo(ciclo);
        }
    }

    private void avanzarCiclo(long ciclo) {
        if (actual == null) {
            if (detenido || controlador == null) {
                return;
            }

            DecisionDisco d = controlador.siguiente(
                    cabezal, direccion, getUltimoBloque(), ciclo);

            switch (d.getTipo()) {
                case ESPERAR -> {
                    return;
                }
                case SALTAR_AL_INICIO -> {
                    cabezal = 0;
                    direccion = DireccionCabezal.ARRIBA;
                    return;
                }
                case MOVER -> {
                    direccion = d.getDireccion();
                    moverHacia(d.getDestino());
                    return;
                }
                case ATENDER -> {
                    actual = d.getTrabajo();
                    indice = 0;
                    direccion = d.getDireccion();
                }
            }
        }

        trabajar(ciclo);
    }

    //avanza el trabajo actual un ciclo
    private void trabajar(long ciclo) {
        int[] cadena = actual.bloques();
        if (cadena.length == 0) {          //operacion sobre un directorio: un ciclo sin mover el cabezal
            terminar(ciclo);
            return;
        }
        int objetivo = cadena[indice];
        if (cabezal != objetivo) {          //primero llegar al bloque
            moverHacia(objetivo);
            return;
        }
        indice++;                           //en este ciclo se lee o escribe el bloque 'objetivo'
        if (indice == cadena.length) {
            terminar(ciclo);
            return;
        }
        int siguiente = cadena[indice];
        if (Math.abs(siguiente - cabezal) == 1) {
            cabezal = siguiente;            //bloque vecino: se lee de corrido, sin ciclo de movimiento
            desplazamiento++;
        }
    }

    //el trabajo termino: se avisa al sistema operativo (interrupcion de fin de E/S)
    private void terminar(long ciclo) {
        TrabajoDisco terminado = actual;
        actual = null;
        indice = 0;
        if (deteniendo) {
            deteniendo = false;
            detenido = true;
        }
        if (manejador != null) {
            manejador.trabajoTerminado(terminado, ciclo);
        }
    }

    //acerca el cabezal al destino, como maximo 'velocidad' posiciones
    private void moverHacia(int destino) {
        int distancia = Math.abs(destino - cabezal);
        int paso = Math.min(velocidad, distancia);
        cabezal += destino > cabezal ? paso : -paso;
        desplazamiento += paso;
    }

    //CONTROLES DEL USUARIO (se llaman con el candado del ciclo tomado)
    //detener: si hay un trabajo en curso, lo termina y luego se detiene
    public void detener() {
        if (actual == null) {
            detenido = true;
        } else {
            deteniendo = true;
        }
    }

    public void reanudar() {
        detenido = false;
        deteniendo = false;
    }

    //solo con el disco detenido y libre; no suma desplazamiento porque no es trabajo del disco
    public void moverCabezal(int posicion, DireccionCabezal nuevaDireccion) {
        if (!detenido || actual != null) {
            throw new IllegalStateException("El cabezal solo se puede mover con el disco detenido y libre");
        }
        if (posicion < 0 || posicion > getUltimoBloque()) {
            throw new IllegalArgumentException("La posición debe estar entre 0 y " + getUltimoBloque());
        }
        cabezal = posicion;
        direccion = nuevaDireccion;
    }

    //MAPA DE BLOQUES (lo usan el sistema de archivos y el swap desde el hilo del kernel)
    //marca un bloque como ocupado e indica cual es el siguiente de su cadena
    public void ocupar(int bloque, String ocupante, int color, int siguiente) {
        mapa.acquireUninterruptibly();
        try {
            bloques[bloque].ocupar(ocupante, color, siguiente);
        } finally {
            mapa.release();
        }
    }

    //deja un bloque libre (sin dueño ni siguiente)
    public void liberar(int bloque) {
        mapa.acquireUninterruptibly();
        try {
            bloques[bloque].liberar();
        } finally {
            mapa.release();
        }
    }

    //el bloque que sigue en la cadena, o Bloque.FIN
    public int siguienteDe(int bloque) {
        mapa.acquireUninterruptibly();
        try {
            return bloques[bloque].getSiguiente();
        } finally {
            mapa.release();
        }
    }

    public boolean estaLibre(int bloque) {
        mapa.acquireUninterruptibly();
        try {
            return bloques[bloque].isLibre();
        } finally {
            mapa.release();
        }
    }

    //los primeros 'cantidad' bloques libres de una zona, en orden ascendente; null si no alcanzan
    public int[] primerosLibres(int cantidad, boolean zonaSwap) {
        mapa.acquireUninterruptibly();
        try {
            int desde = zonaSwap ? bloquesArchivos : 0;
            int hasta = zonaSwap ? bloques.length : bloquesArchivos;
            if (cantidad < 0 || cantidad > hasta - desde) {
                return null;
            }
            int[] elegidos = new int[cantidad];
            int n = 0;
            for (int i = desde; i < hasta && n < cantidad; i++) {
                if (bloques[i].isLibre()) {
                    elegidos[n++] = i;
                }
            }
            return n == cantidad ? elegidos : null;
        } finally {
            mapa.release();
        }
    }

    //cuantos bloques libres tiene una zona
    public int libresEnZona(boolean zonaSwap) {
        mapa.acquireUninterruptibly();
        try {
            int libres = 0;
            int desde = zonaSwap ? bloquesArchivos : 0;
            int hasta = zonaSwap ? bloques.length : bloquesArchivos;
            for (int i = desde; i < hasta; i++) {
                if (bloques[i].isLibre()) {
                    libres++;
                }
            }
            return libres;
        } finally {
            mapa.release();
        }
    }

    //copias del mapa para la foto de la interfaz
    public String[] ocupantes() {
        mapa.acquireUninterruptibly();
        try {
            String[] r = new String[bloques.length];
            for (int i = 0; i < bloques.length; i++) {
                r[i] = bloques[i].getOcupante();
            }
            return r;
        } finally {
            mapa.release();
        }
    }

    public int[] colores() {
        mapa.acquireUninterruptibly();
        try {
            int[] r = new int[bloques.length];
            for (int i = 0; i < bloques.length; i++) {
                r[i] = bloques[i].getColor();
            }
            return r;
        } finally {
            mapa.release();
        }
    }

    public int[] siguientes() {
        mapa.acquireUninterruptibly();
        try {
            int[] r = new int[bloques.length];
            for (int i = 0; i < bloques.length; i++) {
                r[i] = bloques[i].getSiguiente();
            }
            return r;
        } finally {
            mapa.release();
        }
    }

    //DATOS DEL DISCO
    public int getTotalBloques() {
        return bloques.length;
    }

    public int getBloquesArchivos() {
        return bloquesArchivos;
    }

    public int getBloquesSwap() {
        return bloques.length - bloquesArchivos;
    }

    public int getUltimoBloque() {
        return bloques.length - 1;
    }

    public int getCabezal() {
        return cabezal;
    }

    public DireccionCabezal getDireccion() {
        return direccion;
    }

    public int getVelocidad() {
        return velocidad;
    }

    public void setVelocidad(int velocidad) {
        if (velocidad < 1) {
            throw new IllegalArgumentException("La velocidad debe ser al menos 1 posición por ciclo");
        }
        this.velocidad = velocidad;
    }

    public TrabajoDisco getActual() {
        return actual;
    }

    public int getBloquesHechos() {
        return actual == null ? 0 : indice;
    }

    public boolean isDetenido() {
        return detenido;
    }

    public boolean isDeteniendo() {
        return deteniendo;
    }

    public long getDesplazamiento() {
        return desplazamiento;
    }
}
