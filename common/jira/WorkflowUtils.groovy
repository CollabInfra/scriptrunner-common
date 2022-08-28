package common.jira

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.workflow.JiraWorkflow
import com.atlassian.jira.bc.workflow.WorkflowSchemeService
import com.atlassian.jira.util.ErrorCollection
import com.atlassian.jira.util.SimpleErrorCollection
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.workflow.WorkflowScheme
import com.atlassian.jira.bc.workflow.WorkflowService

public class WorkflowUtils {

    WorkflowSchemeService worklowSchemeService = ComponentAccessor.getComponent(WorkflowSchemeService)
    WorkflowService worklowService = ComponentAccessor.getComponent(WorkflowService)

    public WorkflowUtils() {}

    class WorkflowInfo {
        JiraWorkflow workflow
        ArrayList<Map> postFunctions
        ArrayList<Map> validators

        public WorkflowInfo(JiraWorkflow workflow) {
            this.workflow = workflow
        }

        public ArrayList<Map> postFunctions
    }

    /**
     * Return a list of workflows that use validators and post-functions coming from plugins.
    */
    public List<WorkflowInfo> workflowsWithPlugins() {
        def details = [] as ArrayList<WorkflowInfo>

        ComponentAccessor.workflowManager.workflows.each { workflow ->
            def postFunctionsList = new ArrayList<Map>()
            def validatorsList = new ArrayList<Map>()

            def postFunctions = ComponentAccessor.workflowManager.getPostFunctionsForWorkflow(workflow)
            workflow.allActions.each { action ->
                workflow.getPostFunctionsForTransition(action).each { postFunction ->
                    def className = postFunction.getArgs().get("class.name") as String
                    if (!className.contains("com.atlassian.jira")) {
                        postFunctionsList.add([step: action.name, className: className])
                    }
                }
                action.validators.each { validator ->
                    def className = validator.getArgs().get("class.name") as String
                    if (!className.contains("com.atlassian.jira")) {
                        validatorsList.add([step: action.name, className: className])
                    }
                }
            }
            
            def workflowInfo = new WorkflowInfo(workflow)
            workflowInfo.setPostFunctions(postFunctionsList)
            workflowInfo.setValidators(validatorsList)
            details.add(workflowInfo)
        }

        return details
    }

    /**
     * Deleted unused workflows and workflow schemes
     *
     * @param admin A Jira global admin
     *
     * @return a collection of errors, if any
    */
    public ErrorCollection deleteUnusedWorkflowsAndSchemes(ApplicationUser admin) {
        def errorCollector = new SimpleErrorCollection()
        
        ComponentAccessor.workflowSchemeManager.getUnassociatedSchemes().each { scheme ->
            def workflowScheme = ComponentAccessor.workflowSchemeManager.getWorkflowSchemeObj(scheme.id)
            def outcome = worklowSchemeService.deleteWorkflowScheme(admin, workflowScheme)
            if (!outcome.isValid()) {
                errorCollector.addErrorCollection(outcome.errorCollection)
            }
        }
        
        ComponentAccessor.workflowManager.workflows.each { workflow ->
            if (!workflow.isSystemWorkflow()) {
                def schemesForWorkflow = ComponentAccessor.workflowSchemeManager.getSchemesForWorkflow(workflow)
                if (!schemesForWorkflow || schemesForWorkflow.isEmpty()) {
                    def outcome = worklowService.deleteWorkflow(admin, workflow.name)
                    if (!outcome.isValid()) {
                        errorCollector.addErrorCollection(outcome.errorCollection)
                    }
                }
            }
        }

        return errorCollector
    }
}