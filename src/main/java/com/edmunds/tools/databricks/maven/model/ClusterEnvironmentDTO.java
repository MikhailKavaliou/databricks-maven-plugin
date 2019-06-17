/*
 *  Copyright 2019 Edmunds.com, Inc.
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 */

package com.edmunds.tools.databricks.maven.model;

import com.edmunds.tools.databricks.maven.util.ObjectMapperUtils;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Simple POJO to pass Project and Environment properties to the {@link ClusterSettingsDTO}.
 */
public class ClusterEnvironmentDTO extends BaseEnvironmentDTO {

    /**
     * Don't use this - it's for jackson deserialization only!
     */
    public ClusterEnvironmentDTO() {
    }

    public ClusterEnvironmentDTO(MavenProject project, String environment, String databricksRepo, String databricksRepoKey, String prefixToStrip) {
        super(project, environment, databricksRepo, databricksRepoKey, prefixToStrip);
    }

    public static ClusterEnvironmentDTO loadClusterEnvironmentDTOFromFile(File clusterEnvironmentDTOFile) throws MojoExecutionException {
        if (clusterEnvironmentDTOFile == null) {
            throw new MojoExecutionException("clusterEnvironmentDTOFile must be set!");
        }
        try {
            String clusterEnvironmentDTOJson = FileUtils.readFileToString(clusterEnvironmentDTOFile, StandardCharsets.UTF_8);
            return ObjectMapperUtils.deserialize(clusterEnvironmentDTOJson, ClusterEnvironmentDTO.class);
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }
}