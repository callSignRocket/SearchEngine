package searchengine.utils.parsers;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.StatisticsPage;
import searchengine.utils.RandomUserAgent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RecursiveAction;

@Slf4j
public class UrlParser extends RecursiveAction {

    private final String url;
    public final List<StatisticsPage> statisticsPageList;
    private final List<String> urlList;
    private static final String CSS_QUERY = "a";
    private static final String ATTRIBUTE_KEY = "href";
    private final SitesList sitesList;
    private final String https = "https://";
    private final String http ="http://";

    public UrlParser(String url, List<StatisticsPage> statisticsPageList, List<String> urlList, SitesList sitesList) {
        this.url = url;
        this.statisticsPageList = statisticsPageList;
        this.urlList = urlList;
        this.sitesList = sitesList;
    }

    @Override
    protected void compute() {
        try {
            Thread.sleep(150);
            Document document = getConnect(url);
            String html = document.outerHtml();
            Connection.Response response = document.connection().response();
            int statusCode = response.statusCode();
            StatisticsPage statisticsPage = new StatisticsPage(url, html, statusCode);
            statisticsPageList.add(statisticsPage);
            Elements elements = document.select(CSS_QUERY);
            List<UrlParser> taskList = new ArrayList<>();

            for (Element element : elements) {
                String link = element.absUrl(ATTRIBUTE_KEY);
                if (!link.isEmpty()
                        && containsSite(link)
                        && !link.equals(element.baseUri())
                        && !link.contains("#")
                        && !isFile(link)
                        && !urlList.contains(link))
                {
                    urlList.add(link);
                    UrlParser task = new UrlParser(link, statisticsPageList, urlList, sitesList);
                    task.fork();
                    taskList.add(task);
                }
            }
            invokeAll(taskList);
        } catch (Exception e) {
            log.debug("Parsing error - " + url);
            StatisticsPage statisticsPage = new StatisticsPage(url, "", 500);
            statisticsPageList.add(statisticsPage);
        }
    }

    public Document getConnect(String url) {
        Document document = null;
        try {
            Thread.sleep(150);
            document =Jsoup.connect(url).userAgent(RandomUserAgent.getRandomUserAgent()).get();
        } catch (Exception e) {
            log.debug("Не удается подключиться к сайту " + url);
        }
        return document;
    }

    private boolean containsSite(String link) {
        String host;
        boolean containsSite = false;

        for (Site site : sitesList.getSites()) {
            host = site.getUrl();
            if (host.contains(https)){
                host = host.replace(https, "");
            } else if (host.contains(http)) {
                host = host.replace(http, "");
            }

            if (link.contains(host)) {
                containsSite = true;
            }
        }
        return containsSite;
    }

    private static boolean isFile(String link) {
        link.toLowerCase();
        return link.contains(".jpg")
                || link.contains(".JPG")
                || link.contains(".jpeg")
                || link.contains(".png")
                || link.contains(".gif")
                || link.contains(".webp")
                || link.contains(".pdf")
                || link.contains(".eps")
                || link.contains(".xlsx")
                || link.contains(".doc")
                || link.contains(".pptx")
                || link.contains(".docx")
                || link.contains("?_ga")
                || link.contains(".svg");
    }

    public  String getUrl() {
        return url;
    }
}
