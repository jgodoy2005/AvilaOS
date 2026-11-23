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
public class Disco {

    private final Bloque[] bloques;
    private final int bloquesArchivos;
    private final Semaphore mapa = new Semaphore(1, true);

    private int cabezal;
    private DireccionCabezal direccion;
    private int velocidad;                  //posiciones que recorre el cabezal por ciclo

    public Disco(int bloquesArchivos, int bloquesSwap, int cabezalInicial,
                 DireccionCabezal direccionInicial, int velocidad) {
        if (bloquesArchivos <= 0 || bloquesSwap < 0) {
            throw new IllegalArgumentException("El disco necesita al menos un bloque de archivos");
        }
        this.bloquesArchivos = bloquesArchivos;
        this.bloques = new Bloque[bloquesArchivos + bloquesSwap];
        for (int i = 0; i < bloques.length; i++) {
            bloques[i] = new Bloque(i, i >= bloquesArchivos);
        }
        if (cabezalInicial < 0 || cabezalInicial >= bloques.length) {
            throw new IllegalArgumentException("El cabezal debe estar entre 0 y " + (bloques.length - 1));
        }
        this.cabezal = cabezalInicial;
        this.direccion = direccionInicial;
        this.velocidad = velocidad;
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

    public int getTotalBloques() { return bloques.length; }
    public int getBloquesArchivos() { return bloquesArchivos; }
    public int getBloquesSwap() { return bloques.length - bloquesArchivos; }
    public int getUltimoBloque() { return bloques.length - 1; }
    public int getCabezal() { return cabezal; }
    public DireccionCabezal getDireccion() { return direccion; }
    public int getVelocidad() { return velocidad; }

    public void setVelocidad(int velocidad) {
        if (velocidad < 1) {
            throw new IllegalArgumentException("La velocidad debe ser al menos 1 posición por ciclo");
        }
        this.velocidad = velocidad;
    }
}
