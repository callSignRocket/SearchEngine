package searchengine.parsers;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.dto.statistics.StatisticsPage;
import searchengine.utils.RandomUserAgent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.RecursiveTask;

public class UrlParser extends RecursiveTask<List<StatisticsPage>> {
    private final String url;
    public static final CopyOnWriteArrayList<String> WRITE_ARRAY_LIST = new CopyOnWriteArrayList<>();
    private final List<StatisticsPage> statisticsPageList;
    private final List<String> urlList;
    private static final String CSS_QUERY = "a[href]";
    private static final String ATTRIBUTE_KEY = "href";

    public UrlParser(String url, List<StatisticsPage> statisticsPageList, List<String> urlList) {
        this.url = url;
        this.statisticsPageList = statisticsPageList;
        this.urlList = urlList;
    }

    @Override
    protected List<StatisticsPage> compute() {
        try {
            Connection.Response response = Jsoup.connect(url)
                    .ignoreContentType(true)
                    .userAgent(RandomUserAgent.getRandomUserAgent())
                    .execute();
            Document document = response.parse();
            String html = document.outerHtml();
            int status = response.statusCode();
            StatisticsPage statisticsPage = new StatisticsPage(url, html, status);
            statisticsPageList.add(statisticsPage);
            Elements elements = document.select(CSS_QUERY);
            List<UrlParser> taskList = new ArrayList<>();
            for (Element element : elements) {
                String attributeUrl = element.absUrl(ATTRIBUTE_KEY);
                if (!attributeUrl.isEmpty() && attributeUrl.startsWith(url) && !attributeUrl.contains("#") &&
                        !attributeUrl.contains(".pdf") && !attributeUrl.contains(".jpg") && !attributeUrl.contains(".JPG") &&
                        !attributeUrl.contains(".png") && !WRITE_ARRAY_LIST.contains(attributeUrl) && !urlList.contains(attributeUrl)) {
                    WRITE_ARRAY_LIST.add(attributeUrl);
                    urlList.add(attributeUrl);
                    UrlParser task = new UrlParser(attributeUrl, statisticsPageList, urlList);
                    task.fork();
                    taskList.add(task);
                }
            }
            taskList.sort(Comparator.comparing(UrlParser::getUrl));
            int i = 0;
            int allTasksSize = taskList.size();
            while (i < allTasksSize) {
                UrlParser task = taskList.get(i);
                task.join();
                i++;
            }
        } catch (Exception e) {
            StatisticsPage statisticsPage = new StatisticsPage(url, "", 500);
            statisticsPageList.add(statisticsPage);
        }

        try {
            Thread.sleep(150);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return statisticsPageList;
    }

    public String getUrl() {
        return url;
    }
}
