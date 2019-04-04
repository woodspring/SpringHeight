package com.tts.web;

import java.io.File;

import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import com.tts.web.config.WebMvcConfig;


public class EmbeddedJetty {

    private static final Logger logger = LoggerFactory.getLogger(EmbeddedJetty.class);
    private static final String CONTEXT_PATH = "/";
    private static final String MAPPING_URL = "/*";

    public void startJetty(int port, ApplicationContext ctx) throws Exception {
    	SslContextFactory sslContextFactory = null;
    	HttpConfiguration https_config = null;
    	ServerConnector https = null;
    	String keystoreFilePath = Thread.currentThread().getContextClassLoader().getResource("keystore").getFile();
    	
    	File f = new File(keystoreFilePath);
    	
    	if ( f.exists() ) {
	        sslContextFactory = new SslContextFactory();
	        sslContextFactory.setKeyStorePath(keystoreFilePath);
	        sslContextFactory.setKeyStorePassword("ticktrade");
	        sslContextFactory.setKeyManagerPassword("ticktrade");
    	}
    	
    	Server server = new Server(port);
        
    	HttpConfiguration http_config = new HttpConfiguration();
        http_config.setSecureScheme("https");
        http_config.setSecurePort(8443);
        http_config.setOutputBufferSize(32768);
        
        if ( f.exists() ) {
	        https_config = new HttpConfiguration(http_config);
	        SecureRequestCustomizer src = new SecureRequestCustomizer();
	        src.setStsMaxAge(2000);
	        src.setStsIncludeSubDomains(true);
	        https_config.addCustomizer(src);
        }
        
        ServerConnector http = new ServerConnector(server,
                new HttpConnectionFactory(http_config));
        http.setPort(port);
        http.setIdleTimeout(30000);
    	
        if ( f.exists()) {
	        https = new ServerConnector(server,
	                new SslConnectionFactory(sslContextFactory,HttpVersion.HTTP_1_1.asString()),
	                    new HttpConnectionFactory(https_config));
	        https.setPort(8443);
	        https.setIdleTimeout(500000);
        }
        
        if ( f.exists()) {
        	server.setConnectors(new Connector[] { http, https });    
        } else {
        	server.setConnectors(new Connector[] { http });    
        }
        
        logger.debug("Starting server at port {}", port);

        
        AnnotationConfigWebApplicationContext webContext = new AnnotationConfigWebApplicationContext();
        if ( ctx != null ) {
        	webContext.setParent(ctx);
        }
        webContext.setConfigLocation(WebMvcConfig.class.getPackage().getName());
        ServletContextHandler contextHandler = new ServletContextHandler();
        contextHandler.setErrorHandler(new ErrorHandler());
        contextHandler.setContextPath(CONTEXT_PATH);
        contextHandler.addServlet(new ServletHolder(new DispatcherServlet(webContext)), MAPPING_URL);
        contextHandler.addEventListener(new ContextLoaderListener(webContext));
        contextHandler.setResourceBase(new ClassPathResource("webapp").getURI().toString());
        
        server.setHandler(contextHandler);
        server.start();
        logger.info("Server started at port {}", port);
        server.join();
    }



}
