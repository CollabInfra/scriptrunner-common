package examples.rest

import com.atlassian.jira.issue.Issue
import com.atlassian.jira.config.StatusCategoryManager
import com.atlassian.jira.issue.status.category.StatusCategory
import com.atlassian.jira.component.ComponentAccessor
import com.onresolve.scriptrunner.runner.customisers.JiraAgileBean
import com.onresolve.scriptrunner.runner.rest.common.CustomEndpointDelegate
import groovy.transform.BaseScript
import com.atlassian.greenhopper.service.rapid.view.RapidViewService
import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.core.Response
import com.atlassian.greenhopper.service.sprint.SprintManager
import com.atlassian.greenhopper.service.sprint.SprintIssueService
import com.onresolve.scriptrunner.runner.customisers.WithPlugin
import com.atlassian.greenhopper.service.sprint.Sprint.State
import groovy.xml.MarkupBuilder
import javax.ws.rs.core.MediaType
import com.atlassian.greenhopper.service.sprint.SprintTimeRemainingService

/**
 * Return something similar to the Sprint Health gadget
 * Useful to display on a TV, like LG's smart TVs
 * Call it with <jira-base-url>/rest/scriptrunner/latest/custom/wallboard?rapidViewId=<id-of-board>
*/

@WithPlugin("com.pyxis.greenhopper.jira")

@JiraAgileBean
RapidViewService rapidViewService

@JiraAgileBean
SprintIssueService sprintIssueService

@JiraAgileBean
SprintManager sprintManager

@JiraAgileBean
SprintTimeRemainingService timeRemainingService

@BaseScript CustomEndpointDelegate delegate

wallboard(httpMethod: "GET") { MultivaluedMap queryParams, String body ->
    def rapidViewId = Long.valueOf(queryParams.getFirst('rapidViewId') as String)
    def currentUser = ComponentAccessor.userManager.getUserByName("admin")
    def rapidView = rapidViewService.getRapidView(currentUser, rapidViewId).get()
    def activeSprints = sprintManager.getSprintsForView(rapidView.id, EnumSet.of(State.ACTIVE))
    def storyPointsCf = ComponentAccessor.customFieldManager.getCustomFieldObjectByName("Story Points")
    def writer = new StringWriter()
    def builder = new MarkupBuilder(writer)
 
    builder.html {
       head {
            style(type:"text/css",
            """ 
                body, html {
                    background: #000;
                    color: #fff;
                    font-family: Arial, sans-serif;
                    margin: 0;
                    padding: 5px 0 0;
                    overflow: hidden;
                    font-size: xx-large;
                }

                a {
                    color: #ebf2f9;
                }

                div.slot {
                    margin: 0 0 20px;
                }

                div.slot.spacer-only {
                    margin: 0;
                }

                iframe {
                    border: none;
                    width: 100%;
                    height: 100%;
                    overflow: hidden;
                }

                div.wallboard-gadget {
                    width: 100%;
                }

                td iframe:last-of-type {
                    margin-bottom: 0;
                }

                .cyclable {
                    height: 100%;
                }

                table.wallframe {
                    border-collapse: collapse;
                    width: 100%;
                    font-size: xx-large;
                }

                table.wallframe td {
                    padding: 0 20px;
                    vertical-align: top;
                    width: 33%;
                }

                table.wallframe td.narrow {
                    width: 33%;
                }

                table.wallframe td.medium-width {
                    width: 50%;
                }

                table.wallframe td.wide {
                    width: 67%;
                }

                table.wallframe td.full-width {
                    width: 100%;
                }

                .top {
                    margin-top: 20px;
                }

                .bottom {
                    margin-bottom: 20px;
                }

                #blanket {
                    background: #000 url(/jira/s/-e20qwx/820007/1dlckms/5.0.0/_/download/resources/com.atlassian.jirawallboard.atlassian-wallboard-plugin:wallboard-resources/images/spinner_blanket.gif) no-repeat 50% 50%;
                    bottom: 0;
                    height: 100%;
                    opacity: 0.7;
                    -ms-filter: "progid:DXImageTransform.Microsoft.Alpha(Opacity=70)"; /* IE8 */
                    filter: alpha(opacity = 70); /* LTE IE7 */
                    position: absolute;
                    top: 0;
                    width: 100%;
                    z-index: 200;
                }

                tr.hasError td {
                    background-color: #fcc;
                }
                """
            )
        }
        div(id:"wallboards") {
            table(class:"wallframe") {
                activeSprints.get().each { sprint ->
                    def timeOutcome = timeRemainingService.getSprintTimeRemaining(currentUser, rapidView, sprint.id)                    
                    def totalStoryPoints = 0.0
                    def nonStartedPoints = 0.0
                    def inProgressPoints = 0.0
                    def donePoints = 0.0
                    def issues = sprintIssueService.getIssuesForSprint(currentUser, sprint).get()
                    def remainingIssues = [] as ArrayList<Issue>

                    issues.each { issue ->
                        def storyPoints = issue.getCustomFieldValue(storyPointsCf) as Double
                        if (storyPoints) {
                            totalStoryPoints += storyPoints
                            switch (issue.status.statusCategory.key) {
                                case StatusCategory.COMPLETE:
                                    donePoints += storyPoints
                                    break
                                case StatusCategory.IN_PROGRESS:
                                    inProgressPoints += storyPoints
                                    break
                                case StatusCategory.TO_DO:
                                    remainingIssues.add(issue)
                                    nonStartedPoints += storyPoints
                                    break
                                default:
                                    break
                            }
                        }
                    }

                    thead {
                        tr {
                            th sprint.name
                            th "${timeOutcome.getValue().days} days left"
                            th rapidView.name
                        }
                    }

                    tbody {
                        tr("line-height":"300px;") {
                            
                            td(style:"background-color: #42526e;", height: "300px;") {
                                text nonStartedPoints
                            }
                            td(style:"background-color: #0052cc;", height: "300px;") {
                                text inProgressPoints
                            }
                            td(style:"background-color: #00875a;", height: "300px;") {
                                text donePoints
                            }
                        }
                        remainingIssues.each { issue ->
                            tr {
                                td issue.summary
                                td issue.assignee.displayName
                                td issue.reporter.displayName
                            }
                        }
                    }
                }
            }  
        }
    }         
    return Response.ok(writer.toString(), MediaType.TEXT_HTML).build()
}

