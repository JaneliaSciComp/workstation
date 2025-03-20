package org.janelia.workstation.core.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.*;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Scanner;

public class GitHubRestClient {
    private static final Logger logger = LoggerFactory.getLogger(GitHubRestClient.class);

    private final String projectURL;
    private final String accessToken;

    private Client client;

    public GitHubRestClient() {
        this.projectURL = ConsoleProperties.getInstance().getProperty("console.GitHubErrorProjectURL");
        this.accessToken = ConsoleProperties.getInstance().getProperty("console.GitHubErrorProjectAccessToken");
        logger.debug("Using project URL: {}", this.projectURL);

        client = ClientBuilder.newClient();
    }

    private Invocation.Builder getBuilder(String path) {
        WebTarget baseTarget = client.target(projectURL);
        WebTarget pathTarget = baseTarget.path(path);
        return pathTarget.request(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
    }

    // this method is for testing and debugging
    public List<String> getIssueList() {
        List<String> issues = new ArrayList<>();

        Response response = getBuilder("issues").get();
        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
            logger.error("Error getting issues list; status {}, body {}", response.getStatus(), response.readEntity(String.class));
            return issues;
        }

        String json = response.readEntity(String.class);
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode nodes = mapper.readValue(json, JsonNode.class);
            for (JsonNode node: nodes) {
                issues.add(node.at("/title").asText());
            }
        } catch (JsonProcessingException e) {
            logger.error("Error parsing json for issues list");
            return issues;
        }

        return issues;
    }

    public int createIssue(String title, String body) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode newIssue = mapper.createObjectNode();
        newIssue.put("title", title);
        newIssue.put("body", body);

        Response response = getBuilder("issues").post(Entity.json(newIssue));
        if (response.getStatus() != Response.Status.CREATED.getStatusCode()) {
            logger.error("Error creating new issue; status {}, body {}", response.getStatus(), response.readEntity(String.class));
            return 0;
        }

        String json = response.readEntity(String.class);
        try {
            ObjectMapper mapper2 = new ObjectMapper();
            JsonNode node = mapper2.readTree(json);
            if (!node.has("number")) {
                logger.error("Returned json doesn't contain issue number");
                return 0;
            }
            return Integer.parseInt(node.get("number").asText());
        } catch (JsonProcessingException e) {
            logger.error("Error parsing json for created issue");
            return 0;
        }
    }

    public String uploadLogFile(String branch, File logfile, String path) {
        String unencodedLog = "";
        try (Scanner scanner = new Scanner(logfile)) {
            unencodedLog = scanner.useDelimiter("\\A").next();
        } catch (IOException e) {
            logger.error("Error reading log file");
            return "";
        }
        String encodedLog = Base64.getEncoder().encodeToString(unencodedLog.getBytes());

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode upload = mapper.createObjectNode();
        upload.put("message", "uploaded attachment");
        upload.put("branch", branch);
        upload.put("content", encodedLog);

        Response response = getBuilder("contents/" + path).put(Entity.json(upload));
        if (response.getStatus() != Response.Status.CREATED.getStatusCode()) {
            logger.error("Error uploading logfile; status {}, body {}", response.getStatus(), response.readEntity(String.class));
            return "";
        }

        // generate permalink to the uploaded file
        String json = response.readEntity(String.class);
        String permalink = "";
        try {
            ObjectMapper mapper2 = new ObjectMapper();
            JsonNode tree = mapper2.readTree(json);
            String sha = tree.at("/commit/sha").asText();
            permalink = projectURL.replace("api.", "").replace("/repos", "") +
                    "/blob/" + sha + "/" + path;
        } catch (JsonProcessingException e) {
            logger.error("Error parsing json from logfile upload");
            return "";
        }
        return permalink;
    }

    public boolean addComment(int issueId, String comment) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode commentNode = mapper.createObjectNode();
        commentNode.put("body", comment);

        Response response = getBuilder("issues/" + issueId + "/comments").post(Entity.json(commentNode));
        if (response.getStatus() != Response.Status.CREATED.getStatusCode()) {
            logger.error("Error adding comment; status {}, body {}", response.getStatus(), response.readEntity(String.class));
            return false;
        } else {
            return true;
        }
    }

}
