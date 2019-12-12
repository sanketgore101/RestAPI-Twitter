package com.twitter.config;
/**
 * @author Sanket Gore
 *
 */
import javax.ws.rs.ApplicationPath;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.wadl.internal.WadlResource;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import com.twitter.authorization.AuthorizationFilter;
import com.twitter.controller.rest.TweetRESTService;
import com.twitter.controller.rest.UserRESTService;

import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.listing.ApiListingResource;
import io.swagger.jaxrs.listing.SwaggerSerializers;

@Configuration
@Component
@ApplicationPath("/twitter")
public class JerseyConfig extends ResourceConfig {
    public JerseyConfig() {
        registerEndpoints();
        configureSwagger();
        register(AuthorizationFilter.class);
    }

    private void registerEndpoints() {
		register(WadlResource.class);
		register(UserRESTService.class);
		register(TweetRESTService.class);
    }
    
    private void configureSwagger() {
        BeanConfig beanConfig = new BeanConfig();
        beanConfig.setTitle("Twitter-Like service");
        beanConfig.setVersion(UserRESTService.API_VERSION);
        beanConfig.setSchemes(new String[]{"http"});
        beanConfig.setHost("localhost:8080");
        beanConfig.setBasePath("/twitter");
        beanConfig.setResourcePackage(UserRESTService.class.getPackage().getName());
        beanConfig.setPrettyPrint(true);
        beanConfig.setScan(false);

        register(ApiListingResource.class);
        register(SwaggerSerializers.class );
    }
}
