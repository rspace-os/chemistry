package com.researchspace.chemistry.docs;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = "server.servlet.context-path=/chemistry-service")
public class ApiDocsContextPathIT extends ApiDocsIT {}
