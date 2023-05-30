/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package interfaz;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import listas.musicales.Cancion;
import modelo.ModeloListaCanciones;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Permite crear una ventana principal sobre la que se cimentará nuestra
 * interfaz gráfica para que el usuario pueda interactuar con el programa de
 * forma sencilla.
 *
 * @author Heriberto Amezcua
 * @version 1.0
 * @since JDK 19
 */
public class VentanaPrincipal extends javax.swing.JFrame {

    private ModeloListaCanciones modelo;
    private ArrayList<Cancion> listaCanciones;
    private Cancion cancionSeleccionada;
    private int[] filasSelec;

    /**
     * Crea un nuevo form VentanaPrincipal que es la primera venta que se abre
     * al ejecutar el programa y la principal.
     */
    public VentanaPrincipal() {
        initComponents();
        //Establezco los valores iniciales de la ventana principal
        inicializar();
        //establezco el modelo por defecto
        this.listaCanciones = new ArrayList<>();
        modelo = new ModeloListaCanciones();
        tableCanciones.setModel(modelo);
        int[] rows = tableCanciones.getSelectedRows();
        for (int i = 0; i < rows.length; i++) {
            rows[i] = tableCanciones.convertRowIndexToModel(rows[i]);
        }

    }

    /**
     * Nos permite alterar el estado de los atributos de un objeto cancion de la
     * tabla.
     */
    public void modificarCancion() {
        JDAñadir modificar = new JDAñadir(this, true, this.cancionSeleccionada);
        modificar.setVisible(true);

        if (modificar.getHaModificado()) {
            int indice = this.listaCanciones.indexOf(this.cancionSeleccionada);
            this.listaCanciones.set(indice, new Cancion(modificar.getNuevaCancion()));
            modelo.modificarCancion(indice, modificar.getNuevaCancion());
            JOptionPane.showMessageDialog(rootPane, "La canción se ha modificado correctamente");
            modelo.fireTableDataChanged();
        }
    }

    /**
     * A traves de un fichero M3U nos permite extraer las canciones de este e
     * introducirlas en un ArrayList.
     *
     * @param ruta la ruta del fichero M3U.
     * @return true si se ha cargado el arrayList con canciones a traves de un
     * fichero m3u.
     */
    private boolean cargarM3u(String ruta) {
        boolean haCargado = false;
        ArrayList<String> cancionesFichero = new ArrayList<>();

        File fileDatos = new File(ruta);

        if (fileDatos.exists()) {
            try {
                String strContenido = Files.readString(fileDatos.toPath());
                //de ese string con el contenido del fichero extraigo las canciones

                //hice el regex, fallaba y fallaba cuando el validador me lo daba por bueno. 
                //despues de mas de 2 horas buscando el problema he conseguido encontrarlo
                //en windows se usa CRLF para indicar el fin de una linea, mientras que en 
                //linux se usa LF por lo que el problema era de los ficheros
                Pattern patronCompleto = Pattern.compile("^(\n?+)*#EXTM3U\n(((#EXTINF:(-?[0-9]+),(.+)\n(.+)\n)+)?)#EXTINF:(-?[0-9]+),(.+)\n(.+)$(\n?+)*");
                Matcher matchM3U = patronCompleto.matcher(strContenido);
                if (matchM3U.matches()) {
                    Pattern patronCancion = Pattern.compile("#EXTINF:(-?[0-9]+),(.+)\n(.+)");
                    Matcher matcherCancion = patronCancion.matcher(strContenido);
                    while (matcherCancion.find()) {
                        cancionesFichero.add(matcherCancion.group());
                    }

                    //elimino el contenido que haya hasta el momento en el arraylist
                    this.listaCanciones.clear();
                    modelo.vaciarModelo();
                    //por cada cancion en formato m3u creo un objeto cancion
                    for (String cancion : cancionesFichero) {
                        Matcher matcher = patronCancion.matcher(cancion);
                        if (matcher.matches()) {
                            this.listaCanciones.add(new Cancion(matcher.group(2).replaceFirst(",", ""),
                                    matcher.group(3).replaceFirst("\n", ""),
                                    Integer.parseInt(matcher.group(1))));
                            modelo.añadirCancion(new Cancion(matcher.group(2).replaceFirst(",", ""),
                                    matcher.group(3).replaceFirst("\n", ""),
                                    Integer.parseInt(matcher.group(1))));
                        }
                    }
                    haCargado = true;
                }

            } catch (IOException ex) {
                Logger.getLogger(VentanaPrincipal.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return haCargado;
    }

    /**
     * Permite extraer de un fichero fichero en formato PLS las canciones de
     * este y "cargarlas" en nuestro actual ArrayList.
     *
     * @param ruta la ruta del fichero PLS en el ordenador del usuario
     * @return true si el fichero estaba en el formato PLS y se ha podido
     * introducir al menos una cancion
     */
    private boolean cargarPls(String ruta) {
        boolean haCargado = false;
        File fileDatos = new File(ruta);

        if (fileDatos.exists()) {
            try {

                String strContenido = Files.readString(fileDatos.toPath());
                Pattern patron = Pattern.compile("^\\[playlist\\]\n(File[0-9]+=.+\nTitle[0-9]+=.+\nLength[0-9]+=([0-9]+|-1)\n)+NumberOfEntries=[1-9]+\nVersion=2$");
                Matcher matchPLS = patron.matcher(strContenido);

                //compruebo que el fichero concuerde con el formato PLS de forma general
                if (matchPLS.matches()) {
                    //creo un patron para validar cada una de las canciones del fichero PLS
                    Pattern patronCancion = Pattern.compile("File[0-9]+=.+\nTitle[0-9]+=.+\nLength[0-9]+=([0-9]+|-1)\n");
                    Matcher matcherCancion = patronCancion.matcher(strContenido);
                    String numEntradas = extraerValor(strContenido, "NumberOfEntries=[0-9]+").replace("NumberOfEntries=", "");

                    //compruebo que cada numero de cancion concuerda en titulo, ruta y longitud
                    boolean numValidos = true;
                    String nFile = "",
                            nTitle = "",
                            nLength = "";
                    while (matcherCancion.find() && numValidos) {
                        nFile = extraerValor(matcherCancion.group(), "File[0-9]+").replace("File", "");
                        nTitle = extraerValor(matcherCancion.group(), "Title[0-9]+").replace("Title", "");
                        nLength = extraerValor(matcherCancion.group(), "Length[0-9]+").replace("Length", "");

                        numValidos = (nFile.equals(nTitle) && nFile.equals(nLength));
                    }

                    //elimino el contenido que haya hasta el momento en el arraylist
                    this.listaCanciones.clear();
                    modelo.vaciarModelo();

                    //si todos los numeros concuerdan y ademas tambien lo hacen con el numero de entradas
                    //finalmente se que el archivo esta correctamente formado y puedo continuar
                    if (numValidos && nFile.equals(numEntradas)) {
                        matcherCancion = patronCancion.matcher(strContenido);

                        while (matcherCancion.find()) {
                            String path = extraerValor(matcherCancion.group(), "File[0-9]+=.+").replaceFirst("File[0-9]=", "");
                            String titulo = extraerValor(matcherCancion.group(), "Title[0-9]+=.+").replaceFirst("Title[0-9]=", "");
                            String duracion = extraerValor(matcherCancion.group(), "Length[0-9]+=.+").replaceFirst("Length[0-9]=", "");
                            this.listaCanciones.add(new Cancion(titulo, path, Integer.parseInt(duracion)));
                            modelo.añadirCancion(new Cancion(titulo, path, Integer.parseInt(duracion)));
                        }

                    }
                    haCargado = true;
                }

            } catch (IOException ex) {
                Logger.getLogger(VentanaPrincipal.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
        return haCargado;
    }

    /**
     * Permite la creacion de objetos Cancion cuyos datos estan contenidos en un
     * archivo en formato xspf.
     *
     * @param ruta la ruta del archivo xml en el pc del usuario
     * @return true si se ha podido cargar al menos una cancion del fichero
     */
    private boolean cargarXspf(String ruta) {
        boolean haCargado = false;
        try {
            //primero paso de xml a dom
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document documento = db.parse(ruta);

            //extraigo todos los elementos tag del xml
            NodeList listaNodos = documento.getElementsByTagName("track");

            //elimino el contenido que haya hasta el momento en el arraylist
            this.listaCanciones.clear();
            modelo.vaciarModelo();

            boolean sePuedeAñadir = true;
            for (int i = 0; i < listaNodos.getLength() && sePuedeAñadir; i++) {
                Element track = (Element) listaNodos.item(i);
                sePuedeAñadir = JDAñadir.esNumerico(track.getElementsByTagName("duration").item(0).getTextContent());
            }

            if (sePuedeAñadir) {
                //elimino el contenido que haya hasta el momento en el arraylist
                this.listaCanciones.clear();
                for (int i = 0; i < listaNodos.getLength(); i++) {
                    Element track = (Element) listaNodos.item(i);
                    this.listaCanciones.add(new Cancion(track.getElementsByTagName("title").item(0).getTextContent(),
                            track.getElementsByTagName("location").item(0).getTextContent(),
                            (Integer.parseInt(track.getElementsByTagName("duration").item(0).getTextContent()) / 1000)));
                    modelo.añadirCancion(new Cancion(track.getElementsByTagName("title").item(0).getTextContent(),
                            track.getElementsByTagName("location").item(0).getTextContent(),
                            (Integer.parseInt(track.getElementsByTagName("duration").item(0).getTextContent()) / 1000)));

                }

                haCargado = true;
            }
        } catch (SAXException | IOException | ParserConfigurationException ex) {
            Logger.getLogger(VentanaPrincipal.class.getName()).log(Level.SEVERE, null, ex);
        }
        return haCargado;
    }

    /**
     * A traves de una cadena de caracteres y una expresion regular introducidos
     * ambos por el usuario, extrae el valor del primer grupo de caracteres que
     * concuerden con esa expresion regular.
     *
     * @param texto el texto donde queremos buscar
     * @param regex la expresion regular que queremos comprobar que se encuentra
     * en el texto
     * @return el primer grupo de caracteres dentro del texto que concuerdan con
     * la expresion regular o null si no concuerda
     */
    private String extraerValor(String texto, String regex) {
        Pattern patron = Pattern.compile(regex);
        Matcher matcher = patron.matcher(texto);
        return matcher.find() ? matcher.group() : null;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        pMenu = new javax.swing.JPopupMenu();
        pItemModificar = new javax.swing.JMenuItem();
        pItemEliminar = new javax.swing.JMenuItem();
        pMenuEliminar = new javax.swing.JPopupMenu();
        mItemEliminarFilas = new javax.swing.JMenuItem();
        jScrollPane1 = new javax.swing.JScrollPane();
        tableCanciones = new javax.swing.JTable();
        btnCargar = new javax.swing.JButton();
        btnGuardar = new javax.swing.JButton();
        btnNueva = new javax.swing.JButton();
        mBarPrincipal = new javax.swing.JMenuBar();
        mArchivo = new javax.swing.JMenu();
        mItemNueva = new javax.swing.JMenuItem();
        mItemCargar = new javax.swing.JMenuItem();
        mItemGuardar = new javax.swing.JMenuItem();
        mEditar = new javax.swing.JMenu();
        mItemAñadir = new javax.swing.JMenuItem();
        mItemModificar = new javax.swing.JMenuItem();
        mItemEliminar = new javax.swing.JMenuItem();

        pItemModificar.setText("Modificar");
        pItemModificar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pItemModificarActionPerformed(evt);
            }
        });
        pMenu.add(pItemModificar);

        pItemEliminar.setText("Eliminar");
        pItemEliminar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pItemEliminarActionPerformed(evt);
            }
        });
        pMenu.add(pItemEliminar);

        mItemEliminarFilas.setText("Eliminar Filas");
        mItemEliminarFilas.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mItemEliminarFilasActionPerformed(evt);
            }
        });
        pMenuEliminar.add(mItemEliminarFilas);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);

        tableCanciones.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        tableCanciones.setRowHeight(30);
        tableCanciones.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                tableCancionesMousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                tableCancionesMouseReleased(evt);
            }
        });
        jScrollPane1.setViewportView(tableCanciones);

        btnCargar.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        btnCargar.setText("Cargar Lista");
        btnCargar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCargarActionPerformed(evt);
            }
        });
        btnCargar.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                btnCargarKeyPressed(evt);
            }
        });

        btnGuardar.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        btnGuardar.setText("Guardar Lista");
        btnGuardar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGuardarActionPerformed(evt);
            }
        });
        btnGuardar.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                btnGuardarKeyPressed(evt);
            }
        });

        btnNueva.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        btnNueva.setText("Nueva Lista");
        btnNueva.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnNuevaActionPerformed(evt);
            }
        });
        btnNueva.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                btnNuevaKeyPressed(evt);
            }
        });

        mArchivo.setMnemonic('a');
        mArchivo.setText("Archivo");

        mItemNueva.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        mItemNueva.setMnemonic('n');
        mItemNueva.setText("Nueva Lista");
        mItemNueva.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mItemNuevaActionPerformed(evt);
            }
        });
        mArchivo.add(mItemNueva);

        mItemCargar.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_L, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        mItemCargar.setMnemonic('c');
        mItemCargar.setText("Cargar Lista");
        mItemCargar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mItemCargarActionPerformed(evt);
            }
        });
        mArchivo.add(mItemCargar);

        mItemGuardar.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        mItemGuardar.setMnemonic('g');
        mItemGuardar.setText("Guardar Lista");
        mItemGuardar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mItemGuardarActionPerformed(evt);
            }
        });
        mArchivo.add(mItemGuardar);

        mBarPrincipal.add(mArchivo);

        mEditar.setMnemonic('e');
        mEditar.setText("Editar");

        mItemAñadir.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_E, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        mItemAñadir.setMnemonic('a');
        mItemAñadir.setText("Añadir Canción");
        mItemAñadir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mItemAñadirActionPerformed(evt);
            }
        });
        mEditar.add(mItemAñadir);

        mItemModificar.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_M, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        mItemModificar.setMnemonic('m');
        mItemModificar.setText("Modificar Canción");
        mItemModificar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mItemModificarActionPerformed(evt);
            }
        });
        mEditar.add(mItemModificar);

        mItemEliminar.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_D, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        mItemEliminar.setMnemonic('e');
        mItemEliminar.setText("Eliminar Canción");
        mItemEliminar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mItemEliminarActionPerformed(evt);
            }
        });
        mEditar.add(mItemEliminar);

        mBarPrincipal.add(mEditar);

        setJMenuBar(mBarPrincipal);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(67, 67, 67)
                .addComponent(btnNueva, javax.swing.GroupLayout.PREFERRED_SIZE, 224, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(131, 131, 131)
                .addComponent(btnCargar, javax.swing.GroupLayout.PREFERRED_SIZE, 224, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 131, Short.MAX_VALUE)
                .addComponent(btnGuardar, javax.swing.GroupLayout.PREFERRED_SIZE, 224, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(56, 56, 56))
            .addComponent(jScrollPane1)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 369, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 18, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnNueva, javax.swing.GroupLayout.PREFERRED_SIZE, 91, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnCargar, javax.swing.GroupLayout.PREFERRED_SIZE, 91, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnGuardar, javax.swing.GroupLayout.PREFERRED_SIZE, 91, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(22, 22, 22))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Permite modificar una cancion abriendo un JDialog.
     *
     * @param evt
     */
    private void pItemModificarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pItemModificarActionPerformed
        modificarCancion();
    }//GEN-LAST:event_pItemModificarActionPerformed

    /**
     * Nos permite validar la posicion de la cancion en la tabla introducida por
     * el usuario.
     *
     * @param input la posicion de la cancion en la tabla.
     * @return true si la posicion es mayor que cero y menor o igual que el
     * tamaño del arrayList de canciones del modelo de la tabla.
     */
    private boolean validarPosicion(String input) {
        boolean esValida = false;
        if (input != null) {
            try {
                int posicion = Integer.parseInt(input);
                esValida = !(posicion <= 0 || posicion > this.listaCanciones.size());
            } catch (NumberFormatException e) {

            }
        }
        return esValida;
    }

    /**
     * Permite eliminar una cancion de la tabla al hacer click izquierdo en el
     * menu item eliminar del JPopMenu.
     *
     * @param evt click izquierzo
     */
    private void pItemEliminarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pItemEliminarActionPerformed
        if (!this.listaCanciones.isEmpty()) {
            eliminarCancion();
        }
    }//GEN-LAST:event_pItemEliminarActionPerformed

    /**
     * Permite al usuario crear una nueva lista de canciones desde cero.
     *
     * @param evt click izquierdo sobre btnNueva
     */
    private void btnNuevaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNuevaActionPerformed
        crearNuevaLista();
    }//GEN-LAST:event_btnNuevaActionPerformed

    /**
     * Permite al usuario guardar las canciones contenidas en el actual
     * ArrayList de canciones en ficheros con formato m3u, pls o xspf.
     *
     * @param evt click derecho sobre btnGuardar
     */
    private void btnGuardarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGuardarActionPerformed
        if (!this.listaCanciones.isEmpty()) {
            guardarLista();
        }
    }//GEN-LAST:event_btnGuardarActionPerformed

    /**
     * Permite cargar las canciones contenidas en ficheros m3u, pls y xspf en un
     * arrayList de canciones.
     *
     * @param evt click izquierdo en btnCargar
     */
    private void btnCargarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCargarActionPerformed
        if (!cargarLista()) {
            JOptionPane.showMessageDialog(rootPane, "Fichero no válido. No se han podido cargar los datos", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnCargarActionPerformed

    /**
     * Permite abrir un JPopMenu con diferentes opciones al hacer click derecho
     * o modificar una cancion mediante un JDialog al hacer doble click
     * izquierdo sobre una cancion de la tabla.
     *
     * @param evt doble click izquierdo o click derecho.
     */
    private void tableCancionesMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableCancionesMousePressed

        int row = tableCanciones.rowAtPoint(evt.getPoint());
        this.cancionSeleccionada = this.listaCanciones.get(row);

        //al hacer doble click sobre una cancion se abrira la opcion de modificar
        if (evt.getClickCount() == 2 && !evt.isConsumed()) {
            evt.consume();
            modificarCancion();
        }

        //al hacer click derecho sobre una cancion se abrira un jpopupmenu
        if (evt.getButton() == MouseEvent.BUTTON3) {
            pMenu.setVisible(true);
            int column = tableCanciones.columnAtPoint(evt.getPoint());
            pMenu.show(tableCanciones, evt.getPoint().x, evt.getPoint().y);
            tableCanciones.setRowSelectionInterval(row, row);
            tableCanciones.setColumnSelectionInterval(column, column);
        }
    }//GEN-LAST:event_tableCancionesMousePressed

    /**
     * Permite eliminar una cancion itroduciendo la posicion de esta en la
     * tabla.
     *
     * @param evt click sobre el menu item o CTRL+D
     */
    private void mItemEliminarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mItemEliminarActionPerformed
        if (!this.listaCanciones.isEmpty()) {
            eliminarCancion();
        }
    }//GEN-LAST:event_mItemEliminarActionPerformed

    public void eliminarCancion() {
        if (this.cancionSeleccionada != null) {
            this.listaCanciones.remove(this.cancionSeleccionada);
            modelo.eliminarCancion(this.cancionSeleccionada);
            modelo.fireTableDataChanged();
        } else {
            JOptionPane.showMessageDialog(rootPane, "Error, seleccione una canción para poder eliminarla", "ERROR", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Permite modificar una cancion al pulsar sobre el menu item de la barra
     * principal o pulsando CTRL + M.
     *
     * @param evt click sobre el menu item o CTRL+M
     */
    private void mItemModificarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mItemModificarActionPerformed
        if (!this.listaCanciones.isEmpty()) {
            if (this.cancionSeleccionada != null) {
                modificarCancion();

            } else {
                JOptionPane.showMessageDialog(rootPane, "Error, seleccione una canción antes de modificar", "ERROR", JOptionPane.ERROR_MESSAGE);
            }
        }
    }//GEN-LAST:event_mItemModificarActionPerformed

    /**
     * Permite añadir una canción al ArrayList de canciones a traves de un
     * JDialog.
     *
     * @param evt hacer click o CTRL+E
     */
    private void mItemAñadirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mItemAñadirActionPerformed
        JDAñadir añadir = new JDAñadir(this, true);
        añadir.setVisible(true);

        if (añadir.getHaAñadido()) {
            this.listaCanciones.add(new Cancion(añadir.getNuevaCancion()));
            modelo.añadirCancion(añadir.getNuevaCancion());
            JOptionPane.showMessageDialog(rootPane, "La canción se ha añadido correctamente");
            modelo.fireTableDataChanged();
        }
    }//GEN-LAST:event_mItemAñadirActionPerformed

    /**
     * Permite guardar las canciones contenidas en el ArrayList de canciones en
     * formato M3U, PLS O XSPF.
     *
     * @param evt click izquierdo sobre mItemGuardar o CTRL+S
     */
    private void mItemGuardarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mItemGuardarActionPerformed
        if (!this.listaCanciones.isEmpty()) {
            guardarLista();
        }
    }//GEN-LAST:event_mItemGuardarActionPerformed

    /**
     * Permite cargar las canciones contenidas en ficheros m3u, pls y xspf en un
     * arrayList de canciones.
     *
     * @param evt click izquierdo sobre btnCargar
     */
    private void mItemCargarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mItemCargarActionPerformed
        cargarLista();
    }//GEN-LAST:event_mItemCargarActionPerformed

    /**
     * Permite al usuario crear una nueva lista de canciones desde cero.
     *
     * @param evt click izquierdo sobre mItemNueva o CTRL+L
     */
    private void mItemNuevaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mItemNuevaActionPerformed
        crearNuevaLista();
    }//GEN-LAST:event_mItemNuevaActionPerformed

    private void mItemEliminarFilasActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mItemEliminarFilasActionPerformed
        for (int i = this.filasSelec.length - 1; i >= 0; i--) {
            this.listaCanciones.remove(this.filasSelec[i]);
            modelo.eliminarCancion(this.filasSelec[i]);
        }
        modelo.fireTableDataChanged();
    }//GEN-LAST:event_mItemEliminarFilasActionPerformed

    private void tableCancionesMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableCancionesMouseReleased

        int[] rows = tableCanciones.getSelectedRows();
        for (int i = 0; i < rows.length; i++) {
            //en el array filas seleccionadas, introduzco los indices
            rows[i] = tableCanciones.convertRowIndexToModel(rows[i]);
        }

        if (rows.length > 1) {
            this.filasSelec = rows;
            pMenuEliminar.setVisible(true);
            pMenuEliminar.show(tableCanciones, evt.getPoint().x, evt.getPoint().y);
        }

    }//GEN-LAST:event_tableCancionesMouseReleased

    private void btnNuevaKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_btnNuevaKeyPressed
        if (btnNueva.isFocusOwner()) {
            btnNueva.doClick();
        }
    }//GEN-LAST:event_btnNuevaKeyPressed

    private void btnCargarKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_btnCargarKeyPressed
        if (btnCargar.isFocusOwner()) {
            btnCargar.doClick();
        }
    }//GEN-LAST:event_btnCargarKeyPressed

    private void btnGuardarKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_btnGuardarKeyPressed
        if (btnGuardar.isFocusOwner()) {
            btnGuardar.doClick();
        }
    }//GEN-LAST:event_btnGuardarKeyPressed

    /**
     * Permite cargar las canciones contenidas en ficheros m3u, pls y xspf en un
     * arrayList de canciones.
     */
    private boolean cargarLista() {
        boolean seHaCargado = false;
        JFileChooser elegirArchivo = new JFileChooser();

        String[][] filtros = {{"M3U", "m3u"}, {"PLS", "pls"}, {"XSPF", "xspf"}};
        for (String[] filtro : filtros) {
            FileNameExtensionFilter extension = new FileNameExtensionFilter(filtro[0], filtro[1]);
            elegirArchivo.addChoosableFileFilter(extension);
        }

        int seleccion = elegirArchivo.showOpenDialog(rootPane);

        if (seleccion == JFileChooser.APPROVE_OPTION) {

            File f = elegirArchivo.getSelectedFile();
            StringBuilder path = new StringBuilder();
            if (f.exists()) {
                path.append(f.getAbsolutePath());
            }

            //obtengo la extension para realizar una operacion especifica dependiendo del formato
            String extension = f.getName().substring(f.getName().lastIndexOf(".") + 1);
            switch (extension.toUpperCase()) {
                case "M3U" -> {
                    seHaCargado = cargarM3u(path.toString());
                }
                case "PLS" -> {
                    seHaCargado = cargarPls(path.toString());
                }
                case "XSPF" -> {
                    seHaCargado = cargarXspf(path.toString());

                }
            }
        } else {
            seHaCargado = true;
        }

        modelo.fireTableDataChanged();

        return seHaCargado;
    }

    /**
     * Permite guardar las canciones contenidas en el ArrayList de canciones en
     * formato M3U, PLS O XSPF.
     *
     * @return true si el fichero se ha guardado con exito
     */
    private boolean guardarLista() {
        boolean seHaGuardado = false;
        JFileChooser guardarArchivo = new JFileChooser();
        //valores por defecto para que parezca un save dialog
        guardarArchivo.setAcceptAllFileFilterUsed(false);
        guardarArchivo.setDialogTitle("Guardar Lista de Reproducción");
        guardarArchivo.setApproveButtonText("Guardar");
        //establezco los filtros de extensiones
        String[][] filtros = {{"M3U", "m3u"}, {"PLS", "pls"}, {"XSPF", "xspf"}};
        for (String[] filtro : filtros) {
            FileNameExtensionFilter extension = new FileNameExtensionFilter(filtro[0], filtro[1]);
            guardarArchivo.addChoosableFileFilter(extension);
        }

        int seleccion = guardarArchivo.showOpenDialog(rootPane);
        //escribo el contenido en formato m3u
        if (seleccion == JFileChooser.APPROVE_OPTION) {
            File f = guardarArchivo.getSelectedFile();
            String extension = guardarArchivo.getFileFilter().getDescription();

            switch (extension) {
                case "M3U" -> {
                    if (!guardarM3u(f)) {
                        JOptionPane.showMessageDialog(rootPane, "No se ha podido guardar", "ERROR", JOptionPane.ERROR_MESSAGE);
                    } else {
                        seHaGuardado = true;
                    }
                }
                case "PLS" -> {
                    if (!guardarPls(f)) {
                        JOptionPane.showMessageDialog(rootPane, "No se ha podido guardar", "ERROR", JOptionPane.ERROR_MESSAGE);
                    } else {
                        seHaGuardado = true;
                    }
                }
                case "XSPF" -> {
                    if (!guardarXspf(f)) {
                        JOptionPane.showMessageDialog(rootPane, "No se ha podido guardar", "ERROR", JOptionPane.ERROR_MESSAGE);
                    } else {
                        seHaGuardado = true;
                    }

                }
            }
        }

        return seHaGuardado;
    }

    /**
     * Permite guardar el estado de los objetos Cancion contenidas en un
     * ArrayList de canciones en un fichero en formato M3U, añadiendole la
     * extension .m3u
     *
     * @param f el fichero donde queremos guardar los datos
     * @return true si no ha saltado ninguna excepcion a la hora de escribir en
     * el fichero con el BufferedWritter
     */
    private boolean guardarM3u(File f) {
        boolean seHaGuardado = false;

        StringBuilder guardarM3U = new StringBuilder("#EXTM3U\n");
        for (Cancion c : this.listaCanciones) {
            guardarM3U.append("#EXTINF:").append(c.getDuracion()).append(",").append(c.getTitulo()).append("\n");
            guardarM3U.append(c.getRuta()).append("\n");

        }

        if (!f.getName().endsWith(".m3u")) {
            f = new File(f.getAbsolutePath() + ".m3u");
        }

        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(f));
            //pongo un trim para que no haya saltos de linea al incio o al final
            bw.write(guardarM3U.toString().trim());
            bw.flush();
            bw.close();
            seHaGuardado = true;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(rootPane, "Error al guardar el archivo", "Error", JOptionPane.ERROR_MESSAGE);
        }

        return seHaGuardado;
    }

    /**
     * Permite guardar el estado de los objetos Cancion contenidas en un
     * ArrayList de canciones en un fichero en formato PLS, añadiendole la
     * extension .pls
     *
     * @param f el fichero donde queremos guardar los datos
     * @return true si no ha saltado ninguna excepcion a la hora de escribir en
     * el fichero con el BufferedWritter
     */
    private boolean guardarPls(File f) {
        boolean seHaGuardado = false;

        StringBuilder fileGuardar = new StringBuilder("[playlist]\n");
        int contador = 1;
        for (Cancion c : this.listaCanciones) {
            fileGuardar.append("File").append(contador).append("=").append(c.getRuta()).append("\n");
            fileGuardar.append("Title").append(contador).append("=").append(c.getTitulo()).append("\n");
            fileGuardar.append("Length").append(contador).append("=").append(c.getDuracion()).append("\n");
            contador++;
        }
        fileGuardar.append("NumberOfEntries=").append(contador - 1).append("\n");
        fileGuardar.append("Version=2");

        if (!f.getName().endsWith(".pls")) {
            f = new File(f.getAbsolutePath() + ".pls");
        }

        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(f));
            //pongo un trim para que no haya saltos de linea al incio o al final
            bw.write(fileGuardar.toString().trim());
            bw.flush();
            bw.close();
            seHaGuardado = true;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(rootPane, "Error al guardar el archivo", "Error", JOptionPane.ERROR_MESSAGE);
        }

        return seHaGuardado;
    }

    /**
     * Permite guardar el estado de los objetos Cancion contenidas en un
     * ArrayList de canciones en un fichero en formato XSPF, añadiendole la
     * extension .xspf
     *
     * @param f el fichero donde queremos guardar los datos
     * @return true si no ha saltado ninguna excepcion a la hora de escribir en
     * el fichero
     */
    private boolean guardarXspf(File f) {
        boolean seHaGuardado = false;

        try {
            //creo un nuevo documento
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document documento = db.newDocument();

            //creo el elemento raiz del xml
            Element raiz = documento.createElement("playlist");
            //creo los atributos del raiz
            raiz.setAttribute("version", "1");
            raiz.setAttribute("xmlns", "http://xspf.org/ns/0/");
            //lo añado al documento
            documento.appendChild(raiz);

            Element trackList = documento.createElement("trackList");
            raiz.appendChild(trackList);

            //por cada cancion de la lista
            for (int i = 0; i < this.listaCanciones.size(); i++) {
                //añado un track
                trackList.appendChild(crearTrack(documento, this.listaCanciones.get(i), i));
            }

            //una vez creado el dom definitivo lo tranformo a xml y lo guardo en un fichero
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(documento);
            //establezco la extension xspf
            if (!f.getName().endsWith(".xspf")) {
                f = new File(f.getAbsolutePath() + ".xspf");
            }
            //finalmente creo el archivo xspf
            StreamResult result = new StreamResult(f);
            transformer.transform(source, result);
            seHaGuardado = true;
        } catch (ParserConfigurationException | TransformerException ex) {
            Logger.getLogger(VentanaPrincipal.class.getName()).log(Level.SEVERE, null, ex);
        }

        return seHaGuardado;
    }

    /**
     * Permite crear un elemento track para el xml con los valores de la cancion
     * introducida.
     *
     * @param doc el documento donde vamos a añadir el track
     * @param c la cancion cuyos valores queremos establecer al track
     * @param indice el indice de la cancion en nuestra lista de canciones
     * @return un elemento track
     */
    private Element crearTrack(Document doc, Cancion c, int indice) {
        Element track = doc.createElement("track");
        Element title = doc.createElement("title");
        title.appendChild(doc.createTextNode(this.listaCanciones.get(indice).getTitulo()));
        Element location = doc.createElement("location");
        location.appendChild(doc.createTextNode(this.listaCanciones.get(indice).getRuta()));
        Element duration = doc.createElement("duration");
        duration.appendChild(doc.createTextNode((this.listaCanciones.get(indice).getDuracion() * 1000) + ""));
        track.appendChild(title);
        track.appendChild(location);
        track.appendChild(duration);

        return track;
    }

    /**
     * Funcion que vacia los datos contenidos en nuestro arraylist de canciones
     * preguntando previamente si quiere guardarlos en caso de que no este
     * vacio.
     *
     * @return true si nuestro arrayList no esta vacio y el usuario ha
     * seleccionado guardar o no guardar los datos.
     */
    private boolean crearNuevaLista() {
        boolean seHaCreado = false;
        if (!this.listaCanciones.isEmpty()) {
            int input = JOptionPane.showConfirmDialog(rootPane, "¿Desea guardar los cambios antes de crear una nueva lista?");

            switch (input) {
                case JOptionPane.YES_OPTION -> {
                    if (guardarLista()) {
                        this.listaCanciones.clear();
                        modelo.vaciarModelo();
                        modelo.fireTableDataChanged();
                        seHaCreado = true;
                    }
                }
                case JOptionPane.NO_OPTION -> {
                    this.listaCanciones.clear();
                    modelo.vaciarModelo();
                    modelo.fireTableDataChanged();
                    seHaCreado = true;
                }
            }
        }

        return seHaCreado;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(VentanaPrincipal.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(VentanaPrincipal.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(VentanaPrincipal.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(VentanaPrincipal.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new VentanaPrincipal().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCargar;
    private javax.swing.JButton btnGuardar;
    private javax.swing.JButton btnNueva;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JMenu mArchivo;
    private javax.swing.JMenuBar mBarPrincipal;
    private javax.swing.JMenu mEditar;
    private javax.swing.JMenuItem mItemAñadir;
    private javax.swing.JMenuItem mItemCargar;
    private javax.swing.JMenuItem mItemEliminar;
    private javax.swing.JMenuItem mItemEliminarFilas;
    private javax.swing.JMenuItem mItemGuardar;
    private javax.swing.JMenuItem mItemModificar;
    private javax.swing.JMenuItem mItemNueva;
    private javax.swing.JMenuItem pItemEliminar;
    private javax.swing.JMenuItem pItemModificar;
    private javax.swing.JPopupMenu pMenu;
    private javax.swing.JPopupMenu pMenuEliminar;
    private javax.swing.JTable tableCanciones;
    // End of variables declaration//GEN-END:variables

    /**
     * Nos permite inicializar los valores por defecto que queremos que tenga el
     * JFrame al iniciar el programa.
     */
    private void inicializar() {
        setLocationRelativeTo(null);
        ImageIcon mainIcon = new ImageIcon("iconos/icon-btnPlay.png");
        setIconImage(mainIcon.getImage());
        setTitle("Listas Musicales");
        tableCanciones.getTableHeader().setPreferredSize(new Dimension(0, 40));
        tableCanciones.getTableHeader().setFont(new Font("Seoe UI", Font.BOLD, 14));
        //Establezco iconos de los menu Item
        mItemCargar.setIcon(crearIcono("iconos/folder.png", 18, 18));
        mItemNueva.setIcon(crearIcono("iconos/add-file.png", 18, 18));
        mItemGuardar.setIcon(crearIcono("iconos/save.png", 18, 18));
        mItemAñadir.setIcon(crearIcono("iconos/cd-player.png", 18, 18));
        mItemEliminar.setIcon(crearIcono("iconos/delete.png", 18, 18));
        mItemModificar.setIcon(crearIcono("iconos/edit.png", 18, 18));
    }

    /**
     * Metodo que nos permite crear un ImageIcon.
     *
     * @param ruta la ruta de la imagen del icono
     * @param anchura la anchura que queremos para el icono
     * @param altura la altura que queremos para el icono
     * @return un objeto ImageIcon
     */
    private ImageIcon crearIcono(String ruta, int anchura, int altura) {
        Image img = new ImageIcon(ruta).getImage().getScaledInstance(anchura, altura, Image.SCALE_DEFAULT);
        return new ImageIcon(img);
    }

}
