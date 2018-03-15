package zs.live.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.ErrorAttributes;
import org.springframework.boot.autoconfigure.web.ErrorController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.ServletRequestAttributes;
import zs.live.ApiException;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class SYController implements ErrorController {
    @RequestMapping(value = "${error.path:/error}")
    public Map error(HttpServletRequest request) {
        int status = 500;
        String message = null;

        RequestAttributes requestAttributes = new ServletRequestAttributes(request);
        //noinspection ThrowableResultOfMethodCallIgnored
        Throwable throwable = errorAttributes.getError(requestAttributes);
        //noinspection StatementWithEmptyBody
        if (throwable == null) {
        } else if (throwable instanceof ApiException) {
            ApiException exception = (ApiException) throwable;
            status = exception.getStatus();
            message = exception.getDescription();
        } else {
            for (Throwable t = throwable; t != null; t = t.getCause())
                if (t.getMessage() != null)
                    message = t.getMessage();
        }

        if (message == null || message.length() < 1)
            message = "未知错误";
        LinkedHashMap<String, Object> h = new LinkedHashMap<>(), m = new LinkedHashMap<>();
        h.put("status", status);
        h.put("desc", message);
        m.put("head", h);
        m.put("body", throwable == null ? message : throwable.getMessage());
        return m;
    }

    @Autowired
    private ErrorAttributes errorAttributes;
    @Value("${error.path:/error}")
    private String errorPath;

    @Override
    public String getErrorPath() {
        return this.errorPath;
    }
}
