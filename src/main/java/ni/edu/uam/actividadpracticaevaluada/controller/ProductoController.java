package ni.edu.uam.actividadpracticaevaluada.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import ni.edu.uam.actividadpracticaevaluada.database.ConexionBD;
import ni.edu.uam.actividadpracticaevaluada.model.Producto;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/*
 * Controlador de la interfaz gráfica (Avance Sesión 1).
 * Maneja la visualización en TableView, contador de registros y filtros de búsqueda.
 */
public class ProductoController {

    // Filtros superiores
    @FXML
    private TextField txtBusqueda;
    @FXML
    private ComboBox<String> cmbFiltroCategoria;
    @FXML
    private CheckBox chkSoloStock;
    @FXML
    private Label lblContador;

    // Tabla principal y sus columnas
    @FXML
    private TableView<Producto> tablaProductos;
    @FXML
    private TableColumn<Producto, Integer> colId;
    @FXML
    private TableColumn<Producto, String> colCodigo;
    @FXML
    private TableColumn<Producto, String> colNombre;
    @FXML
    private TableColumn<Producto, String> colCategoria;
    @FXML
    private TableColumn<Producto, BigDecimal> colPrecio;
    @FXML
    private TableColumn<Producto, Integer> colStock;

    // Controles del formulario
    @FXML
    private TextField txtId;
    @FXML
    private TextField txtCodigo;
    @FXML
    private TextField txtNombre;
    @FXML
    private ComboBox<String> cbCategoria;
    @FXML
    private TextField txtPrecio;
    @FXML
    private TextField txtStock;

    private final ObservableList<Producto> listaProductos = FXCollections.observableArrayList();

    /*
     * Inicializa los componentes de la interfaz.
     */
    @FXML
    public void initialize() {
        // Cargar opciones en ComboBox
        cmbFiltroCategoria.getItems().addAll("Todas", "Papelería", "Tecnología", "Accesorios");
        cmbFiltroCategoria.setValue("Todas");

        cbCategoria.getItems().addAll("Papelería", "Tecnología", "Accesorios");

        // Configurar columnas de la tabla
        colId.setCellValueFactory(new PropertyValueFactory<>("idProducto"));
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colCategoria.setCellValueFactory(new PropertyValueFactory<>("categoria"));
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precio"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("stock"));

        tablaProductos.setItems(listaProductos);

        // Mostrar selección en el formulario lateral
        tablaProductos.getSelectionModel().selectedItemProperty().addListener((obs, viejo, nuevo) -> {
            if (nuevo != null) {
                txtId.setText(String.valueOf(nuevo.getIdProducto()));
                txtCodigo.setText(nuevo.getCodigo());
                txtNombre.setText(nuevo.getNombre());
                cbCategoria.setValue(nuevo.getCategoria());
                txtPrecio.setText(nuevo.getPrecio().toString());
                txtStock.setText(String.valueOf(nuevo.getStock()));
            }
        });

        // Carga inicial
        recargarDatos();
    }

    @FXML
    private void accionBuscar() {
        recargarDatos();
    }

    @FXML
    private void accionLimpiarFiltros() {
        txtBusqueda.clear();
        cmbFiltroCategoria.setValue("Todas");
        chkSoloStock.setSelected(false);
        recargarDatos();
    }

    @FXML
    private void accionRecargar() {
        recargarDatos();
    }

    @FXML
    private void accionLimpiarFormulario() {
        txtId.clear();
        txtCodigo.clear();
        txtNombre.clear();
        cbCategoria.setValue(null);
        txtPrecio.clear();
        txtStock.clear();
        tablaProductos.getSelectionModel().clearSelection();
    }

    /*
     * Realiza la consulta directa a PostgreSQL y actualiza la lista y el contador.
     */
    private void recargarDatos() {
        try {
            List<Producto> datos = consultarProductosPostgreSQL(
                    txtBusqueda.getText(),
                    cmbFiltroCategoria.getValue(),
                    chkSoloStock.isSelected()
            );

            listaProductos.setAll(datos);
            lblContador.setText("Registros visibles: " + datos.size());

            if (datos.isEmpty() && txtBusqueda.getText() != null && !txtBusqueda.getText().trim().isEmpty()) {
                mostrarAlerta(Alert.AlertType.INFORMATION, "Búsqueda", "Sin coincidencias para el criterio ingresado.");
            }
        } catch (SQLException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error de Conexión", "No se pudo conectar a la base de datos PostgreSQL.");
        }
    }

    /*
     * Consulta parametrizada a PostgreSQL para la Sesión 1.
     */
    private List<Producto> consultarProductosPostgreSQL(String busqueda, String categoria, boolean soloStock) throws SQLException {
        List<Producto> lista = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT id_producto, codigo, nombre, categoria, precio, stock FROM producto WHERE 1=1");

        if (busqueda != null && !busqueda.trim().isEmpty()) {
            sql.append(" AND (nombre ILIKE ? OR codigo ILIKE ?)");
        }

        if (categoria != null && !categoria.equalsIgnoreCase("Todas") && !categoria.trim().isEmpty()) {
            sql.append(" AND categoria = ?");
        }

        if (soloStock) {
            sql.append(" AND stock > 0");
        }

        sql.append(" ORDER BY codigo ASC");

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            int paramIndex = 1;

            if (busqueda != null && !busqueda.trim().isEmpty()) {
                String patron = "%" + busqueda.trim() + "%";
                stmt.setString(paramIndex++, patron);
                stmt.setString(paramIndex++, patron);
            }

            if (categoria != null && !categoria.equalsIgnoreCase("Todas") && !categoria.trim().isEmpty()) {
                stmt.setString(paramIndex++, categoria.trim());
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Producto prod = new Producto();
                    prod.setIdProducto(rs.getInt("id_producto"));
                    prod.setCodigo(rs.getString("codigo"));
                    prod.setNombre(rs.getString("nombre"));
                    prod.setCategoria(rs.getString("categoria"));
                    prod.setPrecio(rs.getBigDecimal("precio"));
                    prod.setStock(rs.getInt("stock"));

                    lista.add(prod);
                }
            }
        }
        return lista;
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }

    // Métodos de los botones del formulario (Pendientes para la Sesión 2/3)
    @FXML private void accionGuardar() {}
    @FXML private void accionActualizar() {}
    @FXML private void accionEliminar() {}
}