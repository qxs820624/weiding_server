package zs.live.web;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CharacterEncodingFilter;
import zs.live.utils.Speed;

import java.util.Arrays;

@Configuration
public class CfgWEB {
    @Bean
    FilterRegistrationBean speed() {
        FilterRegistrationBean frb = new FilterRegistrationBean();
        frb.setFilter(new Speed());
        frb.setUrlPatterns(Arrays.asList("/*"));
        return frb;
    }
    @Bean
    FilterRegistrationBean encoding() {
        CharacterEncodingFilter f = new CharacterEncodingFilter();
        f.setEncoding("UTF-8");
        f.setForceEncoding(true);
        FilterRegistrationBean frb = new FilterRegistrationBean();
        frb.setFilter(f);
        frb.setUrlPatterns(Arrays.asList("/*"));
        return frb;
    }

    @Bean
    ServletRegistrationBean groovy() {
        ServletRegistrationBean srb = new ServletRegistrationBean(new CGrvServlet(), "*.groovy", "/groovy/*");
        srb.setLoadOnStartup(1);
        return srb;
    }

    @Bean
    ServletRegistrationBean videocallback() {
        //由于视频拉取的回调数据在groovy中无法正常解析，固使用servlet的方式
        ServletRegistrationBean srb = new ServletRegistrationBean(new VideoCallBackServlet(), "/live/videocallback");
        srb.setLoadOnStartup(1);
        return srb;
    }
}
