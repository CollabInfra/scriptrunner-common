package common.confluence.dc.spaces

import com.atlassian.jira.util.WarningCollection
import com.atlassian.jira.util.ErrorCollection
import com.atlassian.jira.bc.ServiceOutcome

class SpaceOutcome implements ServiceOutcome {
    protected ErrorCollection errorCollection
    protected WarningCollection warningCollection
    protected Space spaceInfo

    public SpaceOutcome(ErrorCollection errorCollection, WarningCollection warningCollection, Space spaceInfo) {
        this.errorCollection = errorCollection
        this.warningCollection = warningCollection
        this.spaceInfo = spaceInfo
    }

    public Space getReturnedValue() {
        return this.spaceInfo
    }

    public Space get() {
        return this.spaceInfo
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