package avilaos.config;

/**
 * Forma del JSON del sistema de archivos, campo por campo como en el enunciado, para que
 * Gson lo lea y lo escriba. Los números son Integer para detectar los que faltan (null).
 * "chain" es un campo propio y opcional: los bloques del archivo en orden, cuando no son seguidos.
 */
public class JsonSistemaArchivos {

    DiskConfig diskConfig;
    Entrada[] fileSystem;

    static class DiskConfig {
        Integer totalBlocks;
        Integer initialHead;
    }

    static class Entrada {
        String path;
        String type;
        Integer startBlock;
        Integer blocks;
        String owner;
        String permissions;
        int[] chain;
    }
}