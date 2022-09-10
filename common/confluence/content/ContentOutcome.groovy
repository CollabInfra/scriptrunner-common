package common.confluence.content

import com.atlassian.jira.util.WarningCollection
import com.atlassian.jira.util.ErrorCollection
import com.atlassian.jira.bc.ServiceOutcome

class ContentOutcome implements ServiceOutcome {
    protected ErrorCollection errorCollection
    protected WarningCollection warningCollection
    protected Content contentDetails

    public ContentOutcome(ErrorCollection errorCollection, WarningCollection warningCollection, Content contentDetails) {
        this.errorCollection = errorCollection
        this.warningCollection = warningCollection
        this.contentDetails = contentDetails
    }

    public Content getReturnedValue() {
        return this.contentDetails
    }

    public Content get() {
        return this.contentDetails
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