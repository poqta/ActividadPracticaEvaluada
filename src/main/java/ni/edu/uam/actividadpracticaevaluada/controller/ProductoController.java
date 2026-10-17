package ni.edu.uam.actividadpracticaevaluada.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import ni.edu.uam.actividadpracticaevaluada.model.Producto;
import ni.edu.uam.actividadpracticaevaluada.model.ProductoDAO;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;


public class ProductoController {

    @FXML
    private TextField txtBusqueda;
    @FXML
    private ComboBox<String> cmbFiltroCategoria;
    @FXML
    private CheckBox chkSoloStock;
    @FXML
    private Label lblContador;

    @FXML
    private TableView<Producto> tblProductos;
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

    @FXML
    private TextField txtId;
    @FXML
    private TextField txtCodigo;
    @FXML
    private TextField txtNombre;
    @FXML
    private ComboBox<String> cmbCategoria;
    @FXML
    private TextField txtPrecio;
    @FXML
    private TextField txtStock;

    // Instancia del DAO y lista observable para reflejar cambios en la tabla
    private final ProductoDAO productoDAO = new ProductoDAO();
    private final ObservableList<Producto> listaProductos = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Llenar los combobox con las categorías de la guia
        cmbFiltroCategoria.getItems().addAll("Todas", "Papelería", "Tecnología", "Accesorios");
        cmbFiltroCategoria.setValue("Todas");

        cmbCategoria.getItems().addAll("Papelería", "Tecnología", "Accesorios");

        // Conectar cada columna de la tabla con los getters de la clase Producto
        colId.setCellValueFactory(new PropertyValueFactory<>("idProducto"));
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colCategoria.setCellValueFactory(new PropertyValueFactory<>("categoria"));
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precio"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("stock"));

        tblProductos.setItems(listaProductos);

        // Cuando el usuario toca una fila, pasa los datos a los campos de la derecha
        tblProductos.getSelectionModel().selectedItemProperty().addListener((obs, viejo, nuevo) -> {
            if (nuevo != null) {
                txtId.setText(String.valueOf(nuevo.getIdProducto()));
                txtCodigo.setText(nuevo.getCodigo());
                txtNombre.setText(nuevo.getNombre());
                cmbCategoria.setValue(nuevo.getCategoria());
                txtPrecio.setText(nuevo.getPrecio().toString());
                txtStock.setText(String.valueOf(nuevo.getStock()));
            }
        });

        recargarDatos();
    }

    @FXML
    private void accionBuscar() {
        recargarDatos();
    }

    // Limpia los filtros superiores y trae todo otra vez
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

    // Deja en blanco las cajas de texto del formulario
    @FXML
    private void accionLimpiarFormulario() {
        txtId.clear();
        txtCodigo.clear();
        txtNombre.clear();
        cmbCategoria.setValue(null);
        txtPrecio.clear();
        txtStock.clear();
        tblProductos.getSelectionModel().clearSelection();
    }

    // Metodo privado para revisar las validaciones (V1 a V9)
    private String validarFormulario(boolean esEdicion) {
        // V1: Validar tamaño del código
        String cod = txtCodigo.getText() != null ? txtCodigo.getText().trim().toUpperCase() : "";
        if (cod.isEmpty() || cod.length() > 10) {
            return "V1: El código debe tener entre 1 y 10 caracteres.";
        }

        // V2: Validar tamaño del nombre
        String nom = txtNombre.getText() != null ? txtNombre.getText().trim() : "";
        if (nom.length() < 3 || nom.length() > 80) {
            return "V2: El nombre debe tener entre 3 y 80 caracteres.";
        }

        // V3: Forzar selección de categoría
        if (cmbCategoria.getValue() == null || cmbCategoria.getValue().isEmpty()) {
            return "V3: Debe seleccionar una categoría.";
        }

        // V4: Chequear si el precio es un número válido
        BigDecimal prec;
        try {
            prec = new BigDecimal(txtPrecio.getText().trim());
        } catch (Exception e) {
            return "V4: El precio debe ser un valor numérico válido.";
        }

        // V5: El precio no puede ser 0 ni negativo
        if (prec.compareTo(BigDecimal.ZERO) <= 0) {
            return "V5: El precio debe ser strictly mayor a 0.";
        }

        // V6: Que no se pase de 2 decimales
        if (prec.scale() > 2 || prec.compareTo(new BigDecimal("99999999.99")) > 0) {
            return "V6: El precio no debe exceder de 2 decimales ni el rango permitido.";
        }

        // V7 y V8: El stock debe ser entero y no negativo
        try {
            int st = Integer.parseInt(txtStock.getText().trim());
            if (st < 0) return "V8: El stock no puede ser un valor negativo.";
        } catch (NumberFormatException e) {
            return "V7: El stock debe ser un número entero.";
        }

        // V9: Consultar en la BD si ya existe ese código
        Integer idActual = esEdicion ? Integer.parseInt(txtId.getText()) : null;
        try {
            if (productoDAO.existeCodigo(cod, idActual)) {
                return "V9: El código '" + cod + "' ya existe en el catálogo.";
            }
        } catch (SQLException e) {
            return "Error de validación contra la base de datos.";
        }

        return null;
    }

    // Boton de guardar nuevo registro
    @FXML
    private void accionGuardar() {
        String error = validarFormulario(false);
        if (error != null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Validación de Datos", error);
            return;
        }

        Producto p = new Producto(
                null,
                txtCodigo.getText().trim().toUpperCase(),
                txtNombre.getText().trim(),
                cmbCategoria.getValue(),
                new BigDecimal(txtPrecio.getText().trim()),
                Integer.parseInt(txtStock.getText().trim())
        );

        try {
            productoDAO.insertar(p);
            accionLimpiarFormulario();
            recargarDatos();
            mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito", "Producto guardado correctamente en PostgreSQL.");
        } catch (SQLException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error SQL", "Ocurrió un fallo al insertar el producto.");
        }
    }

    // Boton de actualizar registro seleccionado
    @FXML
    private void accionActualizar() {
        // V10: Verificar que hay un registro seleccionado
        if (txtId.getText().isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "V10: Selección", "Debe seleccionar un registro de la tabla para editar.");
            return;
        }

        String error = validarFormulario(true);
        if (error != null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Validación de Datos", error);
            return;
        }

        Producto p = new Producto(
                Integer.parseInt(txtId.getText()),
                txtCodigo.getText().trim().toUpperCase(),
                txtNombre.getText().trim(),
                cmbCategoria.getValue(),
                new BigDecimal(txtPrecio.getText().trim()),
                Integer.parseInt(txtStock.getText().trim())
        );

        try {
            int filas = productoDAO.actualizar(p);
            if (filas == 0) {
                mostrarAlerta(Alert.AlertType.WARNING, "Aviso", "El registro seleccionado ya no existe en la base de datos.");
            } else {
                mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito", "Producto actualizado correctamente.");
            }
            accionLimpiarFormulario();
            recargarDatos();
        } catch (SQLException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error SQL", "No se pudo actualizar el registro.");
        }
    }

    // Boton de eliminar
    @FXML
    private void accionEliminar() {
        // V10: Obligar a seleccionar una fila primero
        if (txtId.getText().isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "V10: Selección", "Debe seleccionar un producto para eliminar.");
            return;
        }

        // Confirmacion antes de borrar
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "¿Está seguro de eliminar este producto?", ButtonType.YES, ButtonType.NO);
        Optional<ButtonType> resp = confirm.showAndWait();

        if (resp.isPresent() && resp.get() == ButtonType.YES) {
            try {
                int id = Integer.parseInt(txtId.getText());
                int filas = productoDAO.eliminar(id);
                if (filas == 0) {
                    mostrarAlerta(Alert.AlertType.WARNING, "Aviso", "El producto no existe o ya fue eliminado.");
                } else {
                    mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito", "Producto eliminado correctamente de PostgreSQL.");
                }
                accionLimpiarFormulario();
                recargarDatos();
            } catch (SQLException e) {
                mostrarAlerta(Alert.AlertType.ERROR, "Error SQL", "Error al eliminar el producto.");
            }
        }
    }

    // Trae los datos actualizados desde el DAO y refresca la tabla y el contador
    private void recargarDatos() {
        try {
            List<Producto> datos = productoDAO.buscarYFiltrar(
                    txtBusqueda.getText(),
                    cmbFiltroCategoria.getValue(),
                    chkSoloStock.isSelected()
            );

            listaProductos.setAll(datos);
            lblContador.setText("Registros visibles: " + datos.size());

            // Mensaje por si la búsqueda no trajo nada
            if (datos.isEmpty() && txtBusqueda.getText() != null && !txtBusqueda.getText().trim().isEmpty()) {
                mostrarAlerta(Alert.AlertType.INFORMATION, "Búsqueda", "Sin coincidencias para el criterio ingresado.");
            }
        } catch (SQLException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error de Conexión", "Error al conectar a PostgreSQL.");
        }
    }

    // Metodo para reutilizar la creacion de ventanas emergentes / alertas
    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}