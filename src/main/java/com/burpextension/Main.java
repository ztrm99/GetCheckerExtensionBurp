package com.burpextension;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.extension.Extension;
import burp.api.montoya.extension.ExtensionUnloadingHandler;
import burp.api.montoya.http.Http;
import burp.api.montoya.logging.Logging;
import burp.api.montoya.proxy.Proxy;
import burp.api.montoya.scanner.Scanner;
import burp.api.montoya.scanner.audit.AuditIssueHandler;
import burp.api.montoya.scanner.audit.issues.AuditIssue;

public class Main implements BurpExtension{
    private Logging logging;

    @Override
    public void initialize(MontoyaApi api) {
        logging = api.logging();

        Http http = api.http();
        Proxy proxy = api.proxy();
        Extension extension = api.extension();
        Scanner scanner = api.scanner();

        // set extension name
        extension.setName("Get_Checker_URLENCODED");

        // register a new HTTP handler
        http.registerHttpHandler(new MyHttpHandler(api));

        // register new Proxy handlers
        proxy.registerRequestHandler(new MyProxyRequestHandler(api));
        proxy.registerResponseHandler(new MyProxyResponseHandler(api));

        // register a new Audit Issue handler
        scanner.registerAuditIssueHandler(new MyAuditIssueListenerHandler());
        // register a new extension unload handler
        extension.registerUnloadingHandler(new MyExtensionUnloadHandler());
    }

    private class MyAuditIssueListenerHandler implements AuditIssueHandler {
        @Override
        public void handleNewAuditIssue(AuditIssue auditIssue) {
            logging.logToOutput("New scan issue: " + auditIssue.name());
        }
    }

    private class MyExtensionUnloadHandler implements ExtensionUnloadingHandler {
        @Override
        public void extensionUnloaded() {
            logging.logToOutput("Extension was unloaded.");
        }
    }
}