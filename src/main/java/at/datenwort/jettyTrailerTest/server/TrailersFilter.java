package at.datenwort.jettyTrailerTest.server;

import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.server.Response;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

public class TrailersFilter implements Filter
{
    @Override
    public void init(FilterConfig filterConfig) throws ServletException
    {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
    {
        HttpFields responseTrailers = new HttpFields();
        long start = System.currentTimeMillis();
        try
        {
            ((Response) response).setTrailers(() -> responseTrailers);

            chain.doFilter(request, response);
        }
        finally
        {
            responseTrailers.add("Server-Timing", "total;dur=" + (System.currentTimeMillis() - start));
        }
    }

    @Override
    public void destroy()
    {
    }
}
