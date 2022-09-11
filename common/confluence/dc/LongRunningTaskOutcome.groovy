package common.confluence.dc

import com.atlassian.jira.util.WarningCollection
import com.atlassian.jira.util.ErrorCollection
import com.atlassian.jira.bc.ServiceOutcome

class LongRunningTaskOutcome implements ServiceOutcome {
    protected ErrorCollection errorCollection
    protected WarningCollection warningCollection
    protected Boolean isComplete

    public LongRunningTaskOutcome(ErrorCollection errorCollection, WarningCollection warningCollection, Boolean isComplete) {
        this.errorCollection = errorCollection
        this.warningCollection = warningCollection
    }

    public Boolean getReturnedValue() {
        return this.isComplete
    }

    public Boolean get() {
        return this.isComplete
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