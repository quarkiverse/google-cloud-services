package io.quarkiverse.googlecloudservices.it;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.google.cloud.bigtable.admin.v2.BigtableTableAdminClient;
import com.google.cloud.bigtable.admin.v2.models.CreateTableRequest;
import com.google.cloud.bigtable.data.v2.BigtableDataClient;
import com.google.cloud.bigtable.data.v2.models.Row;
import com.google.cloud.bigtable.data.v2.models.RowCell;
import com.google.cloud.bigtable.data.v2.models.RowMutation;

@Path("/bigtable")
public class BigtableResource {
    private static final String INSTANCE_ID = "test-instance";
    private static final String TABLE_ID = "test-table";
    private static final String COLUMN_FAMILY_ID = "test-column-family";

    @ConfigProperty(name = "quarkus.google.cloud.project-id")
    String projectId;

    @PostConstruct
    void initBigtable() throws IOException {
        BigtableTableAdminClient adminClient = BigtableTableAdminClient.create(projectId, INSTANCE_ID);
        if (!adminClient.exists(TABLE_ID)) {
            System.out.println("Creating table: " + TABLE_ID);
            CreateTableRequest createTableRequest = CreateTableRequest.of(TABLE_ID).addFamily(COLUMN_FAMILY_ID);
            adminClient.createTable(createTableRequest);
            System.out.printf("Table %s created successfully%n", TABLE_ID);
        }
    }

    @GET
    public String bigtable() throws IOException {
        BigtableDataClient dataClient = BigtableDataClient.create(projectId, INSTANCE_ID);
        try {
            // create a row
            RowMutation rowMutation = RowMutation.create(TABLE_ID, "key1").setCell(COLUMN_FAMILY_ID, "test", "value1");
            dataClient.mutateRow(rowMutation);

            Row row = dataClient.readRow(TABLE_ID, "key1");
            System.out.println("Row: " + row.getKey().toStringUtf8());
            StringBuilder cells = new StringBuilder();
            for (RowCell cell : row.getCells()) {
                cells.append(String.format(
                        "Family: %s    Qualifier: %s    Value: %s%n",
                        cell.getFamily(), cell.getQualifier().toStringUtf8(), cell.getValue().toStringUtf8()));
            }
            return cells.toString();
        } finally {
            dataClient.close();
        }
    }
}
