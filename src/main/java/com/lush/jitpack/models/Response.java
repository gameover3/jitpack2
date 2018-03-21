package com.lush.jitpack.models;

import lombok.Data;

import org.springframework.stereotype.Component;

@Data
@Component
public class Response {

  /**
   * Response status.
   */
  private String status;

  /**
   * Response message.
   */
  private String message;

  /**
   * Response data.
   */
  private Object data;

  /**
   * Description : Default constructor.
   */
  public Response () {
    this.status = "ok";
    this.message = "";
    this.data = "";
  }

  /**
   * Description : Constructor.
   *
   */
  public Response(String status) {
    this.status = status;
    this.message = "";
    this.data = "";
  }

  /**
   * Description : Constructor.
   *
   */
  public Response(String status, String message) {
    this.status = status;
    this.message = message;
    this.data = "";
  }
}
