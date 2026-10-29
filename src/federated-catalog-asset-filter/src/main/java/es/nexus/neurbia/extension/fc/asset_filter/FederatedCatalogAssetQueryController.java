package es.nexus.neurbia.extension.fc.asset_filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;


@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/federated-catalog")
public class FederatedCatalogAssetQueryController {

    private final ObjectMapper objectMapper;

    private final DataSourceRegistry dataSourceRegistry;

    private final TransactionContext transactionContext;

    private final Monitor monitor;

    public FederatedCatalogAssetQueryController(DataSourceRegistry dataSourceRegistry, TransactionContext transactionContext, Monitor monitor) {
        this.dataSourceRegistry = dataSourceRegistry;
        this.transactionContext = transactionContext;
        this.objectMapper = new ObjectMapper();
        this.monitor = monitor;
    }

    @POST
    @Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    public String searchAssets(String searchQuery) {
        try {
            CatalogQueryBuilder.ListCondition conditionArray = objectMapper.readValue(searchQuery, CatalogQueryBuilder.ListCondition.class);
            CatalogQueryBuilder builder = new CatalogQueryBuilder();
            String query = builder.buildSql(conditionArray.conditions);
            return runQueryToJson(query);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private String runQueryToJson(String sql, String... params) {
        DataSource dataSource = dataSourceRegistry.resolve("default");
        monitor.info("Executing SQL: " + sql);
        try {
            ArrayNode arr = transactionContext.execute(() -> {
                try (var conn = dataSource.getConnection();
                     var ps = conn.prepareStatement(sql)) {

                    for (int i = 0; i < params.length; i++) {
                        ps.setString(i + 1, params[i]);
                    }
                    long startTime = System.nanoTime();
                    try (var rs = ps.executeQuery()) {
                        long endTime = System.nanoTime();
                        monitor.info("Query took " + (endTime - startTime) / 1000000 + " ms");
                        return resultSetToJson(rs);
                    }
                } catch (SQLException e) {
                    throw new EdcException("SQL error: " + e.getMessage(), e);
                }
            });
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(arr);
        } catch (Exception e) {
            return "{\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}";
        }
    }

    private ArrayNode resultSetToJson(ResultSet rs) throws SQLException {
        ArrayNode array = objectMapper.createArrayNode();
        ResultSetMetaData meta = rs.getMetaData();
        int colCount = meta.getColumnCount();
        int rowCount = 0;
        while (rs.next()) {
            rowCount++;
            ObjectNode row = objectMapper.createObjectNode();
            for (int i = 1; i <= colCount; i++) {
                String label = meta.getColumnLabel(i);
                String typeName = meta.getColumnTypeName(i).toLowerCase();

                Object value = rs.getObject(i);
                if (value == null) {
                    row.putNull(label);
                    continue;
                }

                // Parse JSON/JSONB into structured JSON
                if (typeName.equals("json") || typeName.equals("jsonb")) {
                    try {
                        JsonNode node = objectMapper.readTree(rs.getString(i));
                        row.set(label, node);
                    } catch (Exception ex) {
                        row.put(label, rs.getString(i)); // fallback to raw text
                    }
                    continue;
                }

                // Timestamps
                if (value instanceof Timestamp ts) {
                    row.put(label, ts.toInstant().toString());
                    continue;
                }

                // Java time via getObject
                if (value instanceof OffsetDateTime odt) {
                    row.put(label, odt.toString());
                    continue;
                }

                // Numeric types
                if (value instanceof Integer v) {
                    row.put(label, v);
                    continue;
                }
                if (value instanceof Long v) {
                    row.put(label, v);
                    continue;
                }
                if (value instanceof Double v) {
                    row.put(label, v);
                    continue;
                }
                if (value instanceof Float v) {
                    row.put(label, v);
                    continue;
                }
                if (value instanceof Short v) {
                    row.put(label, v);
                    continue;
                }
                if (value instanceof Boolean v) {
                    row.put(label, v);
                    continue;
                }

                // Everything else as string
                row.put(label, rs.getString(i));
            }
            array.add(row);
        }
        monitor.info("Rows: " + rowCount + "");
        return array;
    }
}
