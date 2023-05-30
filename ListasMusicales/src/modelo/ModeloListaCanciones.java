/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package modelo;

import java.util.ArrayList;
import javax.swing.table.AbstractTableModel;
import listas.musicales.Cancion;

/**
 * Permite instanciar un modelo personalizado de canciones para una JTable.
 *
 * @author Heriberto Amezcua
 * @version 1.0
 * @since JDK 19
 */
public class ModeloListaCanciones extends AbstractTableModel {

    private ArrayList<Cancion> listaCanciones;
    private String[] columnas = {"Título", "Ruta", "Duración (en segundos)"};

    /**
     * Permite instanciar un objeto de la clase ModeloListaCanciones
     *
     */
    public ModeloListaCanciones() {
        this.listaCanciones = new ArrayList<>();
    }

    public void añadirCancion(Cancion c) {
        this.listaCanciones.add(new Cancion(c));
    }

    public void modificarCancion(int indice, Cancion c) {
        this.listaCanciones.set(indice, new Cancion(c));
    }

    public void eliminarCancion(Cancion c) {
        this.listaCanciones.remove(c);
    }

    public void vaciarModelo() {
        this.listaCanciones.clear();
    }

    public void eliminarCancion(int indice) {
        this.listaCanciones.remove(indice);
    }

    /**
     *
     * @return el numero de filas del modelo.
     */
    @Override
    public int getRowCount() {
        return this.listaCanciones.size();
    }

    /**
     *
     * @return el numero de columnas del modelo.
     */
    @Override
    public int getColumnCount() {
        return 3;
    }

    /**
     * Permite obtener el valor de una celda en la posicion de fila y columna
     * indicada.
     *
     * @param rowIndex el indice de la fila.
     * @param columnIndex el indice de la columna.
     * @return el valor de cuya posicion ha sido introducida o null si el valor
     * no concuerda con los indices de la tabla.
     */
    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        switch (columnIndex) {
            case 0 -> {
                return listaCanciones.get(rowIndex).getTitulo();
            }
            case 1 -> {
                return listaCanciones.get(rowIndex).getRuta();
            }
            case 2 -> {
                return listaCanciones.get(rowIndex).getDuracion();
            }
        }
        return null;
    }

    /**
     * Devuelve el nombre de la columna de la tabla.
     *
     * @param column el indice de la columna.
     * @return el nombre de la columna.
     */
    @Override
    public String getColumnName(int column) {
        return columnas[column];
    }
}
