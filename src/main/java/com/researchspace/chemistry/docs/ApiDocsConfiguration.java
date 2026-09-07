package com.researchspace.chemistry.docs;

import com.scalar.maven.webmvc.ScalarWebMvcAutoConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
// Springdoc suppresses Scalar auto-configuration, even with its API-only starter.
@Import(ScalarWebMvcAutoConfiguration.class)
public class ApiDocsConfiguration implements WebMvcConfigurer {
  private final String docsPath;

  public ApiDocsConfiguration(@Value("${scalar.path}") String docsPath) {
    this.docsPath = docsPath;
  }

  @Override
  public void addViewControllers(ViewControllerRegistry registry) {
    registry.addRedirectViewController("/swagger-ui.html", docsPath);
    registry.addRedirectViewController("/swagger-ui/index.html", docsPath);
    registry.addRedirectViewController(docsPath + "/", docsPath);
  }
}
