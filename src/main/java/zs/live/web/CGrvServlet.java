package zs.live.web;

import groovy.servlet.GroovyServlet;
import groovy.servlet.ServletBinding;
import groovy.util.ResourceException;

import java.io.File;
import java.io.IOException;
import java.net.URLConnection;
import java.util.Map;

public class CGrvServlet extends GroovyServlet {
    @Override
    public URLConnection getResourceConnection(String name) throws ResourceException {
        name = removeNamePrefix(name).replace('\\', '/');
        File file = name.startsWith("file:") ? new File(name.substring(5)) : new File("./groovy", name);
        if (file.isFile())
            try {
                return file.toURI().toURL().openConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }
        return super.getResourceConnection(name);
    }

    @Override
    protected void setVariables(ServletBinding binding) {
        super.setVariables(binding);
        Map<String, Object> headers = (Map<String, Object>) binding.getVariable("headers");
        headers.put("User-Agent", headers.get("user-agent"));
    }
}
