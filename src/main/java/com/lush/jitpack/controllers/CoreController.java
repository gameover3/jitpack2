package com.lush.jitpack.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.lush.jitpack.models.EndpointDto;
import com.lush.jitpack.models.Endpoint;
import com.lush.jitpack.models.Response;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

/**
 * ActuatorController
 *
 * Class that receives health check for services and endpoint information requests.
 *
 * @author Is
 * @author Jelly
 */
@RestController
public class CoreController {

  /**
   * Define HttpServletRequest for get client request information.
   */
  @Autowired
  private HttpServletRequest request;

  /**
   * Define RestTemplate for get response information of uri.
   */
  private RestTemplate restTemplate;

  /**
   * Define Gson for json convert and parse.
   */
  @Autowired
  private Gson gson;

  /**
   * Define service name.
   */
  @Value("${service.name}")
  private String serviceName;

  /**
   * Define service type.
   */
  @Value("${service.type}")
  private String serviceType;

  /**
   * Define service scope.
   */
  @Value("${service.scope}")
  private String serviceScope;

  /**
   * Define service version.
   */
  @Value("${service.version}")
  private String serviceVersion;

  /**
   * Define InetAddress for get host name.
   * The hostname can be imported as a HttpServletRequest object,
   * but an issue occurs in the docker collector container that recognizes the hostname as ' java_http'.
   */
  private InetAddress ip;

  /**
   * Set to RestTemplate Bean.
   */
  public CoreController(RestTemplateBuilder restTemplateBuilder) {
    this.restTemplate = restTemplateBuilder.build();
  }

  /**
   * Set uri for rest api.
   *
   * @param context
   * @return String
   * @throws UnknownHostException
   */
  public String setUri(String context) throws UnknownHostException {
    ip = InetAddress.getLocalHost();
    return request.getScheme() + "://" + ip.getHostName() + ":" + request.getServerPort() + "/" + context;
  }

  /**
   * Check health.(application, database, redis)
   *
   * @return Response
   * @throws UnknownHostException
   */
  @GetMapping("/healthz")
  public Response healthz() throws UnknownHostException {
    Response response = new Response();

    // Get health data.
    String uri = setUri("health");
    ResponseEntity<String> health = restTemplate.getForEntity(uri, String.class);
    JsonObject healthBody = gson.fromJson(health.getBody(), JsonObject.class);
    String appStatus = healthBody.get("status").getAsString();

    // Check status of application.
    if (!"UP".equals(appStatus)) {
      response.setStatus("fail");
      response.setMessage("AppStatus is fail");
    }

    // Check status of database connection.
    if (health.getBody().contains("db")) {
      JsonObject temp = healthBody.get("details").getAsJsonObject();
      temp = temp.get("db").getAsJsonObject();
      String dbStatus = temp.get("status").getAsString();

      if (!"UP".equals(dbStatus)) {
        response.setStatus("fail");

        if (!"".equals(response.getMessage())) {
          response.setMessage(response.getMessage() + " and database status is fail" );
        } else {
          response.setMessage("Redis status is fail");
        }
      }
    }

    // Check status of redis connection.
    if (health.getBody().contains("redis")) {
      JsonObject temp = healthBody.get("details").getAsJsonObject();
      temp = temp.get("redis").getAsJsonObject();
      String redisStatus = temp.get("status").getAsString();

      if (!"UP".equals(redisStatus)) {
        response.setStatus("fail");

        if (!"".equals(response.getMessage())) {
          response.setMessage(response.getMessage() + " and redis status is fail" );
        } else {
          response.setMessage("Redis status is fail");
        }
      }
    }

    return response;
  }

  /**
   * Get endpoint list of api.
   *
   * @return ResponseEntity
   * @throws UnknownHostException
   */
  @GetMapping("/")
  public ResponseEntity endpoints() throws UnknownHostException {
    
    // Get endpoints data.
    String uri = setUri("mappings");
    ResponseEntity<JsonNode> data = restTemplate.getForEntity(uri, JsonNode.class);
    JsonNode dataBody = data.getBody().findPath("dispatcherServlet");

    // Find all method and uri
    List<JsonNode> methods = dataBody.findValues("methods");
    List<JsonNode> patterns = dataBody.findValues("patterns");

    String method = "";
    String pattern = "";
    String regex = "[\"\\[\\]]";
    List<EndpointDto.Info> endpointList = new ArrayList<EndpointDto.Info>();

    for (int idx=0; idx < methods.size(); idx++) {
      method = methods.get(idx).toString().replaceAll(regex, "");
      pattern = patterns.get(idx).toString().replaceAll(regex, "");

      if (method.length() == 0 || pattern.length() == 0 || "/health".equals(pattern) || "/mappings".equals(pattern)) {
        continue;
      }

      EndpointDto.Info endpoint = new EndpointDto.Info();
      endpoint.setMethod(method);
      endpoint.setUri(pattern);
      endpointList.add(endpoint);
    }

    // Set endpoints data.
    Endpoint endpoints = new Endpoint();
    endpoints.setService_name(serviceName);
    endpoints.setService_type(serviceType);
    endpoints.setService_scope(serviceScope);
    endpoints.setService_version(serviceVersion);
    endpoints.setEndpoints(endpointList);

    return new ResponseEntity(endpoints, HttpStatus.OK);
  }
}
