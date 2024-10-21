package com.burpextension;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.Annotations;
import burp.api.montoya.core.HighlightColor;
import burp.api.montoya.http.Http;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.params.HttpParameter;
import burp.api.montoya.http.message.params.HttpParameterType;
import burp.api.montoya.http.message.params.ParsedHttpParameter;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.logging.Logging;
import burp.api.montoya.proxy.http.InterceptedRequest;
import burp.api.montoya.proxy.http.ProxyRequestHandler;
import burp.api.montoya.proxy.http.ProxyRequestReceivedAction;
import burp.api.montoya.proxy.http.ProxyRequestToBeSentAction;
import burp.api.montoya.scanner.audit.issues.AuditIssue;
import burp.api.montoya.scanner.audit.issues.AuditIssueConfidence;
import burp.api.montoya.scanner.audit.issues.AuditIssueSeverity;
import burp.api.montoya.sitemap.SiteMap;

import java.util.ArrayList;
import java.util.List;

public class MyProxyRequestHandler implements ProxyRequestHandler
{
    private final Logging logging;
    private final Http http;
    private final SiteMap sitemap;
    public MyProxyRequestHandler(MontoyaApi api)
    {
        logging = api.logging();
        http = api.http();
        sitemap = api.siteMap();
    }

    @Override
    public ProxyRequestReceivedAction handleRequestReceived(InterceptedRequest interceptedRequest) {
//        logging.logToOutput("Initial intercepted proxy request to " + interceptedRequest.httpService());
        Annotations annotations = interceptedRequest.annotations();

        // If the request is a post, log the body and add notes.
        if (isPost(interceptedRequest)) {
            if (hasContentTypeHeaderURLEncoded(interceptedRequest)) {
                annotations = annotations.withNotes("Request was a post").withHighlightColor(HighlightColor.RED);
                List<ParsedHttpParameter> listParsedParams = interceptedRequest.parameters(HttpParameterType.BODY);
                List<HttpParameter> listParams = new ArrayList<HttpParameter>();
                for( ParsedHttpParameter param : listParsedParams ) { listParams.add(HttpParameter.urlParameter(param.name(),param.value())); }
                HttpRequest modifiedRequest = interceptedRequest.withRemovedParameters(listParams).withMethod("GET").withAddedParameters(listParams);
//                logging.logToOutput("Modified Request: " + "\n" + modifiedRequest.toString());
                HttpRequestResponse getResponseModifiedRequest = http.sendRequest(modifiedRequest);
                if (is200(getResponseModifiedRequest)) {
                    logging.logToOutput("modified 200 response");
                    sitemap.add(
                            AuditIssue.auditIssue("ISSUE 200 GET Obtained",
                                    "Testing Detail",
                                    "Test Remediation" ,
                                    interceptedRequest.url(),
                                    AuditIssueSeverity.HIGH,
                                    AuditIssueConfidence.CERTAIN,
                                    "Test Background",
                                    "Test Remediation background",
                                    AuditIssueSeverity.HIGH,
                                    getResponseModifiedRequest)
                    );
                }
            }
            else {
                annotations = annotations.withNotes("Request was a post but not have URLEncoded Content Type header");
            }
        }

        return ProxyRequestReceivedAction.continueWith(interceptedRequest,annotations);
    }

    @Override
    public ProxyRequestToBeSentAction handleRequestToBeSent(InterceptedRequest interceptedRequest) {
        return ProxyRequestToBeSentAction.continueWith(interceptedRequest);
    }

    public static boolean isPost(InterceptedRequest httpRequestToBeSent) {
        return httpRequestToBeSent.method().equalsIgnoreCase("POST");
    }

    public static boolean is200(HttpRequestResponse httpRequestResponse) {
        return httpRequestResponse.response().statusCode() >= 200 | httpRequestResponse.response().statusCode() < 300;
    }

    public static boolean hasContentTypeHeaderURLEncoded(InterceptedRequest httpRequestToBeSent) {
        return httpRequestToBeSent.contentType().toString().contains("URL_ENCODED");
    }
}