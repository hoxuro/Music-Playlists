/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package listas.musicales;

import java.util.Objects;

/**
 * Clase que nos permite instanciar canciones.
 *
 * @author Heriberto Amezcua
 * @version 1.0
 * @since JDK 19
 */
public class Cancion {

    private String titulo, ruta;
    private int duracion;

    /**
     * Permite instanciar una cancion de titulo, ruta y duracion deseados.
     *
     * @param titulo el titulo de la cancion
     * @param ruta la ruta de la cancion
     * @param duracion la duracion de la cancion en segundos (si el usuario
     * introduce -1 significa que la duracion es desconocida)
     */
    public Cancion(String titulo, String ruta, int duracion) {
        if (titulo.isBlank()) {
            throw new IllegalArgumentException("El titulo no es valido");
        }
        if (ruta.isBlank()) {
            throw new IllegalArgumentException("La ruta esta no es valida");
        }
        if ((duracion <= 0) && duracion != -1) {
            throw new IllegalArgumentException("La duracion no es valida");
        }

        this.titulo = titulo;
        this.ruta = ruta;
        this.duracion = duracion;
    }

    /**
     * Permite instanciar una cancion de titulo y ruta introducidos por el
     * usuario y con una duracion desconocida.
     *
     * @param titulo el titulo de la cancion
     * @param ruta la ruta de la cancion
     */
    public Cancion(String titulo, String ruta) {
        this(titulo, ruta, -1);
    }

    /**
     * Constructor que crea una cancion con el mismo estado de la cancion
     * introducida por parametro.
     *
     * @param c la cancion cuyos atributos queremos copiar
     */
    public Cancion(Cancion c) {
        this(c.titulo, c.ruta, c.duracion);
    }

    /**
     *
     * @return el titulo de la cancion
     */
    public String getTitulo() {
        return titulo;
    }

    /**
     * Establece el titulo de la cancion.
     *
     * @param titulo el titulo que queremos establecer a la cancion.
     *
     * @throws IllegalArgumentException si el titulo de la cancion está vacío o
     * compuesto de caracteres en blanco.
     */
    public void setTitulo(String titulo) {
        if (titulo.isBlank()) {
            throw new IllegalArgumentException("El titulo no es valido");
        }
        this.titulo = titulo;
    }

    /**
     *
     * @return la ruta donde se encuentra la cancion.
     */
    public String getRuta() {
        return ruta;
    }

    /**
     * Establece la ruta de la cancion.
     *
     * @param ruta la ruta de la cancion que queremos establecer a la cancion.
     *
     * @throws IllegalArgumentException si la ruta de la cancion está vacío o
     * compuesto de caracteres en blanco.
     */
    public void setRuta(String ruta) {
        if (ruta.isBlank()) {
            throw new IllegalArgumentException("La ruta esta no es valida");
        }
        this.ruta = ruta;
    }

    /**
     *
     * @return la duracion de la cancion.
     */
    public int getDuracion() {
        return duracion;
    }

    /**
     * Establece la duracion de la cancion.
     *
     * @param duracion la duracion que queremos establecer a la cancion.
     *
     * @throws IllegalArgumentException si la duracion de la cancion es menor
     * que 0 y distinta de -1.
     */
    public void setDuracion(int duracion) {
        if (duracion <= 0 && duracion != -1) {
            throw new IllegalArgumentException("La duracion no es valida");
        }

        this.duracion = duracion;
    }

    /**
     * Devuelve una representacion en formato String de ese objeto Cancion.
     *
     * @return una representacion en formato String de ese objeto cancion
     */
    @Override
    public String toString() {
        return new StringBuilder("Titulo= ").append(titulo).append("; Ruta= ").append(ruta).append("; Duracion= ").append(duracion).append(" segundos").toString();
    }

    /**
     * Permite comprobar si dos objetos de la clase cancion son iguales. Son
     * iguales si coinciden en titulo y duracion.
     *
     * @param obj el objeto cancion que queremos comprobar si es igual al
     * primero.
     * @return true si son iguales, false si no.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Cancion other = (Cancion) obj;
        if (this.duracion != other.duracion) {
            return false;
        }
        if (!Objects.equals(this.titulo, other.titulo)) {
            return false;
        }
        return Objects.equals(this.ruta, other.ruta);
    }
}
