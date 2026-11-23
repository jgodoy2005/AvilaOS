package avilaos.config;

import avilaos.Limites;
import avilaos.so.archivos.EntradaFS;
import avilaos.so.archivos.Permiso;
import avilaos.so.archivos.TipoNodo;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Lee y guarda el JSON del sistema de archivos (diskConfig + fileSystem).
 *
 * Aquí solo se revisa la forma: que estén los campos y que los textos sean válidos. Lo que
 * depende del disco y de los usuarios (bloques libres, cadenas, dueños) lo revisa
 * SistemaArchivos.cargar.
 */
public final class ArchivoSistemaArchivos {

    private static final Gson GSON = FormatoJson.lector().setPrettyPrinting().create();

    private ArchivoSistemaArchivos() {
    }

    //LEER

    public static DatosSistemaArchivos cargar(Path archivo) throws IOException {
        try (Reader lector = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
            return convertir(leer(lector));
        }
    }

    public static DatosSistemaArchivos desdeTexto(String json) throws IOException {
        return convertir(leer(new StringReader(json)));
    }

    private static JsonSistemaArchivos leer(Reader lector) throws IOException {
        JsonSistemaArchivos json;
        try {
            json = GSON.fromJson(lector, JsonSistemaArchivos.class);
        } catch (JsonParseException e) {
            throw new IOException("El archivo no es un JSON válido del sistema de archivos: " + e.getMessage(), e);
        }
        if (json == null) {
            throw new IOException("El archivo está vacío");
        }
        return json;
    }

    //revisa la forma y junta todos los errores en un solo mensaje
    private static DatosSistemaArchivos convertir(JsonSistemaArchivos json) {
        StringBuilder errores = new StringBuilder();
        int total = 0;
        int cabezal = 0;
        if (json.diskConfig == null) {
            errores.append("- Falta \"diskConfig\".\n");
        } else {
            if (json.diskConfig.totalBlocks == null) {
                errores.append("- Falta \"totalBlocks\" en \"diskConfig\".\n");
            } else {
                total = json.diskConfig.totalBlocks;
                if (total < Limites.BLOQUES_DISCO_MIN || total > Limites.BLOQUES_DISCO_MAX) {
                    errores.append("- \"totalBlocks\" debe estar entre ").append(Limites.BLOQUES_DISCO_MIN)
                           .append(" y ").append(Limites.BLOQUES_DISCO_MAX).append(".\n");
                }
            }
            if (json.diskConfig.initialHead == null) {
                errores.append("- Falta \"initialHead\" en \"diskConfig\".\n");
            } else {
                cabezal = json.diskConfig.initialHead;
                if (total > 0 && (cabezal < 0 || cabezal >= total)) {
                    errores.append("- \"initialHead\" debe estar entre 0 y ").append(total - 1).append(".\n");
                }
            }
        }
        if (json.fileSystem == null) {
            errores.append("- Falta \"fileSystem\".\n");
        }
        EntradaFS[] entradas = new EntradaFS[json.fileSystem == null ? 0 : json.fileSystem.length];
        for (int i = 0; i < entradas.length; i++) {
            entradas[i] = convertir(json.fileSystem[i], i + 1, errores);
        }
        if (errores.length() > 0) {
            throw new IllegalArgumentException(errores.toString().trim());
        }
        return new DatosSistemaArchivos(total, cabezal, entradas);
    }

    private static EntradaFS convertir(JsonSistemaArchivos.Entrada e, int numero, StringBuilder errores) {
        if (e == null) {
            errores.append("- El elemento ").append(numero).append(" de \"fileSystem\" está vacío.\n");
            return null;
        }
        String donde = "- " + (e.path != null ? e.path : "El elemento " + numero + " de \"fileSystem\"") + ": ";
        int antes = errores.length();
        if (e.path == null) {
            errores.append(donde).append("falta \"path\".\n");
        }
        TipoNodo tipo = TipoNodo.desdeJson(e.type);
        if (tipo == null) {
            errores.append(donde).append("\"type\" debe ser FILE o DIRECTORY.\n");
        }
        if (e.owner == null) {
            errores.append(donde).append("falta \"owner\".\n");
        }
        Permiso permiso = Permiso.desdeJson(e.permissions);
        if (permiso == null) {
            errores.append(donde).append("\"permissions\" debe ser public o private.\n");
        }
        if (tipo == TipoNodo.ARCHIVO) {
            if (e.startBlock == null) {
                errores.append(donde).append("falta \"startBlock\".\n");
            }
            if (e.blocks == null) {
                errores.append(donde).append("falta \"blocks\".\n");
            }
        }
        if (errores.length() > antes) {
            return null;
        }
        if (tipo == TipoNodo.DIRECTORIO) {
            return new EntradaFS(e.path, tipo, -1, 0, e.owner, permiso, null);   //los directorios no ocupan bloques
        }
        return new EntradaFS(e.path, tipo, e.startBlock, e.blocks, e.owner, permiso, e.chain);
    }

    //GUARDAR

    public static void guardar(Path archivo, int totalBloques, int cabezal, EntradaFS[] entradas) throws IOException {
        try (Writer escritor = Files.newBufferedWriter(archivo, StandardCharsets.UTF_8)) {
            escritor.write(aTexto(totalBloques, cabezal, entradas));
        }
    }

    /**
     * Arma el JSON con el estado actual. El cabezal puede estar en la zona de swap, que no es
     * parte de este archivo: en ese caso se guarda el último bloque de la zona de archivos.
     */
    public static String aTexto(int totalBloques, int cabezal, EntradaFS[] entradas) {
        JsonSistemaArchivos json = new JsonSistemaArchivos();
        json.diskConfig = new JsonSistemaArchivos.DiskConfig();
        json.diskConfig.totalBlocks = totalBloques;
        json.diskConfig.initialHead = Math.min(cabezal, totalBloques - 1);
        json.fileSystem = new JsonSistemaArchivos.Entrada[entradas.length];
        for (int i = 0; i < entradas.length; i++) {
            EntradaFS e = entradas[i];
            JsonSistemaArchivos.Entrada j = new JsonSistemaArchivos.Entrada();
            j.path = e.ruta();
            j.type = e.tipo().getNombreJson();
            j.owner = e.dueno();
            j.permissions = e.permiso().getNombreJson();
            if (e.tipo() == TipoNodo.ARCHIVO) {
                j.startBlock = e.primerBloque();
                j.blocks = e.bloques();
                j.chain = sonSeguidos(e.cadena()) ? null : e.cadena();   //"chain" solo si hace falta
            }
            json.fileSystem[i] = j;
        }
        return GSON.toJson(json);
    }

    private static boolean sonSeguidos(int[] cadena) {
        for (int i = 1; i < cadena.length; i++) {
            if (cadena[i] != cadena[i - 1] + 1) {
                return false;
            }
        }
        return true;
    }
}