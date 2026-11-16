/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Enum.java to edit this template
 */
package avilaos.so;
import avilaos.hardware.ModoCPU;
/**
 *
 * @author juandiegogodoy
 */
public enum TipoInstruccion {
    CALCULO("Cálculo", ModoCPU.USUARIO),
    PRODUCIR("producir()", ModoCPU.USUARIO),
    CONSUMIR("consumir()", ModoCPU.USUARIO),
    SOLICITAR_ES("Solicitar E/S", ModoCPU.SISTEMA_OPERATIVO),
    SEM_WAIT_E("semWait(e)", ModoCPU.SISTEMA_OPERATIVO),
    SEM_WAIT_N("semWait(n)", ModoCPU.SISTEMA_OPERATIVO),
    SEM_WAIT_S("semWait(s)", ModoCPU.SISTEMA_OPERATIVO),
    SEM_SIGNAL_E("semSignal(e)", ModoCPU.SISTEMA_OPERATIVO),
    SEM_SIGNAL_N("semSignal(n)", ModoCPU.SISTEMA_OPERATIVO),
    SEM_SIGNAL_S("semSignal(s)", ModoCPU.SISTEMA_OPERATIVO),
    INSERTAR("añadir()", ModoCPU.SISTEMA_OPERATIVO),
    EXTRAER("extraer()", ModoCPU.SISTEMA_OPERATIVO);
    
    private final String texto;
    private final ModoCPU modo;
    
    TipoInstruccion(String texto, ModoCPU modo){
        this.texto = texto;
        this.modo = modo;
    }
    
    public ModoCPU getModo(){
        return modo;
    }
    
    public boolean esOperacionDeBuffer(){
        return modo == ModoCPU.SISTEMA_OPERATIVO && this != SOLICITAR_ES;
    }
    
    @Override
   public String toString(){
       return texto;
   }
}
