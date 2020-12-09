/*
 *  Copyright 2018 Edmunds.com, Inc.
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

package com.edmunds.tools.databricks.maven;

import static org.apache.commons.io.FileUtils.readFileToString;
import static org.apache.commons.io.FilenameUtils.getBaseName;
import static org.apache.commons.io.FilenameUtils.getExtension;
import static org.apache.commons.lang3.StringUtils.substringAfter;

import com.edmunds.rest.databricks.DTO.workspace.ExportFormatDTO;
import com.edmunds.rest.databricks.DTO.workspace.LanguageDTO;
import com.edmunds.rest.databricks.DatabricksRestException;
import com.edmunds.rest.databricks.request.ImportWorkspaceRequest;
import com.edmunds.rest.databricks.service.WorkspaceService;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;


/**
 * A mojo that is responsible for taking care of importing notebooks into databricks.
 */
@Mojo(name = "import-workspace", requiresProject = true)
public class ImportWorkspaceMojo extends BaseWorkspaceMojo {

    /**
     * The number of threads to use during import.<br>
     */
    @Parameter(property = "import.threads")
    private int threads = 20;

    private ExecutorService executorService = Executors.newFixedThreadPool(threads);

    /**
     * Execute ImportWorkspaceMojo.
     *
     * @throws MojoExecutionException exception
     */
    public void execute() throws MojoExecutionException {
        try {
            //We have to validate before importing because it hasn't been done already.
            validateNotebooks(sourceWorkspacePath);
            importWorkspace(sourceWorkspacePath);
        } catch (DatabricksRestException | IOException e) {
            throw new MojoExecutionException(String.format("Could not execute workspace command: [%s]. Local Path: "
                    + "[%s] TO DB: [%s]", "IMPORT", packagedWorkspacePath, workspacePrefix), e);
        }
    }

    protected void importWorkspace(File workspacePath) throws IOException, DatabricksRestException {
        //We use packaged workspace dir to import notebooks into databricks.
        if (!workspacePath.exists()) {
            // Oh just no notebooks. Warn just incase user wasn't expecting this.
            getLog().warn(String.format("No notebooks found at [%s]", workspacePath.getPath()));
        } else {
            getLog().info("Working on copying [" + workspacePath + "] on [" + threads + "] threads.");
            Collection<File> files = FileUtils.listFiles(workspacePath,
                    new SuffixFileFilter(DATABRICKS_SOURCE_EXTENSIONS),
                    DirectoryFileFilter.DIRECTORY);

            for (File file : files) {
                // e.g. the path under the local root, not the full path to it
                String relativePath = substringAfter(file.getParentFile().getPath(), workspacePath.getPath())
                        .replaceAll("[.\\\\]", "/");
                String remoteFilePath = relativePath + "/" + getBaseName(file.getName());

                createRemoteDir(relativePath);

                LanguageDTO languageDTO = getLanguageDTO(file);
                getLog().info(
                        String.format("writing remote file: [%s] with source type: [%s]", remoteFilePath, languageDTO));

                String source = readFileToString(file, StandardCharsets.UTF_8);
                getLog().debug(String.format("file path: [%s] has source:%n%s", file.getPath(), source));

                ImportWorkspaceRequest importWorkspaceRequest =
                        new ImportWorkspaceRequest.ImportWorkspaceRequestBuilder(
                                remoteFilePath)
                                .withContent(source.getBytes(StandardCharsets.UTF_8))
                                .withFormat(ExportFormatDTO.SOURCE)
                                .withLanguage(languageDTO)
                                .withOverwrite(true)
                                .build();

                executorService.execute(() -> {
                    try {
                        getWorkspaceService().importWorkspace(importWorkspaceRequest);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }
    }

    private LanguageDTO getLanguageDTO(File file) {
        String extension = getExtension(file.getName()).toUpperCase();
        if (extension.equals("PY")) {
            return LanguageDTO.PYTHON;
        }
        return LanguageDTO.valueOf(extension);
    }

    private void createRemoteDir(String remoteDir) throws IOException, DatabricksRestException {
        getLog().info(String.format("creating dir if it does not already exist: [%s]", remoteDir));
        getWorkspaceService().mkdirs(remoteDir);
    }

    private WorkspaceService getWorkspaceService() {
        return getDatabricksServiceFactory().getWorkspaceService();
    }

    /**
     * NOTE: for unit testing only
     */
    void setThreads(int threads) {
        this.threads = threads;
    }
}