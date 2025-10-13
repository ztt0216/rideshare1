package com.unimelb.rideshare;

import com.unimelb.rideshare.presentation.ConsoleApplication;
import com.unimelb.rideshare.presentation.WebServer;

import java.io.IOException;
import java.util.Arrays;

/**
 * Entry point for the RideShare reference implementation built for SWEN90007.
 */
public final class App {
    private App() {
        // no-op
    }

    public static void main(String[] args) throws IOException {
        ApplicationContext context = ApplicationContext.bootstrap();
        WebServer webServer = new WebServer(context);
        webServer.start();

        boolean runConsole = Arrays.asList(args).contains("--console");
        if (runConsole) {
            ConsoleApplication application = new ConsoleApplication(context);
            application.run();
        } else {
            System.out.println("Console UI not started (run with --console to enable). Press Ctrl+C to stop the server.");
        }
    }
}