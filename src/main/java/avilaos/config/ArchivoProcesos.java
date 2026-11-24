/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.config;

import avilaos.Limites;
import avilaos.so.DatosProceso;
import avilaos.so.ModoUsuario;
import avilaos.so.OperacionArchivo;
import avilaos.so.TipoOperacionES;
import avilaos.so.archivos.Permiso;
import avilaos.so.archivos.TipoNodo;
import avilaos.so.disco.TipoPoliticaDisco;
import avilaos.so.planificacion.TipoPolitica;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 *
 * @author juandiegogodoy
 * Lee el JSON de procesos y configuración (globalConfig + computer + processes).
 *
 * Cada proceso del JSON es un proceso de E/S de disco que lanza el administrador. Su primera
 * instrucción es la operación de archivo; si el archivo no existe o no hay espacio, eso se
 * descubre al ejecutarla (el proceso termina con "Operación rechazada").
 */

public final class ArchivoProcesos {
    private static final Gson GSON = FormatoJson.lector().create();

    private ArchivoProcesos() {
    }

    public static DatosProcesos cargar(Path archivo) throws IOException {
        try (Reader lector = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
            return convertir(leer(lector));
        }
    }

    public static DatosProcesos desdeTexto(String json) throws IOException {
        return convertir(leer(new StringReader(json)));
    }

    private static JsonProcesos leer(Reader lector) throws IOException {
        JsonProcesos json;
        try {
            json = GSON.fromJson(lector, JsonProcesos.class);
        } catch (JsonParseException e) {
            throw new IOException("El archivo no es un JSON válido de procesos: " + e.getMessage(), e);
        }
        if (json == null) {
            throw new IOException("El archivo está vacío");
        }
        return json;
    }

    //revisa todo y junta los errores en un solo mensaje
    private static DatosProcesos convertir(JsonProcesos json) {
        StringBuilder errores = new StringBuilder();

        int duracion = 0;
        int swap = 0;
        TipoPoliticaDisco politicaDisco = null;
        if (json.globalConfig == null) {
            errores.append("- Falta \"globalConfig\".\n");
        } else {
            duracion = numero(errores, "cycleDurationMs", json.globalConfig.cycleDurationMs,
                    Limites.DURACION_CICLO_MIN_MS, Limites.DURACION_CICLO_MAX_MS);
            swap = numero(errores, "swapSizeBlocks", json.globalConfig.swapSizeBlocks, Limites.SWAP_MIN, Limites.SWAP_MAX);
            politicaDisco = TipoPoliticaDisco.desdeTexto(json.globalConfig.initialDiskPolicy);
            if (politicaDisco == null) {
                errores.append("- \"initialDiskPolicy\" debe ser FIFO, SSTF, SCAN o C-SCAN.\n");
            }
        }

        int ram = 0;
        TipoPolitica politica = null;
        int quantum = Limites.QUANTUM_DEFECTO;
        if (json.computer == null) {
            errores.append("- Falta \"computer\".\n");
        } else {
            ram = numero(errores, "ramCapacity", json.computer.ramCapacity, Limites.RAM_MIN_MB, Limites.RAM_MAX_MB);
            politica = TipoPolitica.desdeTexto(json.computer.policy);
            if (politica == null) {
                errores.append("- \"policy\" debe ser FCFS, ROUND_ROBIN, PRIORIDAD o EDF.\n");
            } else if (politica == TipoPolitica.ROUND_ROBIN) {
                //el quantum solo se usa con Round Robin; con las demas politicas se ignora el del JSON
                quantum = numero(errores, "quantum (Round Robin)", json.computer.quantum, Limites.QUANTUM_MIN, Limites.QUANTUM_MAX);
            }
        }

        DatosProceso[] procesos = new DatosProceso[json.processes == null ? 0 : json.processes.length];
        if (json.processes == null) {
            errores.append("- Falta \"processes\" (puede ser una lista vacía).\n");
        }
        for (int i = 0; i < procesos.length; i++) {
            procesos[i] = convertir(json.processes[i], i + 1, ram, errores);
        }
        if (errores.length() > 0) {
            throw new IllegalArgumentException(errores.toString().trim());
        }
        return new DatosProcesos(duracion, swap, politicaDisco, ram, politica, quantum, procesos);
    }

    private static DatosProceso convertir(JsonProcesos.Proceso p, int numero, int ram, StringBuilder errores) {
        if (p == null) {
            errores.append("- El proceso ").append(numero).append(" está vacío.\n");
            return null;
        }
        String quien = "Proceso " + numero + (p.name != null ? " (" + p.name + ")" : "");
        StringBuilder suyos = new StringBuilder();
        if (p.name == null || p.name.isBlank()) {
            suyos.append("falta \"name\"; ");
        } else if (p.name.length() > Limites.NOMBRE_MAX) {
            suyos.append("\"name\" admite hasta ").append(Limites.NOMBRE_MAX).append(" caracteres; ");
        }
        int memoria = campo(suyos, "memory", p.memory, Limites.MEMORIA_PROCESO_MIN_MB, Limites.MEMORIA_PROCESO_MAX_MB);
        if (ram > 0 && memoria > ram) {
            suyos.append("pide ").append(memoria).append(" MB y la RAM tiene ").append(ram).append(" MB; ");
        }
        int deadline = campo(suyos, "deadline", p.deadline, Limites.DEADLINE_MIN, Limites.DEADLINE_MAX);
        int instrucciones = campo(suyos, "instructions", p.instructions, Limites.INSTRUCCIONES_MIN, Limites.INSTRUCCIONES_MAX);
        int prioridad = p.priority == null ? Limites.PRIORIDAD_DEFECTO
                : campo(suyos, "priority", p.priority, Limites.PRIORIDAD_MIN, Limites.PRIORIDAD_MAX);
        if (p.targetPath == null || !p.targetPath.startsWith("/")) {
            suyos.append("\"targetPath\" debe ser una ruta que empiece con /; ");
        }
        TipoOperacionES operacion = TipoOperacionES.desdeJson(p.operation);
        if (operacion == null) {
            suyos.append("\"operation\" debe ser CREATE, READ, UPDATE o DELETE; ");
        }
        OperacionArchivo archivo = null;
        if (operacion != null && p.targetPath != null) {
            archivo = operacion(p, operacion, suyos);
        }
        if (suyos.length() > 0) {
            errores.append("- ").append(quien).append(": ").append(suyos.substring(0, suyos.length() - 2)).append(".\n");
            return null;
        }
        //un solo computador (el 1); los procesos del JSON los lanza el administrador
        return DatosProceso.deDisco(p.name, archivo, instrucciones, memoria, prioridad, deadline, 1);
    }

    //arma la operacion de archivo; los campos que no aplican a la operacion se ignoran
    private static OperacionArchivo operacion(JsonProcesos.Proceso p, TipoOperacionES operacion, StringBuilder suyos) {
        String admin = ModoUsuario.CUENTA_ADMIN;
        switch (operacion) {
            case LEER:
                return OperacionArchivo.leer(p.targetPath, admin);
            case ELIMINAR:
                return OperacionArchivo.eliminar(p.targetPath, admin);
            case ACTUALIZAR:
                return OperacionArchivo.renombrar(p.targetPath, p.newName, admin);   //sin newName: no cambia el nombre
            default: {   //CREATE
                TipoNodo tipo = TipoNodo.desdeJson(p.type);
                Permiso permiso = p.permissions == null ? Permiso.PUBLICO : Permiso.desdeJson(p.permissions);
                if (tipo == null) {
                    suyos.append("para CREATE, \"type\" debe ser FILE o DIRECTORY; ");
                }
                if (permiso == null) {
                    suyos.append("\"permissions\" debe ser public o private; ");
                }
                if (tipo == TipoNodo.ARCHIVO && (p.fileSizeBlocks == null || p.fileSizeBlocks < Limites.TAMANO_ARCHIVO_MIN)) {
                    suyos.append("para crear un archivo, \"fileSizeBlocks\" debe ser al menos ").append(Limites.TAMANO_ARCHIVO_MIN).append("; ");
                }
                if (p.owner != null && !p.owner.equals(admin) && !p.owner.matches("user[0-9]+")) {
                    suyos.append("\"owner\" debe ser admin o userN; ");
                }
                if (tipo == null || permiso == null) {
                    return null;
                }
                return tipo == TipoNodo.ARCHIVO
                        ? OperacionArchivo.crearArchivo(p.targetPath, p.fileSizeBlocks == null ? 0 : p.fileSizeBlocks, permiso, p.owner, admin)
                        : OperacionArchivo.crearDirectorio(p.targetPath, permiso, p.owner, admin);
            }
        }
    }

    //numero obligatorio de la configuracion
    private static int numero(StringBuilder errores, String campo, Integer valor, int min, int max) {
        if (valor == null) {
            errores.append("- Falta \"").append(campo).append("\".\n");
            return 0;
        }
        if (valor < min || valor > max) {
            errores.append("- \"").append(campo).append("\" debe estar entre ").append(min).append(" y ").append(max).append(".\n");
        }
        return valor;
    }

    //numero obligatorio de un proceso
    private static int campo(StringBuilder suyos, String campo, Integer valor, int min, int max) {
        if (valor == null) {
            suyos.append("falta \"").append(campo).append("\"; ");
            return 0;
        }
        if (valor < min || valor > max) {
            suyos.append("\"").append(campo).append("\" debe estar entre ").append(min).append(" y ").append(max).append("; ");
        }
        return valor;
    }
}
