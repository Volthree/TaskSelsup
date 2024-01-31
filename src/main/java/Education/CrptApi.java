package Education;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

/*
Тестовое задание на вакансию Backend-разработчик в компанию «Selsup»

Комментарий:
1) Я не знаю деталей реализации подписи, переданной в метод. Добавил в хедер, что бы использовать его в качестве фильтра запроса.
2) Если лимит запросов превышен, можно реализовывать другой http запрос, зависящий от архитектуры, пока добавил просто логирование.
3) Класс можно сделать @Service, для этого повесить аннотацию и изменить конструктор.
4) Я вынес исключение в обработку в вызывающем методе, их можно обрабатывать внутри в зависимости от архитектуры, можно создать собственные исключения.
5) Синхронизацию можно сделать Lock-ами на отдельных участках кода, что бы обеспечить многопоточную работу не влияющих друг на друга процессов.
Author: Vladislav Maltsev
*/
@Log4j2
public class CrptApi {
    private TimeUnit timeUnit;
    private int requestLimit;
    private String url = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private HttpClient client;
    private ObjectMapper objectMapper = new ObjectMapper();
    private LocalTime lastUpdatedTime;
    private Queue<LocalTime> requestTimes = new ArrayDeque<>();

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
    }

    public synchronized HttpResponse<String> postCrptDocument(CrptRegistration crptRegistration, String crptSignature) throws IOException, InterruptedException {
        lastUpdatedTime = LocalTime.now();
        client = HttpClient.newHttpClient();
        while (requestTimes.peek() != null &&
                requestTimes.peek().isBefore(lastUpdatedTime.minus(1, timeUnit.toChronoUnit()))) {
            requestTimes.remove();
        }
        if (requestTimes.size() < requestLimit) {
            requestTimes.add(LocalTime.now());
        } else {
            log.warn("Request out of limit");
            return null;
        }
        HttpRequest.BodyPublisher body = HttpRequest.BodyPublishers.ofString(
                objectMapper.writeValueAsString(crptRegistration),
                StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("CrptSignature", crptSignature)
                .POST(body)
                .build();
        return client.send(
                request,
                HttpResponse.BodyHandlers.ofString());
    }

    @Data
    @NoArgsConstructor
    @Builder
    @AllArgsConstructor
    public final static class CrptRegistration {
        private Description description;
        private String doc_id;
        private String doc_status;
        private String doc_type;
        private boolean importRequest;
        private String owner_inn;
        private String participant_inn;
        private String producer_inn;
        private Date production_date;
        private String production_type;
        private List<Product> products;
        private String reg_date;
        private String reg_number;

        @Data
        @NoArgsConstructor
        @Builder
        @AllArgsConstructor
        public final static class Description {
            private String participantInn;
        }

        @Data
        @NoArgsConstructor
        @Builder
        @AllArgsConstructor
        static final class Product {
            private String certificate_document;
            private String certificate_document_date;
            private String certificate_document_number;
            private String owner_inn;
            private String producer_inn;
            private String production_date;
            private String tnved_code;
            private String uit_code;
            private String uitu_code;
        }
    }
}
