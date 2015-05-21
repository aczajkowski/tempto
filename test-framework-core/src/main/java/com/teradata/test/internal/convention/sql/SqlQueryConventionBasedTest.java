/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */

package com.teradata.test.internal.convention.sql;

import com.teradata.test.Requirement;
import com.teradata.test.convention.SqlResultDescriptor;
import com.teradata.test.internal.convention.ConventionBasedTest;
import com.teradata.test.internal.convention.SqlQueryDescriptor;
import com.teradata.test.query.QueryExecutor;
import com.teradata.test.query.QueryResult;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.Optional;

import static com.teradata.test.assertions.QueryAssert.assertThat;
import static com.teradata.test.context.ThreadLocalTestContextHolder.testContext;
import static com.teradata.test.internal.convention.ProcessUtils.execute;
import static java.lang.Character.isAlphabetic;
import static org.slf4j.LoggerFactory.getLogger;

public class SqlQueryConventionBasedTest
        extends ConventionBasedTest
{
    private static final Logger LOGGER = getLogger(SqlQueryConventionBasedTest.class);

    private final Optional<Path> beforeScriptPath;
    private final Optional<Path> afterScriptPath;
    private final Path queryFile;
    private final int testNumber;
    private final SqlQueryDescriptor queryDescriptor;
    private final SqlResultDescriptor resultDescriptor;
    private final Requirement requirement;

    public SqlQueryConventionBasedTest(Optional<Path> beforeScriptFile, Optional<Path> afterScriptFile,
            Path queryFile, int testNumber, SqlQueryDescriptor queryDescriptor, SqlResultDescriptor resultDescriptor,
            Requirement requirement)
    {
        this.beforeScriptPath = beforeScriptFile;
        this.afterScriptPath = afterScriptFile;
        this.queryFile = queryFile;
        this.testNumber = testNumber;
        this.queryDescriptor = queryDescriptor;
        this.resultDescriptor = resultDescriptor;
        this.requirement = requirement;
    }

    @Override
    public void test()
    {
        LOGGER.debug("Executing sql test: {}#{}", queryFile.getFileName(), queryDescriptor.getName());

        if (beforeScriptPath.isPresent()) {
            execute(beforeScriptPath.get().toString());
        }

        QueryExecutor queryExecutor = getQueryExecutor(queryDescriptor);

        QueryResult queryResult;
        if (queryDescriptor.getQueryType().isPresent()) {
            queryResult = queryExecutor.executeQuery(queryDescriptor.getContent(), queryDescriptor.getQueryType().get());
        }
        else {
            queryResult = queryExecutor.executeQuery(queryDescriptor.getContent());
        }

        assertThat(queryResult).matches(resultDescriptor);

        if (afterScriptPath.isPresent()) {
            execute(afterScriptPath.get().toString());
        }
    }

    @Override
    public String testName()
    {
        String testName = FilenameUtils.getBaseName(queryFile.getParent().toString());
        if (!isAlphabetic(testName.charAt(0))) {
            return "Test" + testName;
        }
        return testName;
    }

    @Override
    public String testCaseName()
    {
        String testCaseName;
        if (queryDescriptor.getName().isPresent()) {
            testCaseName = queryDescriptor.getName().get().replaceAll("\\s", "");
        }
        else {
            testCaseName = FilenameUtils.getBaseName(queryFile.getFileName().toString()) + "_" + testNumber;
        }

        if (!isAlphabetic(testCaseName.charAt(0))) {
            return "test_" + testCaseName;
        }

        return testCaseName;
    }

    @Override
    public Requirement getRequirements()
    {
        return requirement;
    }

    @Override
    public String[] testGroups()
    {
        return queryDescriptor.getTestGroups().toArray(new String[0]);
    }

    private QueryExecutor getQueryExecutor(SqlQueryDescriptor sqlQueryDescriptor)
    {
        String database = sqlQueryDescriptor.getDatabaseName();
        try {
            return testContext().getDependency(QueryExecutor.class, database);
        }
        catch (RuntimeException e) {
            throw new RuntimeException("Cannot get query executor for database '" + database + "'", e);
        }
    }
}
