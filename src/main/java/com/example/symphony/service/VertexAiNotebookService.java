package com.example.symphony.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.notebooks.v2.ContainerImage;
import com.google.cloud.notebooks.v2.CreateInstanceRequest;
import com.google.cloud.notebooks.v2.GceSetup;
import com.google.cloud.notebooks.v2.Instance;
import com.google.cloud.notebooks.v2.ListInstancesRequest;
import com.google.cloud.notebooks.v2.LocationName;
import com.google.cloud.notebooks.v2.NetworkInterface;
import com.google.cloud.notebooks.v2.NotebookServiceClient;
import com.google.cloud.notebooks.v2.NotebookServiceSettings;
import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.notebooks.v2.OperationMetadata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
public class VertexAiNotebookService {

    private static final Logger logger = LoggerFactory.getLogger(VertexAiNotebookService.class);

    private final String projectId;
    private final String location;
    private final String credentialsPath;

    public VertexAiNotebookService(
            @Value("${gcp.project-id}") String projectId,
            @Value("${gcp.location}") String location,
            @Value("${gcp.credentials.path}") String credentialsPath) {
        this.projectId = projectId;
        this.location = location;
        this.credentialsPath = credentialsPath;
    }

    /**
     * Helper method to construct the NotebookServiceSettings with manual credentials.
     *
     * @return The configured NotebookServiceSettings.
     * @throws IOException If the credentials file cannot be read.
     */
    private NotebookServiceSettings getSettings() throws IOException {
        GoogleCredentials credentials;
        try (FileInputStream credentialsStream = new FileInputStream(credentialsPath)) {
            credentials = GoogleCredentials.fromStream(credentialsStream);
        }
        
        return NotebookServiceSettings.newBuilder()
                .setCredentialsProvider(() -> credentials)
                .build();
    }

    /**
     * Lists all Vertex AI Workbench instances in the configured project and location.
     *
     * @return A list of instance names (fully qualified paths).
     * @throws IOException If the API call fails.
     */
    public List<String> listNotebookInstances() throws IOException {
        try (NotebookServiceClient notebookServiceClient = NotebookServiceClient.create(getSettings())) {
            LocationName parent = LocationName.of(projectId, location);
            logger.info("Listing instances in: {}", parent);
            
            List<String> instanceNames = new ArrayList<>();
            for (Instance instance : notebookServiceClient.listInstances(parent).iterateAll()) {
                logger.info("Found instance: {}", instance.getName());
                instanceNames.add(instance.getName());
            }
            logger.info("Total instances found: {}", instanceNames.size());
            return instanceNames;
        }
    }

    /**
     * Creates a new Vertex AI Workbench instance (User-Managed Notebook).
     * <p>
     * Configurations:
     * - Machine Type: e2-standard-4
     * - Image: gcr.io/deeplearning-platform-release/workbench-container:latest
     * - Network: Internal IP only (No public IP)
     *
     * @param instanceId The unique ID for the new instance (e.g., "my-notebook-1").
     * @return The Operation Name (can be used to track the long-running creation process).
     * @throws IOException           If the API call fails.
     * @throws ExecutionException    If the async operation fails.
     * @throws InterruptedException  If the thread is interrupted.
     */
    public String createNotebookInstance(String instanceId) throws IOException, ExecutionException, InterruptedException {
        try (NotebookServiceClient notebookServiceClient = NotebookServiceClient.create(getSettings())) {
            LocationName locationName = LocationName.of(projectId, location);
            String parent = locationName.toString();

            // Define the Container Image (based on user's console config)
            ContainerImage containerImage = ContainerImage.newBuilder()
                    .setRepository("gcr.io/deeplearning-platform-release/workbench-container")
                    .setTag("latest")
                    .build();

            // Configure Network (Internal IP Only)
            NetworkInterface networkInterface = NetworkInterface.newBuilder()
                    .setNetwork("global/networks/default") 
                    .build();

            // Define GCE Setup
            GceSetup gceSetup = GceSetup.newBuilder()
                    .setMachineType("e2-standard-4") // Verified from console
                    .setContainerImage(containerImage)
                    .addNetworkInterfaces(networkInterface)
                    .setDisablePublicIp(true) // Confirmed internal IP only
                    .build();

            Instance instance = Instance.newBuilder()
                    .setGceSetup(gceSetup)
                    .build();

            CreateInstanceRequest request = CreateInstanceRequest.newBuilder()
                    .setParent(parent)
                    .setInstanceId(instanceId)
                    .setInstance(instance)
                    .build();

            logger.info("Creating instance: {} in {}", instanceId, parent);
            OperationFuture<Instance, OperationMetadata> operation = notebookServiceClient.createInstanceAsync(request);
            
            return operation.getName();
        }
    }

    /**
     * Retrieves the Proxy URI (JupyterLab URL) for a specific instance.
     *
     * @param instanceId The ID of the instance.
     * @return The HTTPS URL to access the JupyterLab interface.
     * @throws IOException If the API call fails.
     */
    public String getNotebookProxyUri(String instanceId) throws IOException {
        try (NotebookServiceClient notebookServiceClient = NotebookServiceClient.create(getSettings())) {
            LocationName locationName = LocationName.of(projectId, location);
            String name = locationName.toString() + "/instances/" + instanceId;
            
            logger.info("Fetching details for instance: {}", name);
            Instance instance = notebookServiceClient.getInstance(name);
            return "https://" + instance.getProxyUri();
        }
    }

    /**
     * Starts a stopped Workbench instance.
     *
     * @param instanceId The ID of the instance to start.
     * @return The Operation Name.
     * @throws IOException           If the API call fails.
     * @throws InterruptedException  If the thread is interrupted.
     * @throws ExecutionException    If the async operation fails.
     */
    public String startNotebookInstance(String instanceId) throws IOException, InterruptedException, ExecutionException {
        try (NotebookServiceClient notebookServiceClient = NotebookServiceClient.create(getSettings())) {
             LocationName locationName = LocationName.of(projectId, location);
             String name = locationName.toString() + "/instances/" + instanceId;
             
             com.google.cloud.notebooks.v2.StartInstanceRequest request = 
                 com.google.cloud.notebooks.v2.StartInstanceRequest.newBuilder().setName(name).build();
             
             logger.info("Starting instance: {}", name);
             OperationFuture<Instance, OperationMetadata> operation = notebookServiceClient.startInstanceAsync(request);
             return operation.getName();
        }
    }

    /**
     * Stops a running Workbench instance.
     *
     * @param instanceId The ID of the instance to stop.
     * @return The Operation Name.
     * @throws IOException           If the API call fails.
     * @throws InterruptedException  If the thread is interrupted.
     * @throws ExecutionException    If the async operation fails.
     */
    public String stopNotebookInstance(String instanceId) throws IOException, InterruptedException, ExecutionException {
        try (NotebookServiceClient notebookServiceClient = NotebookServiceClient.create(getSettings())) {
             LocationName locationName = LocationName.of(projectId, location);
             String name = locationName.toString() + "/instances/" + instanceId;

             com.google.cloud.notebooks.v2.StopInstanceRequest request = 
                 com.google.cloud.notebooks.v2.StopInstanceRequest.newBuilder().setName(name).build();

             logger.info("Stopping instance: {}", name);
             OperationFuture<Instance, OperationMetadata> operation = notebookServiceClient.stopInstanceAsync(request);
             return operation.getName();
        }
    }

    /**
     * Deletes a Workbench instance.
     *
     * @param instanceId The ID of the instance to delete.
     * @return The Operation Name.
     * @throws IOException           If the API call fails.
     * @throws InterruptedException  If the thread is interrupted.
     * @throws ExecutionException    If the async operation fails.
     */
    public String deleteNotebookInstance(String instanceId) throws IOException, InterruptedException, ExecutionException {
        try (NotebookServiceClient notebookServiceClient = NotebookServiceClient.create(getSettings())) {
             LocationName locationName = LocationName.of(projectId, location);
             String name = locationName.toString() + "/instances/" + instanceId;

             com.google.cloud.notebooks.v2.DeleteInstanceRequest request = 
                 com.google.cloud.notebooks.v2.DeleteInstanceRequest.newBuilder().setName(name).build();

             logger.info("Deleting instance: {}", name);
             OperationFuture<com.google.protobuf.Empty, OperationMetadata> operation = notebookServiceClient.deleteInstanceAsync(request);
             return operation.getName();
        }
    }
}
