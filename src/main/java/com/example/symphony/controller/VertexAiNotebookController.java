package com.example.symphony.controller;

import com.example.symphony.service.VertexAiNotebookService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

import com.example.symphony.dto.CreateInstanceDto;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api")
public class VertexAiNotebookController {

    private final VertexAiNotebookService notebookService;

    public VertexAiNotebookController(VertexAiNotebookService notebookService) {
        this.notebookService = notebookService;
    }

    @GetMapping("/notebooks")
    public List<String> listNotebooks() throws IOException {
        return notebookService.listNotebookInstances();
    }

    @PostMapping("/notebooks")
    public String createNotebook(@RequestBody CreateInstanceDto createInstanceDto) throws IOException, ExecutionException, InterruptedException {
        return notebookService.createNotebookInstance(createInstanceDto.getInstanceId());
    }

    @GetMapping("/notebooks/{instanceId}/proxy")
    public String getNotebookProxy(@PathVariable String instanceId) throws IOException {
        return notebookService.getNotebookProxyUri(instanceId);
    }

    @PostMapping("/notebooks/{instanceId}/start")
    public String startNotebook(@PathVariable String instanceId) throws IOException, InterruptedException, ExecutionException {
        return notebookService.startNotebookInstance(instanceId);
    }

    @PostMapping("/notebooks/{instanceId}/stop")
    public String stopNotebook(@PathVariable String instanceId) throws IOException, InterruptedException, ExecutionException {
        return notebookService.stopNotebookInstance(instanceId);
    }

    @DeleteMapping("/notebooks/{instanceId}")
    public String deleteNotebook(@PathVariable String instanceId) throws IOException, InterruptedException, ExecutionException {
        return notebookService.deleteNotebookInstance(instanceId);
    }
}
