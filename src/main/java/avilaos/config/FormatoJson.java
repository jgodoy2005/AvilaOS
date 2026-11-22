/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.config;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;

/**
 *
 * @author juandiegogodoy
 * Reglas de tipos que comparten los dos archivos JSON de AvilaOS v2 
 */
final class FormatoJson {
   
    private FormatoJson() {
    }

    static GsonBuilder lector() {
        TypeAdapter<Integer> entero = new TypeAdapter<Integer>() {
            @Override
            public Integer read(JsonReader entrada) throws IOException {
                String campo = entrada.getPath();
                if (entrada.peek() != JsonToken.NUMBER) {
                    throw new JsonSyntaxException(campo + " debe ser un número entero sin comillas");
                }
                try {
                    return Integer.valueOf(entrada.nextString());
                } catch (NumberFormatException e) {
                    throw new JsonSyntaxException(campo + " debe ser un número entero válido", e);
                }
            }

            @Override
            public void write(JsonWriter salida, Integer valor) throws IOException {
                salida.value(valor);
            }
        }.nullSafe();

        TypeAdapter<String> texto = new TypeAdapter<String>() {
            @Override
            public String read(JsonReader entrada) throws IOException {
                if (entrada.peek() != JsonToken.STRING) {
                    throw new JsonSyntaxException(entrada.getPath() + " debe ser texto entre comillas");
                }
                return entrada.nextString();
            }

            @Override
            public void write(JsonWriter salida, String valor) throws IOException {
                salida.value(valor);
            }
        }.nullSafe();

        return new GsonBuilder().registerTypeAdapter(Integer.class, entero)
                .registerTypeAdapter(int.class, entero)
                .registerTypeAdapter(String.class, texto);
    } 
}
