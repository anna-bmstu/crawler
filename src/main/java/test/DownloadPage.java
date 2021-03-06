package test;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import org.jsoup.nodes.Document;
import java.nio.charset.StandardCharsets;

public class DownloadPage extends Thread {
    private static String server = "http://inelstal.ru";
    public static void AppendPage(String page) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setUsername("rabbitmq");
        factory.setPassword("rabbitmq");
        factory.setPort(5672);
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            channel.queueDeclare("PAGES", false, false, true, null);
            channel.basicPublish("", "PAGES", null, page.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            System.out.println("error: "+ e.toString());
        }
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
            channel.queueDeclare("LINKS", false, false, true, null);
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String link = new String(delivery.getBody(), "UTF-8");  // get link from queue LINKS
                try {
                    TaskController downloader = new TaskController();
                    Document doc = downloader.getUrl(server + link);             // download page
                    AppendPage(doc.toString());                                  // append page to queue PAGES
                } catch (Exception e) {
                    System.out.println("error while downloading page " + e.toString());
                } finally {
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                }
            };
            boolean autoAck = false;
            channel.basicConsume("LINKS", autoAck, deliverCallback, consumerTag -> { });
        } catch (Exception e) {
            System.out.println("error: "+ e.toString());
        }
    }
}
