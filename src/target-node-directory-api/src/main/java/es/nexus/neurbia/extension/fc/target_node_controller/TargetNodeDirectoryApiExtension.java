package es.nexus.neurbia.extension.fc.target_node_controller;


import org.eclipse.edc.crawler.spi.TargetNodeDirectory;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.web.spi.WebService;

@Provides(TargetNodeDirectoryApiController.class)
@Extension(value = "Target Node Directory API")
public class TargetNodeDirectoryApiExtension implements ServiceExtension {
    static final String CATALOG_QUERY_SCOPE = "CATALOG_QUERY_API";

    @Inject
    private QueryExecutor queryExecutor;

    @Inject
    private TargetNodeDirectory directory;

    @Inject
    private WebService webService;
    @Inject
    private JsonLd jsonLd;

    @Inject
    private Monitor monitor;

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor.info("Initializing Target Node Directory API");

        jsonLd.registerNamespace("nexus", "https://w3id.org/nexus/v0.0.1/ns/", CATALOG_QUERY_SCOPE);

        var controller = new TargetNodeDirectoryApiController(directory,queryExecutor);
        webService.registerResource(controller);
    }
}