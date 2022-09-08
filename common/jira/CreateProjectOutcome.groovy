package common.jira

import com.atlassian.jira.util.ErrorCollection
import com.atlassian.jira.project.Project
import com.atlassian.jira.util.WarningCollection
import com.atlassian.jira.bc.ServiceOutcome

class CreateProjectOutcome implements ServiceOutcome {
    protected ErrorCollection errorCollection
    protected WarningCollection warningCollection
    protected Project project

    public CreateProjectOutcome(ErrorCollection errorCollection, WarningCollection warningCollection, Project project) {
        this.errorCollection = errorCollection
        this.warningCollection = warningCollection
        this.project = project
    }

    public Project getReturnedValue() {
        return this.project
    }

    public Project get() {
        return this.project
    }

    public ErrorCollection getErrorCollection() {
        return this.errorCollection
    }

    public WarningCollection getWarningCollection() {
        return this.warningCollection
    }

    public boolean isValid() {
        return (errorCollection && !errorCollection.hasAnyErrors())? true: false
    }

    public boolean hasWarnings() {
        return (warningCollection && warningCollection.hasAnyWarnings())? true: false
    }
}