package ni.edu.uam.actividadpracticaevaluada.model;

import ni.edu.uam.actividadpracticaevaluada.database.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// DAO de la entidad Producto para manejar todo el CRUD con PostgreSQL
public class ProductoDAO {

    // Método para filtrar la tabla por texto, categoría o si solo se quiere ver lo que tiene stock
    public List<Producto> buscarYFiltrar(String busqueda, String categoria, boolean soloStock) throws SQLException {
        List<Producto> lista = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT id_producto, codigo, nombre, categoria, precio, stock FROM producto WHERE 1=1");

        // Si mandan algo en la caja de búsqueda, filtra por nombre o código (sin importar mayúsculas/minúsculas)
        if (busqueda != null && !busqueda.trim().isEmpty()) {
            sql.append(" AND (nombre ILIKE ? OR codigo ILIKE ?)");
        }

        // Si eligieron una categoría específica en el combo
        if (categoria != null && !categoria.equalsIgnoreCase("Todas") && !categoria.trim().isEmpty()) {
            sql.append(" AND categoria = ?");
        }

        // Si marcaron el checkbox de solo stock mayor a 0
        if (soloStock) {
            sql.append(" AND stock > 0");
        }

        // Ordenar siempre por código de forma ascendente
        sql.append(" ORDER BY codigo ASC");

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            int paramIndex = 1;

            // Mapear los parámetros dinámicos en el PreparedStatement
            if (busqueda != null && !busqueda.trim().isEmpty()) {
                String patron = "%" + busqueda.trim() + "%";
                stmt.setString(paramIndex++, patron);
                stmt.setString(paramIndex++, patron);
            }

            if (categoria != null && !categoria.equalsIgnoreCase("Todas") && !categoria.trim().isEmpty()) {
                stmt.setString(paramIndex++, categoria.trim());
            }

            // Ejecutar la consulta y armar la lista de objetos Producto
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
        } catch (SQLException e) {
            System.err.println("[LOG Error DAO Buscar] " + e.getMessage());
            throw e;
        }

        return lista;
    }

    // Insertar un nuevo registro en la base de datos
    public void insertar(Producto p) throws SQLException {
        String sql = "INSERT INTO producto (codigo, nombre, categoria, precio, stock) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, p.getCodigo());
            stmt.setString(2, p.getNombre());
            stmt.setString(3, p.getCategoria());
            stmt.setBigDecimal(4, p.getPrecio());
            stmt.setInt(5, p.getStock());
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[LOG Error DAO Insertar] " + e.getMessage());
            throw e;
        }
    }

    // Modificar un producto ya existente buscando por su ID
    public int actualizar(Producto p) throws SQLException {
        String sql = "UPDATE producto SET codigo = ?, nombre = ?, categoria = ?, precio = ?, stock = ? WHERE id_producto = ?";
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, p.getCodigo());
            stmt.setString(2, p.getNombre());
            stmt.setString(3, p.getCategoria());
            stmt.setBigDecimal(4, p.getPrecio());
            stmt.setInt(5, p.getStock());
            stmt.setInt(6, p.getIdProducto());
            return stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[LOG Error DAO Actualizar] " + e.getMessage());
            throw e;
        }
    }

    // Borrar un producto de la tabla según su ID
    public int eliminar(int idProducto) throws SQLException {
        String sql = "DELETE FROM producto WHERE id_producto = ?";
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idProducto);
            return stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[LOG Error DAO Eliminar] " + e.getMessage());
            throw e;
        }
    }

    // Método auxiliar para la validación V9 (comprobar que no exista otro producto con el mismo código)
    public boolean existeCodigo(String codigo, Integer idExcluir) throws SQLException {
        String sql = "SELECT COUNT(*) FROM producto WHERE UPPER(TRIM(codigo)) = UPPER(TRIM(?))";

        // Si estamos editando, ignoramos el código del mismo registro que se está modificando
        if (idExcluir != null) {
            sql += " AND id_producto <> ?";
        }
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, codigo);
            if (idExcluir != null) {
                stmt.setInt(2, idExcluir);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }
}