package com.compi2.contacto.interfaz;

import com.compi2.contacto.compilacion.CompiladorProyecto;
import com.compi2.contacto.compilacion.ResultadoCompilacion;
import com.compi2.contacto.editor.EditorCodigo;
import com.compi2.contacto.proyecto.ArchivoFuente;
import com.compi2.contacto.proyecto.GestorProyecto;
import com.compi2.contacto.proyecto.LenguajeFuente;
import com.compi2.contacto.proyecto.ProyectoCompilacion;

import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class VentanaPrincipal extends JFrame {

    private final GestorProyecto gestorProyecto;
    private final CompiladorProyecto compilador;
    private final JTree arbolProyecto;
    private final JTabbedPane pestanas;
    private final ModeloTablaDiagnosticos modeloDiagnosticos;
    private final JLabel estado;
    private Path raizProyecto;

    public VentanaPrincipal() {
        super("Contacto 3xtrat3rr3str3D");

        gestorProyecto = new GestorProyecto();
        compilador = new CompiladorProyecto();
        pestanas = new JTabbedPane();
        modeloDiagnosticos = new ModeloTablaDiagnosticos();
        estado = new JLabel("Abra una carpeta de proyecto para comenzar");

        DefaultMutableTreeNode raizInicial = new DefaultMutableTreeNode(
                "Sin proyecto"
        );
        arbolProyecto = new JTree(new DefaultTreeModel(raizInicial));

        configurarVentana();
        construirInterfaz();
        configurarEventos();
    }

    private void configurarVentana() {
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(1050, 700));
        setSize(1280, 820);
        setLocationRelativeTo(null);
        setJMenuBar(crearMenu());
    }

    private void construirInterfaz() {
        JPanel contenido = new JPanel(new BorderLayout());
        contenido.setBackground(new Color(243, 244, 246));

        arbolProyecto.setRootVisible(true);
        arbolProyecto.setShowsRootHandles(true);
        arbolProyecto.setFont(new Font("SansSerif", Font.PLAIN, 14));
        arbolProyecto.setCellRenderer(new RenderizadorArbolProyecto());

        JScrollPane panelArbol = new JScrollPane(arbolProyecto);
        panelArbol.setPreferredSize(new Dimension(260, 500));
        panelArbol.setBorder(BorderFactory.createTitledBorder("Proyecto"));

        JTable tablaDiagnosticos = new JTable(modeloDiagnosticos);
        tablaDiagnosticos.setFillsViewportHeight(true);
        tablaDiagnosticos.setAutoCreateRowSorter(true);
        tablaDiagnosticos.setRowHeight(24);
        tablaDiagnosticos.getColumnModel().getColumn(5)
                .setPreferredWidth(600);

        JScrollPane panelDiagnosticos = new JScrollPane(tablaDiagnosticos);
        panelDiagnosticos.setBorder(
                BorderFactory.createTitledBorder("Diagnosticos")
        );

        JSplitPane centroVertical = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                pestanas,
                panelDiagnosticos
        );
        centroVertical.setResizeWeight(0.75);

        JSplitPane principal = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                panelArbol,
                centroVertical
        );
        principal.setDividerLocation(270);

        estado.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        estado.setHorizontalAlignment(SwingConstants.LEFT);

        contenido.add(principal, BorderLayout.CENTER);
        contenido.add(estado, BorderLayout.SOUTH);
        setContentPane(contenido);
    }

    private JMenuBar crearMenu() {
        JMenuBar barra = new JMenuBar();

        JMenu archivo = new JMenu("Archivo");
        JMenuItem abrir = new JMenuItem("Abrir proyecto...");
        abrir.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_O,
                InputEvent.CTRL_DOWN_MASK
        ));
        abrir.addActionListener(evento -> seleccionarProyecto());

        JMenuItem guardar = new JMenuItem("Guardar archivo");
        guardar.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_S,
                InputEvent.CTRL_DOWN_MASK
        ));
        guardar.addActionListener(evento -> guardarActivo());

        JMenuItem salir = new JMenuItem("Salir");
        salir.addActionListener(evento -> intentarCerrar());

        archivo.add(abrir);
        archivo.add(guardar);
        archivo.addSeparator();
        archivo.add(salir);

        JMenu compilacion = new JMenu("Compilacion");
        JMenuItem analizar = new JMenuItem("Analizar proyecto");
        analizar.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0));
        analizar.addActionListener(evento -> analizarProyecto());
        compilacion.add(analizar);

        barra.add(archivo);
        barra.add(compilacion);
        return barra;
    }

    private void configurarEventos() {
        arbolProyecto.addTreeSelectionListener(this::seleccionArbol);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent evento) {
                intentarCerrar();
            }
        });
    }

    private void seleccionarProyecto() {
        JFileChooser selector = new JFileChooser();
        selector.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        selector.setDialogTitle("Seleccionar carpeta del proyecto");

        if (selector.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            abrirProyecto(selector.getSelectedFile().toPath());
        }
    }

    private void abrirProyecto(Path raiz) {
        try {
            raizProyecto = raiz.toAbsolutePath().normalize();
            DefaultMutableTreeNode nodoRaiz = crearNodo(raizProyecto);
            arbolProyecto.setModel(new DefaultTreeModel(nodoRaiz));
            pestanas.removeAll();
            modeloDiagnosticos.actualizar(List.of());
            estado.setText("Proyecto abierto: " + raizProyecto);
        } catch (IOException excepcion) {
            mostrarError("No se pudo abrir el proyecto", excepcion);
        }
    }

    private DefaultMutableTreeNode crearNodo(Path ruta) throws IOException {
        DefaultMutableTreeNode nodo = new DefaultMutableTreeNode(ruta);

        if (!Files.isDirectory(ruta)) {
            return nodo;
        }

        List<Path> hijos;
        try (var flujo = Files.list(ruta)) {
            hijos = flujo
                    .filter(this::mostrarEnArbol)
                    .sorted(Comparator
                            .comparing((Path path) -> !Files.isDirectory(path))
                            .thenComparing(path -> path.getFileName().toString()))
                    .toList();
        }

        for (Path hijo : hijos) {
            nodo.add(crearNodo(hijo));
        }

        return nodo;
    }

    private boolean mostrarEnArbol(Path ruta) {
        if (Files.isDirectory(ruta)) {
            String nombre = ruta.getFileName().toString();
            return !nombre.equals("target")
                    && !nombre.equals(".idea")
                    && !nombre.equals(".git");
        }
        return LenguajeFuente.desdeRuta(ruta).isPresent();
    }

    private void seleccionArbol(TreeSelectionEvent evento) {
        Object seleccionado = arbolProyecto.getLastSelectedPathComponent();
        if (!(seleccionado instanceof DefaultMutableTreeNode nodo)) {
            return;
        }
        if (!(nodo.getUserObject() instanceof Path ruta)) {
            return;
        }
        if (Files.isRegularFile(ruta)) {
            abrirArchivo(ruta);
        }
    }

    private void abrirArchivo(Path ruta) {
        for (int indice = 0; indice < pestanas.getTabCount(); indice++) {
            PanelEditor panel = (PanelEditor) pestanas.getComponentAt(indice);
            if (panel.editor().ruta().equals(ruta)) {
                pestanas.setSelectedIndex(indice);
                return;
            }
        }

        try {
            LenguajeFuente lenguaje = LenguajeFuente.desdeRuta(ruta)
                    .orElseThrow();
            ArchivoFuente archivo = new ArchivoFuente(
                    ruta,
                    lenguaje,
                    Files.readString(ruta, StandardCharsets.UTF_8)
            );
            PanelEditor panel = new PanelEditor(archivo);
            pestanas.addTab(archivo.nombre(), panel);
            pestanas.setSelectedComponent(panel);
        } catch (IOException excepcion) {
            mostrarError("No se pudo abrir el archivo", excepcion);
        }
    }

    private void guardarActivo() {
        if (!(pestanas.getSelectedComponent() instanceof PanelEditor panel)) {
            return;
        }

        guardar(panel.editor());
    }

    private void guardarTodos() {
        for (int indice = 0; indice < pestanas.getTabCount(); indice++) {
            PanelEditor panel = (PanelEditor) pestanas.getComponentAt(indice);
            if (panel.editor().modificado()) {
                guardar(panel.editor());
            }
        }
    }

    private void guardar(EditorCodigo editor) {
        try {
            gestorProyecto.guardar(editor.ruta(), editor.getText());
            editor.marcarGuardado();
            actualizarTituloPestana(editor);
            estado.setText("Archivo guardado: " + editor.ruta().getFileName());
        } catch (IOException excepcion) {
            mostrarError("No se pudo guardar el archivo", excepcion);
        }
    }

    private void actualizarTituloPestana(EditorCodigo editor) {
        for (int indice = 0; indice < pestanas.getTabCount(); indice++) {
            PanelEditor panel = (PanelEditor) pestanas.getComponentAt(indice);
            if (panel.editor() == editor) {
                pestanas.setTitleAt(indice, editor.ruta().getFileName().toString());
                return;
            }
        }
    }

    private void analizarProyecto() {
        if (raizProyecto == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Primero debe abrir una carpeta de proyecto",
                    "Proyecto no seleccionado",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        try {
            guardarTodos();
            ProyectoCompilacion proyecto = gestorProyecto.abrir(raizProyecto);
            ResultadoCompilacion resultado = compilador.compilar(proyecto);
            modeloDiagnosticos.actualizar(resultado.diagnosticos());

            String mensaje = resultado.esValido()
                    ? "Analisis inicial completado sin errores"
                    : "Analisis inicial completado con errores";
            estado.setText(mensaje + " | Archivos: " + proyecto.archivos().size());
        } catch (IOException | RuntimeException excepcion) {
            mostrarError("No se pudo analizar el proyecto", excepcion);
        }
    }

    private void intentarCerrar() {
        List<EditorCodigo> modificados = new ArrayList<>();
        for (int indice = 0; indice < pestanas.getTabCount(); indice++) {
            PanelEditor panel = (PanelEditor) pestanas.getComponentAt(indice);
            if (panel.editor().modificado()) {
                modificados.add(panel.editor());
            }
        }

        if (!modificados.isEmpty()) {
            int opcion = JOptionPane.showConfirmDialog(
                    this,
                    "Hay archivos sin guardar. Desea guardarlos antes de salir?",
                    "Cambios pendientes",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (opcion == JOptionPane.CANCEL_OPTION
                    || opcion == JOptionPane.CLOSED_OPTION) {
                return;
            }
            if (opcion == JOptionPane.YES_OPTION) {
                guardarTodos();
            }
        }

        dispose();
    }

    private void mostrarError(String mensaje, Exception excepcion) {
        JOptionPane.showMessageDialog(
                this,
                mensaje + ": " + excepcion.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE
        );
    }
}
