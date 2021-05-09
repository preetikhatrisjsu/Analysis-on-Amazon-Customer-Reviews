package org.rm.study.mongodb;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.QuoteMode;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;

@SpringBootApplication(exclude = {MongoAutoConfiguration.class})
@Slf4j
public class MongodbClusterConnectionDemoApplication implements CommandLineRunner {

    private static final int BATCH_SIZE = 1000;

    public static void main(String[] args) {
        SpringApplication.run(MongodbClusterConnectionDemoApplication.class, args);
    }

    @Override
    public void run(final String... args) {
        this.uploadCsvDataToMongoDbCluster();
    }

    private void uploadCsvDataToMongoDbCluster() {

        final String filePath = "H:\\Projects\\study\\db\\SJSUAssignments\\amazon";
        final MongoClientURI mongoClientURI = this.getMongoClientURI();

        try (
            final MongoClient mongoClient = new MongoClient(mongoClientURI)
        ) {

            final MongoDatabase mongoDatabase = this.createDB(mongoClient, "amazon");

            Files.walk(Paths.get(filePath)).
                filter(Files::isRegularFile).
                filter(path -> path.getFileName().toString().endsWith(".csv")).
//                filter(path -> "associate.csv".equals(path.getFileName().toString())).
                forEach(path -> this.createCollectionAndInsertDocuments(mongoDatabase, path));
        } catch (IOException e) {

            log.error("Couldn't access the path: {}", filePath, e);
        }
    }

    private void createCollectionAndInsertDocuments(final MongoDatabase mongoDatabase, final Path path) {

        log.info("START: MongodbClusterConnectionDemoApplication.createCollectionAndInsertDocuments processing csv {}...", path.getFileName());

        MongoCollection<Document> mongoCollection = this.createCollection(
            mongoDatabase, this.getCollectionName(path.getFileName().toString())
        );

        try (
            CSVParser csvParser = new CSVParser(new FileReader(path.toFile()), this.getCsvFormat())
        ) {

            List<String> headerNames = csvParser.getHeaderNames();
            List<CSVRecord> records = csvParser.getRecords();
            List<Document> documents = this.getDocuments(headerNames, records);

            this.saveDocuments(mongoCollection, documents);

            log.info("END: MongodbClusterConnectionDemoApplication.createCollectionAndInsertDocuments processed csv {} with records {}.",
                path.getFileName(), CollectionUtils.size(records));
        } catch (IOException e) {

            log.error("Couldn't read the file: {}", path.getFileName(), e);
        }
    }

    private String getCollectionName(final String fileName) {
        return fileName.replaceAll("\\.csv", "");
    }

    private void saveDocuments(final MongoCollection<Document> mongoCollection, final List<Document> documents) {
        ListUtils.partition(documents, BATCH_SIZE).forEach(mongoCollection::insertMany);
    }

    private List<Document> getDocuments(final List<String> headerNames, final List<CSVRecord> records) {

        return records.stream().map(csvRecord -> {

            AtomicInteger columnIndex = new AtomicInteger(-1);
            Document document = new Document();

            csvRecord.forEach(columnValue ->
                document.append(headerNames.get(columnIndex.incrementAndGet()), this.getDataWithDataType(columnValue))
            );

            return document;
        }).collect(toList());
    }

    private Object getDataWithDataType(final Object columnValue) {

        if (!(columnValue instanceof String)) return columnValue;

        if ((StringUtils.trim((String) columnValue)).matches("[0-9]+")) {
            return Long.parseLong(StringUtils.trim((String) columnValue));
        }

        if ((StringUtils.trim((String) columnValue)).matches("[0-9]+\\.[0-9]+")) {
            return Double.parseDouble(StringUtils.trim((String) columnValue));
        }

        if ((StringUtils.trim((String) columnValue)).matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
            return LocalDate.parse(StringUtils.trim((String) columnValue), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }

        return StringUtils.trim((String) columnValue);
    }

    private MongoCollection<Document> createCollection(final MongoDatabase mongoDatabase, final String collectionName) {

        if (
            StreamSupport.stream(mongoDatabase.listCollectionNames().spliterator(), Boolean.FALSE).
                anyMatch(collectionName::equals)
        ) {
            return mongoDatabase.getCollection(collectionName);
        }

        mongoDatabase.createCollection(collectionName);

        return mongoDatabase.getCollection(collectionName);
    }

    private CSVFormat getCsvFormat() {
        return CSVFormat.DEFAULT.
            withIgnoreEmptyLines().
            withQuote('"').
            withDelimiter(',').
            withQuoteMode(QuoteMode.ALL).
            withFirstRecordAsHeader().
            withTrim();
    }

    private MongoClientURI getMongoClientURI() {

        final String user = "root";
        final String password = "root%40123";
        final String db = "sjsu-etl";
        final Integer connectionTimeoutMS = 300000;
        final Integer socketTimeoutMS = 300000;
        final String uri = String.format(
            "mongodb+srv://%s:%s@%s.dnf4k.mongodb.net/%s?retryWrites=true&w=majority&connectionTimeoutMS=%s&socketTimeoutMS=%s",
            user, password, db, db, connectionTimeoutMS, socketTimeoutMS
        );

        // This is yours
        /*
        final String user = "mongouser";
        final String password = "mongouser";
        final String db = "sjsu-etl";
        final Integer connectionTimeoutMS = 300000;
        final Integer socketTimeoutMS = 300000;
        final String uri = String.format(
            "mongodb+srv://%s:%s@%s.dnf4k.mongodb.net/myFirstDatabase?retryWrites=true&w=majority&connectionTimeoutMS=%s&socketTimeoutMS=%s",
            user, password, db, connectionTimeoutMS, socketTimeoutMS
        );*/

        return new MongoClientURI(uri);
    }

    private MongoDatabase createDB(final MongoClient mongoClient, final String dbName) {
        return mongoClient.getDatabase(dbName);
    }
}
