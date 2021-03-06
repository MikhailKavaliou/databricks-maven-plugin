package com.edmunds.tools.databricks.maven;

/**
 * Tests for @{@link UpsertClusterMojo}.
 */
public class UpsertClusterMojoTest extends AbstractUpsertClusterMojoTest<UpsertClusterMojo> {

    @Override
    protected void setGoal() {
        GOAL = "upsert-cluster";
    }

    @Override
    protected String getPath() {
        return underTest.dbClusterFile.getPath();
    }

}