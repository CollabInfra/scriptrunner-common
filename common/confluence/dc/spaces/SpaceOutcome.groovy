package common.confluence.dc.spaces

import com.atlassian.jira.util.WarningCollection
import com.atlassian.jira.util.ErrorCollection
import com.atlassian.jira.bc.ServiceOutcome

class SpaceOutcome implements ServiceOutcome {
    protected ErrorCollection errorCollection
    protected WarningCollection warningCollection
    protected Space spaceInfo

    SpaceOutcome(ErrorCollection errorCollection, WarningCollection warningCollection, Space spaceInfo) {
        this.errorCollection = errorCollection
        this.warningCollection = warningCollection
        this.spaceInfo = spaceInfo
    }

    Space getReturnedValue() {
        return this.spaceInfo
    }

    Space get() {
        return this.spaceInfo
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