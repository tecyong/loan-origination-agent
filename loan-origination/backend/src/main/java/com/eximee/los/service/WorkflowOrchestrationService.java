package com.eximee.los.service;

import org.eximeebpms.bpm.engine.RuntimeService;
import org.eximeebpms.bpm.engine.TaskService;
import org.eximeebpms.bpm.engine.runtime.ProcessInstance;
import org.eximeebpms.bpm.engine.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class WorkflowOrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowOrchestrationService.class);

    private final RuntimeService runtimeService;
    private final TaskService taskService;

    public WorkflowOrchestrationService(RuntimeService runtimeService, TaskService taskService) {
        this.runtimeService = runtimeService;
        this.taskService = taskService;
    }

    public String startLoanOriginationWorkflow(Long applicationId, String applicantEmail) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("applicationId", applicationId);
        variables.put("applicantUsername", applicantEmail);

        log.info("Starting EximeeBPMS process 'loanOriginationProcess' for applicationId: {}", applicationId);
        ProcessInstance instance = runtimeService.startProcessInstanceByKey("loanOriginationProcess", String.valueOf(applicationId), variables);
        log.info("EximeeBPMS Process Instance started with ID: {}", instance.getId());
        return instance.getId();
    }

    public void completeUnderwriterReview(String processInstanceId, String decision, BigDecimal approvedAmount, String reason) {
        Task task = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .taskDefinitionKey("Task_UnderwriterReview")
                .singleResult();

        if (task == null) {
            log.warn("No active Task_UnderwriterReview found for processInstanceId: {}", processInstanceId);
            return;
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("decision", decision);
        if (approvedAmount != null) {
            variables.put("approvedAmount", approvedAmount.doubleValue());
        }
        if (reason != null) {
            variables.put("decisionReason", reason);
        }

        log.info("Completing Underwriter task {} with decision: {}", task.getId(), decision);
        taskService.complete(task.getId(), variables);
    }

    public void completeApplicantRevision(String processInstanceId) {
        Task task = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .taskDefinitionKey("Task_ApplicantRevise")
                .singleResult();

        if (task == null) {
            log.warn("No active Task_ApplicantRevise found for processInstanceId: {}", processInstanceId);
            return;
        }

        log.info("Completing Applicant revision task {}", task.getId());
        taskService.complete(task.getId());
    }
}
