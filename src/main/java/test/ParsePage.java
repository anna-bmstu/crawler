package test;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import java.nio.charset.StandardCharsets;
import com.google.gson.Gson;

public class ParsePage extends Thread {
    public static void AppendContent(String page) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setUsername("rabbitmq");
        factory.setPassword("rabbitmq");
        factory.setPort(5672);
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            channel.queueDeclare("CONTENTS", false, false, true, null);
            channel.basicPublish("", "CONTENTS", null, page.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            System.out.println("error: " + e.toString());
        }
    }
    class Content {
        String Title = null;
        String Datetime = null;
        String Text = null;
    }
    public String GetTitle(String page) {
        Document doc = Jsoup.parse(page);
        Elements title = doc.getElementsByClass("b-page__title");
        return title.text();
    }
    public String GetDatetime(String page) {
        Document doc = Jsoup.parse(page);
        Elements datetime = doc.getElementsByClass("b-page__single-date");
        return datetime.first().text();
    }
    public String GetText(String page) {
        Document doc = Jsoup.parse(page);
        Elements text = doc.getElementsByClass("b-page__content");
        return text.text();
    }
    public void run() {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");
            factory.setUsername("rabbitmq");
            factory.setPassword("rabbitmq");
            factory.setPort(5672);
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();
            channel.queueDeclare("PAGES", false, false, true, null);
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String page = new String(delivery.getBody(), "UTF-8");  // get page from queue PAGES
                try {
                    Gson gson = new Gson();

                    Content cont = new Content();
                    cont.Title = GetTitle(page);
                    cont.Datetime = GetDatetime(page);
                    cont.Text = GetText(page);
                    AppendContent(gson.toJson(cont));   // convert java object to json and append it to queue CONTENTS
                } catch (Exception e) {
                    System.out.println("error while parsing page " + e.toString());
                } finally {
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                }
            };
            boolean autoAck = false;
            channel.basicConsume("PAGES", autoAck, deliverCallback, consumerTag -> { });
        } catch (Exception e) {
            System.out.println("error: " + e.toString());
        }
    }
}
