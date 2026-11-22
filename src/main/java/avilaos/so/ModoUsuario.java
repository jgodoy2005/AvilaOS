/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Enum.java to edit this template
 */
package avilaos.so;

/**
 *
 * @author juandiegogodoy
 * Modo de uso de la interfaz: administrador (todas las operaciones) o usuario (acceso restringido).
 */
public enum ModoUsuario {
     ADMINISTRADOR("Administrador"),
    USUARIO("Usuario");

    //nombre de la cuenta fija del administrador (asi aparece como dueño en los JSON)
    public static final String CUENTA_ADMIN = "admin";

    private final String nombre;

    ModoUsuario(String nombre) {
        this.nombre = nombre;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
