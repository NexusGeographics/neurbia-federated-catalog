package es.nexus.neurbia.extension.fc.asset_filter;

import es.nexus.neurbia.extension.fc.asset_filter.FederatedCatalogAssetQueryController;
import org.eclipse.edc.crawler.spi.TargetNodeDirectory;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.edc.web.spi.WebService;

@Provides(FederatedCatalogAssetQueryController.class)
@Extension(value = "Federated Catalog Asset Query API")
public class FederatedCatalogAssetQueryExtension implements ServiceExtension {
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
    private DataSourceRegistry dataSourceRegistry;

    @Inject
    private TransactionContext transactionContext;

    @Inject
    private Monitor monitor;

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor.info("Initializing Federated Catalog Asset Query API");

        jsonLd.registerNamespace("nexus", "https://w3id.org/nexus/v0.0.1/ns/", CATALOG_QUERY_SCOPE);
        var controller = new FederatedCatalogAssetQueryController(dataSourceRegistry,transactionContext,context.getMonitor());
        webService.registerResource(controller);
    }

}


