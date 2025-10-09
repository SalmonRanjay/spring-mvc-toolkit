package com.ranjay.bootstrap.service;

public class Misc {
    
}
// ============================================
// DYNAMODB ATTRIBUTE MAPPER ENUM
// ============================================
package com.example.csvreport.model;

import lombok.Getter;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;
import java.util.function.Function;

@Getter
public enum DynamoDbAttribute {
    MANIFEST_ID("ManifestID", attr -> attr.s()),
    SUBMISSION_ID("SubmissionId", attr -> attr.s()),
    TIMESTAMP("Timestamp", attr -> attr.s()),
    STATUS("Status", attr -> attr.s()),
    GATEWAY_STATUS("GatewayStatus", attr -> attr.s()),
    FEE_TOTAL("FeeTotal", attr -> attr.n() != null ? Double.parseDouble(attr.n()) : null),
    MID("MiD", attr -> attr.s());
    
    private final String attributeName;
    private final Function<AttributeValue, Object> extractor;
    
    DynamoDbAttribute(String attributeName, Function<AttributeValue, Object> extractor) {
        this.attributeName = attributeName;
        this.extractor = extractor;
    }
    
    public Object extractValue(Map<String, AttributeValue> item) {
        return item.containsKey(attributeName) 
            ? extractor.apply(item.get(attributeName)) 
            : null;
    }
}

// ============================================
// DYNAMODB MAPPER SERVICE
// ============================================
package com.example.csvreport.service;

import com.example.csvreport.model.DynamoDbAttribute;
import com.example.csvreport.model.DynamoSubmissionRecord;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;
import java.util.Optional;

@Service
public class DynamoDbMapperService {
    
    public DynamoSubmissionRecord mapToRecord(Map<String, AttributeValue> item) {
        return DynamoSubmissionRecord.builder()
            .manifestId(extractString(item, DynamoDbAttribute.MANIFEST_ID))
            .submissionId(extractString(item, DynamoDbAttribute.SUBMISSION_ID))
            .timestamp(extractString(item, DynamoDbAttribute.TIMESTAMP))
            .status(extractString(item, DynamoDbAttribute.STATUS))
            .gatewayStatus(extractString(item, DynamoDbAttribute.GATEWAY_STATUS))
            .feeTotal(extractDouble(item, DynamoDbAttribute.FEE_TOTAL))
            .miD(extractString(item, DynamoDbAttribute.MID))
            .build();
    }
    
    private String extractString(Map<String, AttributeValue> item, DynamoDbAttribute attribute) {
        return Optional.ofNullable(attribute.extractValue(item))
            .map(Object::toString)
            .orElse(null);
    }
    
    private Double extractDouble(Map<String, AttributeValue> item, DynamoDbAttribute attribute) {
        return Optional.ofNullable(attribute.extractValue(item))
            .map(obj -> (Double) obj)
            .orElse(null);
    }
}

// ============================================
// UPDATED AWS CONFIG
// ============================================
package com.example.csvreport.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;

@Configuration
public class AwsConfig {
    
    @Value("${aws.dynamodb.region}")
    private String region;
    
    @Value("${aws.dynamodb.endpoint:}")
    private String endpoint;
    
    @Bean
    public DynamoDbClient dynamoDbClient() {
        var builder = DynamoDbClient.builder()
            .region(Region.of(region))
            .credentialsProvider(DefaultCredentialsProvider.create());
        
        if (endpoint != null && !endpoint.isEmpty()) {
            builder.endpointOverride(URI.create(endpoint));
        }
        
        return builder.build();
    }
}

// ============================================
// FUNCTIONAL DYNAMODB SERVICE
// ============================================
package com.example.csvreport.service;

import com.example.csvreport.model.DynamoSubmissionRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class DynamoDbService {
    
    private final DynamoDbClient dynamoDbClient;
    private final DynamoDbMapperService mapperService;
    
    @Value("${aws.dynamodb.table-name}")
    private String tableName;
    
    public List<DynamoSubmissionRecord> queryByMiD(String miD) {
        return scanAllPages(buildScanRequest(miD))
            .flatMap(response -> response.items().stream())
            .map(mapperService::mapToRecord)
            .peek(record -> log.debug("Mapped record for MiD {}: {}", miD, record.getSubmissionId()))
            .collect(Collectors.toList());
    }
    
    public Map<String, List<DynamoSubmissionRecord>> queryByMiDs(List<String> miDs) {
        return miDs.stream()
            .distinct()
            .collect(Collectors.toMap(
                miD -> miD,
                this::queryByMiD
            ));
    }
    
    private ScanRequest buildScanRequest(String miD) {
        return ScanRequest.builder()
            .tableName(tableName)
            .filterExpression("MiD = :mid")
            .expressionAttributeValues(Map.of(
                ":mid", AttributeValue.builder().s(miD).build()
            ))
            .build();
    }
    
    private Stream<ScanResponse> scanAllPages(ScanRequest initialRequest) {
        return Stream.iterate(
            dynamoDbClient.scan(initialRequest),
            response -> response.lastEvaluatedKey() != null && !response.lastEvaluatedKey().isEmpty(),
            response -> dynamoDbClient.scan(
                initialRequest.toBuilder()
                    .exclusiveStartKey(response.lastEvaluatedKey())
                    .build()
            )
        );
    }
}

// ============================================
// FUNCTIONAL CSV READER SERVICE
// ============================================
package com.example.csvreport.service;

import com.example.csvreport.model.CsvHeader;
import com.example.csvreport.model.CsvInputRecord;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@Slf4j
public class CsvReaderService {
    
    public List<CsvInputRecord> readCsv(String filePath) throws IOException {
        try (Reader reader = new FileReader(filePath);
             CSVParser csvParser = new CSVParser(reader, 
                 CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .build())) {
            
            Map<CsvHeader, Integer> headerMap = buildHeaderMap(csvParser.getHeaderMap());
            
            List<CsvInputRecord> records = StreamSupport.stream(csvParser.spliterator(), false)
                .map(csvRecord -> mapToInputRecord(csvRecord, headerMap))
                .collect(Collectors.toList());
            
            log.info("Read {} records from CSV file: {}", records.size(), filePath);
            return records;
        }
    }
    
    private Map<CsvHeader, Integer> buildHeaderMap(Map<String, Integer> csvHeaders) {
        return csvHeaders.entrySet().stream()
            .flatMap(entry -> findHeaderByColumnName(entry.getKey())
                .map(header -> Map.entry(header, entry.getValue()))
                .stream())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
    
    private Optional<CsvHeader> findHeaderByColumnName(String columnName) {
        return StreamSupport.stream(
            ((Iterable<CsvHeader>) () -> List.of(CsvHeader.values()).iterator()).spliterator(), 
            false)
            .filter(header -> header.getColumnName().equals(columnName))
            .findFirst();
    }
    
    private CsvInputRecord mapToInputRecord(CSVRecord csvRecord, Map<CsvHeader, Integer> headerMap) {
        return CsvInputRecord.builder()
            .miD(getValueByHeader(csvRecord, headerMap, CsvHeader.MID))
            .formType(getValueByHeader(csvRecord, headerMap, CsvHeader.FORM_TYPE))
            .manifestId(getValueByHeader(csvRecord, headerMap, CsvHeader.MANIFEST_ID))
            .build();
    }
    
    private String getValueByHeader(CSVRecord csvRecord, 
                                    Map<CsvHeader, Integer> headerMap, 
                                    CsvHeader header) {
        return Optional.ofNullable(headerMap.get(header))
            .map(csvRecord::get)
            .orElse("");
    }
}

// ============================================
// FUNCTIONAL POSTGRES SERVICE
// ============================================
package com.example.csvreport.service;

import com.example.csvreport.model.SubmissionRecord;
import com.example.csvreport.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostgresService {
    
    private final SubmissionRepository submissionRepository;
    
    public Optional<SubmissionRecord> getSubmissionById(String submissionId) {
        log.debug("Querying PostgreSQL for submissionId: {}", submissionId);
        return submissionRepository.findBySubmissionId(submissionId);
    }
    
    public Map<String, SubmissionRecord> getSubmissionsByIds(List<String> submissionIds) {
        return submissionIds.stream()
            .distinct()
            .map(this::getSubmissionById)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toMap(
                SubmissionRecord::getSubmissionId,
                record -> record
            ));
    }
}

// ============================================
// FUNCTIONAL REPORT GENERATION SERVICE
// ============================================
package com.example.csvreport.service;

import com.example.csvreport.model.CsvInputRecord;
import com.example.csvreport.model.DynamoSubmissionRecord;
import com.example.csvreport.model.FinalReportRecord;
import com.example.csvreport.model.SubmissionRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportGenerationService {
    
    private final DynamoDbService dynamoDbService;
    private final PostgresService postgresService;
    
    public List<FinalReportRecord> generateReport(List<CsvInputRecord> csvRecords) {
        List<String> uniqueMiDs = extractUniqueMiDs(csvRecords);
        Map<String, List<DynamoSubmissionRecord>> dynamoRecordsByMiD = 
            dynamoDbService.queryByMiDs(uniqueMiDs);
        
        List<String> allSubmissionIds = extractAllSubmissionIds(dynamoRecordsByMiD);
        Map<String, SubmissionRecord> pgRecordsBySubmissionId = 
            postgresService.getSubmissionsByIds(allSubmissionIds);
        
        List<FinalReportRecord> finalRecords = csvRecords.stream()
            .flatMap(csvRecord -> processCSVRecord(
                csvRecord, 
                dynamoRecordsByMiD, 
                pgRecordsBySubmissionId
            ))
            .collect(Collectors.toList());
        
        log.info("Generated {} final report records", finalRecords.size());
        return finalRecords;
    }
    
    private List<String> extractUniqueMiDs(List<CsvInputRecord> csvRecords) {
        return csvRecords.stream()
            .map(CsvInputRecord::getMiD)
            .distinct()
            .collect(Collectors.toList());
    }
    
    private List<String> extractAllSubmissionIds(Map<String, List<DynamoSubmissionRecord>> dynamoRecordsByMiD) {
        return dynamoRecordsByMiD.values().stream()
            .flatMap(List::stream)
            .map(DynamoSubmissionRecord::getSubmissionId)
            .distinct()
            .collect(Collectors.toList());
    }
    
    private java.util.stream.Stream<FinalReportRecord> processCSVRecord(
            CsvInputRecord csvRecord,
            Map<String, List<DynamoSubmissionRecord>> dynamoRecordsByMiD,
            Map<String, SubmissionRecord> pgRecordsBySubmissionId) {
        
        return Optional.ofNullable(dynamoRecordsByMiD.get(csvRecord.getMiD()))
            .orElse(List.of())
            .stream()
            .map(dynamoRecord -> buildFinalRecord(
                csvRecord, 
                dynamoRecord, 
                pgRecordsBySubmissionId.get(dynamoRecord.getSubmissionId())
            ));
    }
    
    private FinalReportRecord buildFinalRecord(
            CsvInputRecord csvRecord,
            DynamoSubmissionRecord dynamoRecord,
            SubmissionRecord pgRecord) {
        
        return FinalReportRecord.builder()
            .miD(csvRecord.getMiD())
            .formType(csvRecord.getFormType())
            .manifestId(csvRecord.getManifestId())
            .submissionId(dynamoRecord.getSubmissionId())
            .timestamp(dynamoRecord.getTimestamp())
            .status(dynamoRecord.getStatus())
            .gatewayStatus(dynamoRecord.getGatewayStatus())
            .feeTotal(dynamoRecord.getFeeTotal())
            .tappId(Optional.ofNullable(pgRecord).map(SubmissionRecord::getTappId).orElse(""))
            .authorizationStatus(Optional.ofNullable(pgRecord).map(SubmissionRecord::getAuthorizationStatus).orElse(""))
            .authorizationDate(Optional.ofNullable(pgRecord).map(SubmissionRecord::getAuthorizationDate).orElse(""))
            .build();
    }
}

// ============================================
// FUNCTIONAL CSV WRITER SERVICE
// ============================================
package com.example.csvreport.service;

import com.example.csvreport.model.FinalReportRecord;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CsvWriterService {
    
    private static final String[] HEADERS = {
        "MiD", "FormType", "ManifestId", "SubmissionId", "Timestamp",
        "Status", "GatewayStatus", "FeeTotal", "TappId", 
        "AuthorizationStatus", "AuthorizationDate"
    };
    
    public void writeReport(String filePath, List<FinalReportRecord> records) throws IOException {
        List<FinalReportRecord> sortedRecords = records.stream()
            .sorted(Comparator.comparing(FinalReportRecord::getMiD))
            .collect(Collectors.toList());
        
        try (FileWriter writer = new FileWriter(filePath);
             CSVPrinter csvPrinter = new CSVPrinter(writer, 
                 CSVFormat.DEFAULT.builder().setHeader(HEADERS).build())) {
            
            sortedRecords.stream()
                .map(this::recordToArray)
                .forEach(row -> printRow(csvPrinter, row));
        }
        
        log.info("Successfully wrote {} records to: {}", sortedRecords.size(), filePath);
    }
    
    private Object[] recordToArray(FinalReportRecord record) {
        return new Object[] {
            record.getMiD(),
            record.getFormType(),
            record.getManifestId(),
            record.getSubmissionId(),
            record.getTimestamp(),
            record.getStatus(),
            record.getGatewayStatus(),
            record.getFeeTotal(),
            record.getTappId(),
            record.getAuthorizationStatus(),
            record.getAuthorizationDate()
        };
    }
    
    private void printRow(CSVPrinter csvPrinter, Object[] row) {
        try {
            csvPrinter.printRecord(row);
        } catch (IOException e) {
            log.error("Error writing row to CSV", e);
            throw new RuntimeException("Failed to write CSV row", e);
        }
    }
}

// ============================================
// UPDATED UNIT TESTS
// ============================================
package com.example.csvreport.service;

import com.example.csvreport.model.DynamoSubmissionRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DynamoDbServiceTest {
    
    @Mock
    private DynamoDbClient dynamoDbClient;
    
    @Mock
    private DynamoDbMapperService mapperService;
    
    private DynamoDbService dynamoDbService;
    
    @BeforeEach
    void setUp() {
        dynamoDbService = new DynamoDbService(dynamoDbClient, mapperService);
    }
    
    @Test
    void testQueryByMiD() {
        Map<String, AttributeValue> item = Map.of(
            "MiD", AttributeValue.builder().s("MID123").build(),
            "SubmissionId", AttributeValue.builder().s("SUB001").build()
        );
        
        ScanResponse scanResponse = ScanResponse.builder()
            .items(item)
            .lastEvaluatedKey(Map.of())
            .build();
        
        DynamoSubmissionRecord expectedRecord = DynamoSubmissionRecord.builder()
            .miD("MID123")
            .submissionId("SUB001")
            .build();
        
        when(dynamoDbClient.scan(any(ScanRequest.class))).thenReturn(scanResponse);
        when(mapperService.mapToRecord(item)).thenReturn(expectedRecord);
        
        List<DynamoSubmissionRecord> results = dynamoDbService.queryByMiD("MID123");
        
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("MID123", results.get(0).getMiD());
        
        verify(dynamoDbClient).scan(any(ScanRequest.class));
        verify(mapperService).mapToRecord(item);
    }
    
    @Test
    void testQueryByMiD_WithPagination() {
        Map<String, AttributeValue> item1 = Map.of(
            "MiD", AttributeValue.builder().s("MID123").build(),
            "SubmissionId", AttributeValue.builder().s("SUB001").build()
        );
        
        Map<String, AttributeValue> item2 = Map.of(
            "MiD", AttributeValue.builder().s("MID123").build(),
            "SubmissionId", AttributeValue.builder().s("SUB002").build()
        );
        
        Map<String, AttributeValue> lastKey = Map.of(
            "ManifestID", AttributeValue.builder().s("key1").build()
        );
        
        ScanResponse firstResponse = ScanResponse.builder()
            .items(item1)
            .lastEvaluatedKey(lastKey)
            .build();
        
        ScanResponse secondResponse = ScanResponse.builder()
            .items(item2)
            .lastEvaluatedKey(Map.of())
            .build();
        
        DynamoSubmissionRecord record1 = DynamoSubmissionRecord.builder()
            .miD("MID123")
            .submissionId("SUB001")
            .build();
        
        DynamoSubmissionRecord record2 = DynamoSubmissionRecord.builder()
            .miD("MID123")
            .submissionId("SUB002")
            .build();
        
        when(dynamoDbClient.scan(any(ScanRequest.class)))
            .thenReturn(firstResponse, secondResponse);
        when(mapperService.mapToRecord(item1)).thenReturn(record1);
        when(mapperService.mapToRecord(item2)).thenReturn(record2);
        
        List<DynamoSubmissionRecord> results = dynamoDbService.queryByMiD("MID123");
        
        assertEquals(2, results.size());
        verify(dynamoDbClient, times(2)).scan(any(ScanRequest.class));
    }
}

package com.example.csvreport.service;

import com.example.csvreport.model.DynamoDbAttribute;
import com.example.csvreport.model.DynamoSubmissionRecord;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DynamoDbMapperServiceTest {
    
    private final DynamoDbMapperService mapperService = new DynamoDbMapperService();
    
    @Test
    void testMapToRecord() {
        Map<String, AttributeValue> item = Map.of(
            "ManifestID", AttributeValue.builder().s("MANIFEST001").build(),
            "SubmissionId", AttributeValue.builder().s("SUB001").build(),
            "Timestamp", AttributeValue.builder().s("2025-01-01T10:00:00Z").build(),
            "Status", AttributeValue.builder().s("APPROVED").build(),
            "GatewayStatus", AttributeValue.builder().s("SUCCESS").build(),
            "FeeTotal", AttributeValue.builder().n("100.50").build(),
            "MiD", AttributeValue.builder().s("MID123").build()
        );
        
        DynamoSubmissionRecord record = mapperService.mapToRecord(item);
        
        assertNotNull(record);
        assertEquals("MANIFEST001", record.getManifestId());
        assertEquals("SUB001", record.getSubmissionId());
        assertEquals("2025-01-01T10:00:00Z", record.getTimestamp());
        assertEquals("APPROVED", record.getStatus());
        assertEquals("SUCCESS", record.getGatewayStatus());
        assertEquals(100.50, record.getFeeTotal());
        assertEquals("MID123", record.getMiD());
    }
    
    @Test
    void testMapToRecord_WithMissingFields() {
        Map<String, AttributeValue> item = Map.of(
            "ManifestID", AttributeValue.builder().s("MANIFEST001").build(),
            "SubmissionId", AttributeValue.builder().s("SUB001").build()
        );
        
        DynamoSubmissionRecord record = mapperService.mapToRecord(item);
        
        assertNotNull(record);
        assertEquals("MANIFEST001", record.getManifestId());
        assertEquals("SUB001", record.getSubmissionId());
        assertNull(record.getTimestamp());
        assertNull(record.getFeeTotal());
    }
}

package com.example.csvreport.service;

import com.example.csvreport.model.SubmissionRecord;
import com.example.csvreport.repository.SubmissionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostgresServiceTest {
    
    @Mock
    private SubmissionRepository submissionRepository;
    
    @InjectMocks
    private PostgresService postgresService;
    
    @Test
    void testGetSubmissionsByIds() {
        SubmissionRecord record1 = SubmissionRecord.builder()
            .submissionId("SUB001")
            .tappId("TAPP123")
            .build();
        
        SubmissionRecord record2 = SubmissionRecord.builder()
            .submissionId("SUB002")
            .tappId("TAPP456")
            .build();
        
        when(submissionRepository.findBySubmissionId("SUB001"))
            .thenReturn(Optional.of(record1));
        when(submissionRepository.findBySubmissionId("SUB002"))
            .thenReturn(Optional.of(record2));
        when(submissionRepository.findBySubmissionId("SUB003"))
            .thenReturn(Optional.empty());
        
        Map<String, SubmissionRecord> results = postgresService
            .getSubmissionsByIds(List.of("SUB001", "SUB002", "SUB003"));
        
        assertEquals(2, results.size());
        assertTrue(results.containsKey("SUB001"));
        assertTrue(results.containsKey("SUB002"));
        assertFalse(results.containsKey("SUB003"));
    }
}