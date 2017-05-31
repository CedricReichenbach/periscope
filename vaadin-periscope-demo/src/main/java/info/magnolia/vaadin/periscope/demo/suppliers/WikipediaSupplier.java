package info.magnolia.vaadin.periscope.demo.suppliers;

import info.magnolia.vaadin.periscope.result.AsyncResultSupplier;
import info.magnolia.vaadin.periscope.result.Result;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import com.vaadin.ui.UI;

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.impl.JreJsonObject;

public class WikipediaSupplier implements AsyncResultSupplier {

    private static final String API_BASE_URL = "https://en.wikipedia.org/w/api.php";
    private static final String API_URL_TEMPLATE = API_BASE_URL + "?format=json&action=query&titles=%s";

    private static final String PAGE_URL_TEMPLATE = "https://en.wikipedia.org/?curid=%s";

    @Override
    public String getTitle() {
        return "Wikipedia";
    }

    @Override
    public CompletableFuture<List<Result>> search(final String query) {
        final String cleanedQuery = query.toLowerCase().replaceFirst("^wikipedia ", "");

        return CompletableFuture.supplyAsync(() -> findWikipediaPages(query));
    }

    private List<Result> findWikipediaPages(String query) {
        final Client client = ClientBuilder.newClient();
        try {
            final WebTarget target = client.target(String.format(API_URL_TEMPLATE, URLEncoder.encode(query, "UTF-8")));
            final JsonObject resultJson = Json.parse(target.request(MediaType.APPLICATION_JSON_TYPE).get(String.class));

            return compileResults(resultJson, query);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 is not supported");
        }
    }

    private List<Result> compileResults(final JsonObject resultJson, final String query) {
        final JsonObject pagesNode = resultJson.getObject("query").get("pages");

        final List<Result> results = new ArrayList<>();

        for (String id : pagesNode.keys()) {
            if (id.equals("-1")) {
                return Collections.emptyList();
            }

            final JsonObject pageNode = pagesNode.get(id);
            final String pageUrl = String.format(PAGE_URL_TEMPLATE, pageNode.getNumber("pageid"));
            results.add(new Result(pageNode.getString("title"), () -> openInNewTab(pageUrl)));
        }

        return results;
    }

    private void openInNewTab(final String url) {
        UI.getCurrent().getPage().open(url, "_blank");
    }
}
