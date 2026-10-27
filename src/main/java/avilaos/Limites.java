/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos;

/**
 *
 * @author juandiegogodoy
 */
public final class Limites {
    
    private Limites(){
    }
    
    public static final int COMPUTADORES_MIN = 2; //rango de computadores del cluster
    public static final int COMPUTADORES_MAX = 8;
    
    public static final int RAM_MIN_MB = 128; //rango de ram de un computador
    public static final int RAM_MAX_MB = 65536;
    
    public static final int DURACION_CICLO_MIN_MS = 10; //rango de duracion de un ciclo de reloj general
    public static final int DURACION_CICLO_MAX_MS = 5000;
    
    public static final int QUANTUM_MIN = 1; // rango de quantum (turno) en Round Robin
    public static final int QUANTUM_MAX = 100;
    
    public static final int LATENCIA_MIN = 0; // rango de ciclos que tarda cada operacion sobre un buffer remoto
    public static final int LATENCIA_MAX = 100;

    public static final int NOMBRE_MAX = 20; // maximo de caracteres de nombre de un proceso
    
    public static final int INSTRUCCIONES_MIN = 1; //rango de instrucciones que puede tener un proceso
    public static final int INSTRUCCIONES_MAX = 100000;
    
    public static final int MEMORIA_PROCESO_MIN_MB = 1; //rango de memoria que puede tener un proceso
    public static final int MEMORIA_PROCESO_MAX_MB = 65536;
    
    public static final int PRIORIDAD_MIN = 1; // rango de prioridad que puede tener un proceso
    public static final int PRIORIDAD_MAX = 10; // 1 mas alta, 10 mas baja
    
    public static final int DEADLINE_MIN = 1; //rango de deadline de un proceso
    public static final int DEADLINE_MAX = 1000000;

    public static final int CICLOS_ENTRE_ES_MIN = 0;   //rango de cada cuantas instrucciones pide E/S
    public static final int CICLOS_ENTRE_ES_MAX = 10000; //si es 0 no pide E/S

    public static final int DURACION_ES_MIN = 1; //rango de cuantos ciclos de reloj puede durar la E/S de un proceso
    public static final int DURACION_ES_MAX = 10000;

    public static final int CICLOS_POR_ELEMENTO_MIN = 1; // rango de cuantos ciclos puede tomar producir o consumir un elemento
    public static final int CICLOS_POR_ELEMENTO_MAX = 1000;

    public static final int ELEMENTOS_MIN = 1; //rango de cuantos elementos puede producir o consumir un proceso
    public static final int ELEMENTOS_MAX = 10000;
    
    public static final int CAPACIDAD_BUFFER_MIN = 1; // rango de espacios de un buffer
    public static final int CAPACIDAD_BUFFER_MAX = 100;
    
}
