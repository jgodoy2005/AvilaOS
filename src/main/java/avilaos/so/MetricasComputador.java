/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.so;
import avilaos.hardware.ModoCPU;
/**
 *
 * @author juandiegogodoy
 */

/*
ESQUELETO PARA LAS METRICAS
*/

/*
@author erick
contabilidad individual de un computador.
registra exclusivamente sumas y conteos (nunca promedios) para permitir
la combinación exacta de métricas entre varios nodos clúster.
Solo debe ser modificada por el hilo asignado a su propio computador.
*/
public class MetricasComputador {
    
    //Tamano fijado para calcular la utilizacion reciente en la grafica
    public static final int VENTANA_UTILIZACION =20;
    
    // Contadores de ciclos de CPU
    private long ciclos;
    private long ciclosOcupados;
    private long ciclosUsuario;
    private long ciclosSistema;
    
    // Arreglo circular para rastrer a los ultimos N ciclos ejecutados
    private boolean[] ventana = new boolean[VENTANA_UTILIZACION];
    private int ocupadosEnVentana;
    
    // Contadores de procesos
    private int compleados;
    private int abortados;
    private int sinCPU;     // Proceso abortado que sufrio inanicion total
    
    private long sumaRespuesta;
    private int conRespuesta;
    private long sumaRetorno;       // solo completados
    private long sumaEspera;
    private double sumaX;           // indice de Jain sobre x = servicio/tiempo en el sistema
    private double sumaX2;
    private int conX;
    private long sumaBloqueoSemaforo;   // solo produtores y consumidores
    private int terminadosPC;
    
    // Registra el resultado del ciclo de reloj que acaba de ejecutar la CPU 
    public void registrarCiclo(boolean ocupada, ModoCPU modo) { 
        // mapeo circular para reutilizar la posicion del ciclo mas antiguo
        int posicion = (int) (ciclos % ventan.length);
        
        // Si la casilla a sobreescribir registraba ocupacion, la descontamos del total
        if (ventana[posicion]) {
            ocupadosEnVentana--;
        }
        
        // Actualizamos la casilla con el estado actual
        ventana[posicion] = ocupada;
        
        if (ocupada) {
            ocupadoEnVentana++;
            ciclosOcupada++;
            
            if (modo == ModoCPU.USUARIO0) {
                ciclosUsuario++;
            } else {
                ciclosSistema++;
            }
        }
        ciclos++;
    }
    
    //Registra las estadisticas de un procesos al terminar (exito o deadline)
    public void registrarTerminacion(PCB p) { 
    // El tiempo de retorno contabiliza ambos extremos de la ventana temporal (+1)
    long retorno = p.getCicloFin() - p.getCicloCreacion() + 1;
    if (p.getMotivoTerminacion() == MotivoTerminacion.COMPLETADO) {
        completado++;
    } else {
        abortados++;
        if (p.getCicloPrimeraEjecucion() < 0) {
            sinCPU++; //Inanicion: murio sin nunca haber entrado al CPU
        }
    }
    
    // Metricas de respuesta: desde creacion hasta su primera instruccion ejecutada
    if (p.getCicloPrimeraEjecucion() >= 0){
        sumaRespuesta += p.getCicloPrimeraEjecucion() - p.getCicloCreacion();
        conRespuesta++;
    }
    
    sumaEspera += p.getCiclosEnListos();
    
    // Acumuladores para la formula del indice de Jain
    double x =(double) p.getCiclosEjecutados() / retorno;
    sumaX += x;
    sumaX2 += x * x;
    conX++;
    
    if (p.getTipo().usaBuffer()) {
        sumaBloqueoSemaforo += p.getCiclosBloqueadoSemaforo();
        terminadosPC++;
    }
  }
    
   public long getCiclos() { return ciclos; }
   public long getCiclosOcupada() { return ciclosOcupada; }
   public long getCiclosUsuario() { return ciclosUsuario; }
   public long getCiclosSistema() { return ciclosSistema; }
   public int getOcupadosEnVentana() { return ocupadosEnVentana; }
   public int getTamanoVentanaUsada() { return (int) Math.min(ciclos,
   ventana.length); }
   public int getCompletados() { return completados; }
   public int getAbortados() { return abortados; }
   public int getSinCPU() { return sinCPU; }
   public long getSumaRespuesta() { return sumaRespuesta; }
   public int getConRespuesta() { return conRespuesta; }
   public long getSumaRetorno() { return sumaRetorno; }
   public long getSumaEspera() { return sumaEspera; }
    public double getSumaX() { return sumaX; }
    public double getSumaX2() { return sumaX2; }
    public int getConX() { return conX; }
    public long getSumaBloqueoSemaforo() { return sumaBloqueoSemaforo; }
    public int getTerminadosPC() { return terminadosPC; }   
}
