package common.Confluence

import com.atlassian.applinks.api.ApplicationLink
import com.atlassian.applinks.api.ApplicationLinkService
import com.atlassian.applinks.api.application.confluence.ConfluenceApplicationType
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.sal.api.net.Request 
import com.atlassian.sal.api.net.Response
import com.atlassian.sal.api.net.ResponseException
import com.atlassian.sal.api.net.ResponseHandler
import com.atlassian.applinks.api.ApplicationLinkRequestFactory
import org.springframework.lang.NonNull

class AppLink {

    static ApplicationLink getPrimaryConfluenceLink() {
        def applicationLinkService = ComponentAccessor.getComponent(ApplicationLinkService)
        final ApplicationLink conflLink = applicationLinkService.getPrimaryApplicationLink(ConfluenceApplicationType)
    }

    static ApplicationLinkRequestFactory authenticatedRequestFactory() {
        def confluenceLink = getPrimaryConfluenceLink()
        def authenticatedRequestFactory = confluenceLink.createImpersonatingAuthenticatedRequestFactory()
    }

    static Response doRequest(@NonNull String requestUri, String requestBody, @NonNull Request.MethodType httpVerb) {
        Response responseFromRequest = null
        authenticatedRequestFactory()
            .createRequest(httpVerb, requestUri)
            .addHeader("Content-Type", "application/json")
            .setRequestBody(requestBody)
            .execute(new ResponseHandler<Response>() {
                @Override
                void handle(Response response) throws ResponseException {
                    responseFromRequest = response
                }
            })
        return responseFromRequest
    }

}