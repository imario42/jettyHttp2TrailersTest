package at.datenwort.jettyTrailerTest.server;

import com.codahale.metrics.health.jvm.ThreadDeadlockHealthCheck;
import io.dropwizard.Application;
import io.dropwizard.configuration.ResourceConfigurationSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.eclipse.jetty.servlet.FilterHolder;

import javax.servlet.DispatcherType;
import java.util.EnumSet;

public class ServerMain extends Application<ServerConfiguration>
{

    @Override
    public void initialize(Bootstrap<ServerConfiguration> bootstrap)
    {
        super.initialize(bootstrap);

        bootstrap.setConfigurationSourceProvider(new ResourceConfigurationSourceProvider());
    }

    @Override
    public void run(ServerConfiguration configuration, Environment environment) throws Exception
    {
        environment.jersey().register(new SimpleService());
        environment.healthChecks().register("deadlock", new ThreadDeadlockHealthCheck());

        FilterHolder metricsFilter = new FilterHolder(new TrailersFilter());
        environment.getApplicationContext().addFilter(metricsFilter, "/*", EnumSet.of(DispatcherType.REQUEST));
    }

    public static void main(String[] args) throws Exception
    {
        ServerMain server = new ServerMain();
        server.run("server", "Server.yml");
    }
}
