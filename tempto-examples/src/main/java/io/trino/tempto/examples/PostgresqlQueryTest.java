/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.trino.tempto.examples;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import io.trino.tempto.ProductTest;
import io.trino.tempto.Requirement;
import io.trino.tempto.Requirements;
import io.trino.tempto.RequirementsProvider;
import io.trino.tempto.Requires;
import io.trino.tempto.configuration.Configuration;
import io.trino.tempto.fulfillment.table.ImmutableTableRequirement;
import io.trino.tempto.fulfillment.table.MutableTableRequirement;
import io.trino.tempto.fulfillment.table.MutableTablesState;
import io.trino.tempto.fulfillment.table.TableInstance;
import io.trino.tempto.fulfillment.table.jdbc.RelationalDataSource;
import io.trino.tempto.fulfillment.table.jdbc.RelationalTableDefinition;
import io.trino.tempto.query.QueryExecutor;
import org.testng.annotations.Test;

import javax.inject.Named;

import java.util.List;

import static io.trino.tempto.assertions.QueryAssert.Row.row;
import static io.trino.tempto.assertions.QueryAssert.assertThat;
import static io.trino.tempto.fulfillment.table.ImmutableTablesState.immutableTablesState;
import static io.trino.tempto.fulfillment.table.TableHandle.tableHandle;
import static io.trino.tempto.fulfillment.table.jdbc.RelationalTableDefinition.relationalTableDefinition;

public class PostgresqlQueryTest
        extends ProductTest
{
    @Inject
    @Named("psql")
    private QueryExecutor queryExecutor;

    @Inject
    private MutableTablesState mutableTablesState;

    private static final RelationalTableDefinition TEST_TABLE_DEFINITION;

    static {
        RelationalDataSource dataSource = () -> ImmutableList.<List<Object>>of(
                ImmutableList.of(1, "x"),
                ImmutableList.of(2, "y")
        ).iterator();
        TEST_TABLE_DEFINITION = relationalTableDefinition("test_table", "CREATE TABLE %NAME% (a int, b varchar(100))", dataSource);
    }

    private static class ImmutableTestJdbcTables
            implements RequirementsProvider
    {
        @Override
        public Requirement getRequirements(Configuration configuration)
        {
            return Requirements.compose(
                    new ImmutableTableRequirement(RelationalTableDefinition.like(TEST_TABLE_DEFINITION).withDatabase("psql").build()),
                    new ImmutableTableRequirement(RelationalTableDefinition.like(TEST_TABLE_DEFINITION).withDatabase("psql").withSchema("test_schema").build())
            );
        }
    }

    private static class MutableTestJdbcTables
            implements RequirementsProvider
    {
        @Override
        public Requirement getRequirements(Configuration configuration)
        {
            return Requirements.compose(
                    MutableTableRequirement.builder(TEST_TABLE_DEFINITION).withDatabase("psql").build(),
                    MutableTableRequirement.builder(TEST_TABLE_DEFINITION).withDatabase("psql").withSchema("test_schema").build()
            );
        }
    }

    @Test(groups = "psql_query")
    @Requires(ImmutableTestJdbcTables.class)
    public void selectFromImmutableTable()
    {
        String nameInDatabase = immutableTablesState().get(tableHandle("test_table").withNoSchema()).getNameInDatabase();
        assertThat(queryExecutor.executeQuery("select * from " + nameInDatabase)).containsOnly(row(1, "x"), row(2, "y"));
    }

    @Test(groups = "psql_query")
    @Requires(MutableTestJdbcTables.class)
    public void selectFromMutableTable()
    {
        String tableName = mutableTablesState.get(tableHandle("test_table").withNoSchema()).getNameInDatabase();
        assertThat(queryExecutor.executeQuery("select * from " + tableName)).containsOnly(row(1, "x"), row(2, "y"));
    }

    @Test(groups = {"psql_query"})
    @Requires(MutableTestJdbcTables.class)
    public void selectFromTableInDifferentSchema()
    {
        TableInstance tableInstance = mutableTablesState.get(tableHandle("test_table").inSchema("test_schema"));
        assertThat(queryExecutor.executeQuery("select * from " + tableInstance.getNameInDatabase())).containsOnly(row(1, "x"), row(2, "y"));
    }
}
