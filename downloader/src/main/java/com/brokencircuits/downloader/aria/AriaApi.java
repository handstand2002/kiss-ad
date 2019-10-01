package com.brokencircuits.downloader.aria;

import com.brokencircuits.downloader.configprops.AriaProps;
import com.brokencircuits.downloader.domain.AriaResponseStatus;
import com.brokencircuits.downloader.domain.AriaResponseUriSubmit;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AriaApi {

  private final AriaProps ariaProps;

  public AriaResponseUriSubmit submitUri(String requestId, String uri)
      throws IOException {

    JsonArray parameters = new JsonArray();
    JsonArray uriList = new JsonArray();
    uriList.add(uri);
    parameters.add(uriList);

    JsonObject options = new JsonObject();
    options.add("dir", new JsonPrimitive(ariaProps.getAriaTempDownloadDir()));
    parameters.add(options);
    log.debug("Parameters: {}", new Gson().toJson(parameters));

    return submitRequest("aria2.addUri", requestId, parameters, AriaResponseUriSubmit.class);
  }

  public void removeDownload(String downloadId) throws IOException {
    JsonArray parameters = new JsonArray();
    parameters.add(downloadId);
    submitRequest("aria2.remove", "test", parameters, null);
  }

  public AriaResponseStatus queryStatus(String downloadId) throws IOException {
    JsonArray parameters = new JsonArray();
    parameters.add(downloadId);
    return submitRequest("aria2.tellStatus", "test", parameters, AriaResponseStatus.class);
  }

  public <T> T submitRequest(String method, String requestId, JsonArray params,
      Class<T> expectedObject)
      throws IOException {
    log.debug("Starting request");

    Map<String, String> parameters = new HashMap<>();
    parameters.put("method", method);
    parameters.put("id", requestId);
    parameters
        .put("params", Base64.getEncoder().encodeToString(new Gson().toJson(params).getBytes()));

    URL url = new URL("http://127.0.0.1:" + ariaProps.getAriaRpcPort() + "/jsonrpc?"
        + getParamsString(parameters));
    log.debug("Request: {}", url);

    HttpURLConnection con = (HttpURLConnection) url.openConnection();
    con.setRequestMethod("GET");
    con.setRequestProperty("Accept", "application/json");

    if (con.getResponseCode() != 200) {
      throw new RuntimeException("Failed : HTTP error code : "
          + con.getResponseCode());
    }

    BufferedReader br = new BufferedReader(new InputStreamReader(
        (con.getInputStream())));

    String outputLine;
    StringBuilder fullOutput = new StringBuilder();
    while ((outputLine = br.readLine()) != null) {
      fullOutput.append(outputLine);
    }

    if (expectedObject == null) {
      return null;
    }

    log.debug("Raw response: {}", fullOutput.toString());
    T response = new Gson().fromJson(fullOutput.toString(), expectedObject);

    con.disconnect();

    return response;
  }

  private static String getParamsString(Map<String, String> params)
      throws UnsupportedEncodingException {
    StringBuilder result = new StringBuilder();

    for (Map.Entry<String, String> entry : params.entrySet()) {
      result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
      result.append("=");
      result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
      result.append("&");
    }

    String resultString = result.toString();
    return resultString.length() > 0
        ? resultString.substring(0, resultString.length() - 1)
        : resultString;
  }
}
