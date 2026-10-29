package es.nexus.neurbia.extension.fc.target_node_controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.crawler.spi.TargetNode;
import org.eclipse.edc.crawler.spi.TargetNodeDirectory;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.sql.QueryExecutor;

import java.io.IOException;
import java.util.List;
import java.util.Optional;


@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/target-node-directory")
public class TargetNodeDirectoryApiController {

    private final TargetNodeDirectory directory;

    private final ObjectMapper objectMapper;


    public TargetNodeDirectoryApiController(TargetNodeDirectory directory, QueryExecutor queryExecutor) {
        this.directory = directory;
        this.objectMapper = new ObjectMapper();
    }

    @GET
    public String getAll() throws EdcPersistenceException, IOException {
        List<TargetNode> allNodes= directory.getAll();
        return objectMapper.writeValueAsString(allNodes).toString();
    }


    public Optional<TargetNode> findById(String id)  throws EdcPersistenceException, IOException{
        String lowerId = id.toLowerCase();
        return directory.getAll().stream().filter(item-> item.id().equals(lowerId)).findFirst();
    }

    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") String id)  throws EdcPersistenceException, IOException{
        try {
            Optional<TargetNode> node = findById(id);
            if (node.isPresent()) {
                return Response.ok(node.get()).build();
            } else {
                return Response.status(404, "Target Node Not Found").build();
            }
        }catch (Exception e){
            return Response.status(500, e.getMessage()).build();
        }
    }



    @POST
    public Response create(String targetNode)  throws EdcPersistenceException, IOException{
        try {
            JsonNode jsonNode = objectMapper.readTree(targetNode);
            if (jsonNode.has("id") && jsonNode.get("id").isTextual()) {
                ((ObjectNode) jsonNode).put("id", jsonNode.get("id").asText().toLowerCase());

            }
            TargetNode node = objectMapper.treeToValue(jsonNode, TargetNode.class);
            Optional<TargetNode> currentNode = findById(node.id());
            if (currentNode.isPresent()) {
                return Response.status(409, "Target Node Already Exists").build();
            }
            directory.insert(node);
            return Response.status(201).entity(node).build();
        }catch (Exception e){
            return Response.status(500, e.getMessage()).build();
        }
    }



    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") String id)  throws EdcPersistenceException, IOException{
        try {
            String lowerId = id.toLowerCase();
            TargetNode removedNode = directory.remove(lowerId);
            return Response.status(204).entity(removedNode).build();
        }catch (Exception e){
            return Response.status(500, e.getMessage()).build();
        }
    }

}
