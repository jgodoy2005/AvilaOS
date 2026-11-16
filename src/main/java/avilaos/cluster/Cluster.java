/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.cluster;

import avilaos.Limites;
import avilaos.estructuras.ListaEnlazada;
import avilaos.so.Parametros;
import avilaos.so.RegistroEventos;
import avilaos.so.planificacion.TipoPolitica;
import avilaos.so.sincronizacion.BufferAcotado;

/**
 *
 * @author juandiegogodoy
 */
public class Cluster {
    
    private final ListaEnlazada<Computador> computadores = new ListaEnlazada<>();
    private final ListaEnlazada<BufferAcotado> buffers = new ListaEnlazada<>();
    private final Parametros parametros;
    private final RegistroEventos registro;

    public Cluster(Parametros parametros, RegistroEventos registro) {
        this.parametros = parametros;
        this.registro = registro;
    }
    
    //agregar un computador antes de iniciar la simulacion (asigna el numero automaticamente)
    public Computador agregarComputador(int ramMB, TipoPolitica politicaInicial) {
        Computador c = new Computador(computadores.tamano() + 1, ramMB, politicaInicial, parametros, registro);
        computadores.agregarAlFinal(c);
        return c;
    }
    
    //consultar un computador en especifico
    public Computador getComputador(int numero) {
        if (numero < 1 || numero > computadores.tamano()) {
            throw new IllegalArgumentException("No existe el computador " + numero);
        }
        return computadores.obtener(numero - 1);
    }
    
    //ver cuantos computadores hay en el cluster (un numero, cuantos)
    public int cantidadComputadores() {
        return computadores.tamano();
    }
    
    //ver la lista de computadores del cluster, en un arreglo
    public Computador[] getComputadores() {
        return computadores.aArreglo(new Computador[computadores.tamano()]);
    }

    
    
    //crear un buffer en la ram de su computador 'anfitrion'
    public BufferAcotado crearBuffer(int capacidad, int numeroAnfitrion, long ciclo) {
        if (capacidad < Limites.CAPACIDAD_BUFFER_MIN || capacidad > Limites.CAPACIDAD_BUFFER_MAX) {
            throw new IllegalArgumentException("La capacidad debe estar entre " + Limites.CAPACIDAD_BUFFER_MIN
                    + " y " + Limites.CAPACIDAD_BUFFER_MAX);
        }
        Computador anfitrion = getComputador(numeroAnfitrion);
        String nombre = "B" + (buffers.tamano() + 1);
        BufferAcotado buffer = new BufferAcotado(nombre, capacidad, numeroAnfitrion);
        
        //si el anfitrion no tiene suficiente memoria para el buffer no lo crea
        if (!anfitrion.getMemoria().reservar(buffer.memoriaMB())) {
            throw new IllegalArgumentException("El " + anfitrion.getNombre() + " no tiene "
                    + buffer.memoriaMB() + " MB libres para el buffer");
        }
        buffers.agregarAlFinal(buffer);
        registro.registrar(ciclo, "Se crea el buffer " + nombre + " (capacidad " + capacidad + ") en el "
                + anfitrion.getNombre() + "; ocupa " + buffer.memoriaMB() + " MB de su RAM");
        return buffer;
    }

    //busca un buffer del cluster por su nombre 
     public BufferAcotado buscarBuffer(String nombre) {
        return buffers.buscar(b -> b.getNombre().equalsIgnoreCase(nombre));
    }

     //ver todos los buffers creados del cluster
    public BufferAcotado[] getBuffers() {
        return buffers.aArreglo(new BufferAcotado[buffers.tamano()]);
    }
}


