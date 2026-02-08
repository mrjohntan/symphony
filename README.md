# Symphony - Vertex AI Workbench Manager

Symphony is a Spring Boot application designed to programmatically manage Google Cloud Vertex AI Workbench instances (User-Managed Notebooks). It provides a RESTful API to list, create, start, stop, and delete instances, as well as retrieve their JupyterLab proxy URIs.

## Features

- **List Instances**: Retrieve all Workbench instances in the configured region.
- **Create Instance**: Create a new instance with:
    - **Machine Type**: `e2-standard-4`
    - **Image**: Custom Container (`gcr.io/deeplearning-platform-release/workbench-container:latest`)
    - **Network**: Internal IP Only (Secure default)
- **Lifecycle Management**: Start, Stop, and Delete instances.
- **Proxy URI**: Fetch the direct HTTPS link to the JupyterLab interface.
- **Native Implementation**: Uses Google Native Java Client Libraries (`google-cloud-notebooks`, `google-auth-library`).

## Prerequisites

- **Java 21** or higher.
- **Google Cloud Platform (GCP) Project** with:
    - `Notebooks API` enabled.
    - A Service Account with `Notebooks Admin` roles.
    - A JSON key file for the Service Account.

## Configuration

1.  **Service Account Key**: Place your JSON key file in the project root (e.g., `team-485203-aa37d5704a91.json`). *Note: This file is git-ignored for security.*
2.  **Properties**: Configure `src/main/resources/application.properties`:

```properties
gcp.project-id=your-project-id
gcp.location=asia-southeast1-a
gcp.credentials.path=./your-key-file.json
```

## Running the Application

To start the application:

```bash
./gradlew bootRun
```

The server will start on `http://localhost:8080`.

## API Usage

### 1. List Instances
```bash
curl -s http://localhost:8080/api/notebooks
```

### 2. Create Instance
```bash
curl -X POST -H "Content-Type: application/json" \
     -d '{"instanceId": "my-new-notebook"}' \
     http://localhost:8080/api/notebooks
```

### 3. Get Proxy URI (Login URL)
```bash
curl http://localhost:8080/api/notebooks/my-new-notebook/proxy
```

### 4. Stop Instance
```bash
curl -X POST http://localhost:8080/api/notebooks/my-new-notebook/stop
```

### 5. Start Instance
```bash
curl -X POST http://localhost:8080/api/notebooks/my-new-notebook/start
```

### 6. Delete Instance
```bash
curl -X DELETE http://localhost:8080/api/notebooks/my-new-notebook
```

## API Documentation

Swagger UI is available at:
http://localhost:8080/swagger-ui.html
