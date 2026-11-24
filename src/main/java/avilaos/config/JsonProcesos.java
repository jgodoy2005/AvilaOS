/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.config;

/**
 *
 * @author juandiegogodoy
 * Forma del JSON de procesos y configuración, campo por campo como en el enunciado, para que
 * Gson lo lea. Los números son Integer para detectar los que faltan (null).
 *
 * Campos propios y opcionales de cada proceso: priority (1 a 10, por defecto 5), newName (UPDATE),
 * y para CREATE: type (FILE o DIRECTORY), fileSizeBlocks (FILE), permissions y owner.
 */
public class JsonProcesos {
     GlobalConfig globalConfig;
    Computer computer;
    Proceso[] processes;

    static class GlobalConfig {
        Integer cycleDurationMs;
        Integer swapSizeBlocks;
        String initialDiskPolicy;
    }

    static class Computer {
        Integer ramCapacity;
        String policy;
        Integer quantum;
    }

    static class Proceso {
        String name;
        Integer memory;
        Integer deadline;
        Integer instructions;
        String targetPath;
        String operation;
        Integer priority;
        String newName;
        String type;
        Integer fileSizeBlocks;
        String permissions;
        String owner;
    }
}
