package info.magnolia.vaadin.periscope.demo.suppliers;

import info.magnolia.vaadin.periscope.result.Result;
import info.magnolia.vaadin.periscope.result.ResultSupplier;
import info.magnolia.vaadin.periscope.result.SupplierUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.vaadin.ui.UI;

public class WebsiteSupplier implements ResultSupplier {

    private final Map<String, String> websites;

    public WebsiteSupplier() {
        websites = new HashMap<>();
        websites.put("Google", "http://www.google.com/");
        websites.put("Bing", "http://www.bing.com/");
        websites.put("Magnolia CMS", "http://www.magnolia-cms.com/");
        websites.put("Should I use tables for layout?", "http://shouldiusetablesforlayout.com/");
        websites.put("GitHub", "https://github.com/");
    }


    @Override
    public String getTitle() {
        return "Websites";
    }

    @Override
    public List<Result> search(final String query) {
        return websites.entrySet().stream()
                .filter(entry -> entry.getKey().toLowerCase().contains(query.toLowerCase()))
                .map(entry -> new Result(SupplierUtil.highlight(entry.getKey(), query), () -> openInNewTab(entry.getValue())))
                .collect(Collectors.toList());
    }

    private void openInNewTab(final String url) {
        UI.getCurrent().getPage().open(url, "_blank");
    }
}
