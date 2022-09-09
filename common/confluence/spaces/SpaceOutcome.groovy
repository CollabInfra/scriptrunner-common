package common.confluence.spaces

import com.atlassian.jira.util.WarningCollection
import com.atlassian.jira.util.ErrorCollection
import com.atlassian.jira.bc.ServiceOutcome

class SpaceOutcome implements ServiceOutcome {
    protected ErrorCollection errorCollection
    protected WarningCollection warningCollection
    protected SpaceInfo spaceInfo

    public SpaceOutcome(ErrorCollection errorCollection, WarningCollection warningCollection, SpaceInfo spaceInfo) {
        this.errorCollection = errorCollection
        this.warningCollection = warningCollection
        this.spaceInfo = spaceInfo
    }

    public SpaceInfo getReturnedValue() {
        return this.spaceInfo
    }

    public SpaceInfo get() {
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