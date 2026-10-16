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

    public static final int NOMBRE_MAX = 20;
    
    public static final int INSTRUCCIONES_MIN = 1;
    public static final int INSTRUCCIONES_MAX = 100000;
    
    public static final int MEMORIA_PROCESO_MIN_MB = 1;
    public static final int MEMORIA_PROCESO_MAX_MB = 65536;
    
    public static final int PRIORIDAD_MIN = 1;
    public static final int PRIORIDAD_MAX = 10;
    
    public static final int DEADLINE_MIN = 1;
    public static final int DEADLINE_MAX = 1000000;

    public static final int CICLOS_ENTRE_ES_MIN = 0;   
    public static final int CICLOS_ENTRE_ES_MAX = 10000;

    public static final int DURACION_ES_MIN = 1;
    public static final int DURACION_ES_MAX = 10000;

    public static final int CICLOS_POR_ELEMENTO_MIN = 1;
    public static final int CICLOS_POR_ELEMENTO_MAX = 1000;

    public static final int ELEMENTOS_MIN = 1;
    public static final int ELEMENTOS_MAX = 10000;
    
}
