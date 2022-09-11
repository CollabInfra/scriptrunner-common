package common.jira.dc

import com.atlassian.jira.workflow.JiraWorkflow

class WorkflowInfo {
    JiraWorkflow workflow
    ArrayList<Map> postFunctions
    ArrayList<Map> validators

    public WorkflowInfo(JiraWorkflow workflow) {
        this.workflow = workflow
    }

    public ArrayList<Map> postFunctions
}