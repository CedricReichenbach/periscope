package info.magnolia.vaadin.periscope.demo.suppliers;

import info.magnolia.vaadin.periscope.result.Result;
import info.magnolia.vaadin.periscope.result.ResultSupplier;
import info.magnolia.vaadin.periscope.result.SearchFailedException;
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
        websites.put("Hacker News", "https://news.ycombinator.com/");
        websites.put("Slashdot", "https://slashdot.org/");
        websites.put("Good Morning Kitten", "http://goodmorningkitten.com/");
        websites.put("So Pets", "http://www.sopets.com/");
        websites.put("ipetitions", "https://www.ipetitions.com/");
        websites.put("Petit Bateau", "https://www.petit-bateau.de/");
        websites.put("PenInsulin: Insulin pens", "http://peninsulin.com");
        websites.put("Peninsulas of europe", "https://www.quora.com/What-are-all-of-the-peninsulas-in-Europe");
        websites.put("Cargold collection", "http://www.cargold-collection.com/");
        websites.put("SBB Cargo", "http://www.sbbcargo.com/");
        websites.put("Cargo Domizil", "http://www.cargodomizil.ch/");
    }


    @Override
    public String getTitle() {
        return "Websites";
    }

    @Override
    public List<Result> search(final String query) throws SearchFailedException {
        return websites.entrySet().stream()
                .filter(entry -> entry.getKey().toLowerCase().contains(query.toLowerCase()))
                .map(entry -> new Result(SupplierUtil.highlight(entry.getKey(), query), () -> openInNewTab(entry.getValue())))
                .collect(Collectors.toList());
    }

    private void openInNewTab(final String url) {
        UI.getCurrent().getPage().open(url, "_blank");
    }
}
