package ru.viduk;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.lang3.concurrent.TimedSemaphore;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class CrptApi {

    @Data
    @AllArgsConstructor
    public static class Document {
        private Description description;
        private String docId;
        private String docStatus;
        private String docType;
        private boolean importRequest;
        private String ownerInn;
        private String participantInn;
        private String producerInn;
        private Date productionDate;
        private String productionType;
        private List<ProductDetail> products;
        private Date regDate;
        private String regNumber;

        @Data
        @AllArgsConstructor
        public static class Description {
            private String participantInn;

        }

        @Data
        @AllArgsConstructor
        public static class ProductDetail {
            private String certificateDocument;
            private Date certificateDocumentDate;
            private String certificateDocumentNumber;
            private String ownerInn;
            private String producerInn;
            private Date productionDate;
            private String tnvedCode;
            private String uitCode;
            private String uituCode;

        }

    }

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final TimedSemaphore timedSemaphore;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.timedSemaphore = new TimedSemaphore(requestLimit, timeUnit, 1);
    }

    public void createDocument(Document document, String signature) throws InterruptedException, IOException {
        timedSemaphore.acquire();

        String url = "https://ismp.crpt.ru/api/v3/lk/documents/create";
        String requestBody = objectMapper.writeValueAsString(document);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Signature", signature)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IOException("Failed to create document: " + response.body());
            }
        } catch (IOException | InterruptedException e) {
            throw e;
        }
    }


    public static void main(String[] args) { //Создание документа и вызов метода createDocument()

        Document.Description description = new Document.Description("1234567890");
        Document.ProductDetail productDetail = new Document.ProductDetail(
                "CertDoc",
                new Date(),
                "CertDocNumber",
                "1234567890",
                "0987654321",
                new Date(),
                "TNVED123",
                "UIT123",
                "UITU123"
        );
        Document document = new Document(
                description,
                "doc123",
                "status",
                "LP_INTRODUCE_GOODS",
                true,
                "1234567890",
                "0987654321",
                "1122334455",
                new Date(),
                "productionType",
                List.of(productDetail),
                new Date(),
                "regNumber"
        );

        CrptApi crptApi = new CrptApi(TimeUnit.SECONDS, 5);
        try {
            crptApi.createDocument(document, "fdfds");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
