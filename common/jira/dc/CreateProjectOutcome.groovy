package common.jira.dc

import com.atlassian.jira.util.ErrorCollection
import com.atlassian.jira.project.Project
import com.atlassian.jira.util.WarningCollection
import com.atlassian.jira.bc.ServiceOutcome

class CreateProjectOutcome implements ServiceOutcome {
    protected ErrorCollection errorCollection
    protected WarningCollection warningCollection
    protected Project project

    CreateProjectOutcome(ErrorCollection errorCollection, WarningCollection warningCollection, Project project) {
        this.errorCollection = errorCollection
        this.warningCollection = warningCollection
        this.project = project
    }

    Project getReturnedValue() {
        return this.project
    }

    Project get() {
        return this.project
    }

    ErrorCollection getErrorCollection() {
        return this.errorCollection
    }

    WarningCollection getWarningCollection() {
        return this.warningCollection
    }

    boolean isValid() {
        return (errorCollection && !errorCollection.hasAnyErrors())? true: false
    }

    boolean hasWarnings() {
        return (warningCollection && warningCollection.hasAnyWarnings())? true: false
    }
}