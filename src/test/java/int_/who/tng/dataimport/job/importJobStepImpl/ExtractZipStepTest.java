package int_.who.tng.dataimport.job.importJobStepImpl;

import eu.europa.ec.dgc.utils.CertificateUtils;
import int_.who.tng.dataimport.job.ImportJobContext;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = {ExtractZipStep.class})
public class ExtractZipStepTest {

    @Autowired
    private ExtractZipStep extractZipStep;

    private final static String TEST_ARCHIVE_NAME = "archive.zip";

    private final static String TEST_ARCHIVE_ENTRY_1_NAME = "test1.txt";
    private final static byte[] TEST_ARCHIVE_ENTRY_1_CONTENT = new byte[1000];

    private final static String TEST_ARCHIVE_ENTRY_2_NAME = "test2.txt";
    private final static byte[] TEST_ARCHIVE_ENTRY_2_CONTENT = new byte[2000];

    private final static String TEST_ARCHIVE_ENTRY_3_NAME = "test3.txt";
    private final static byte[] TEST_ARCHIVE_ENTRY_3_CONTENT = new byte[3000];

    private final static String TEST_EXTRACT_PREFIX = "test-extract";

    private static byte[] TEST_ARCHIVE;

    @BeforeAll
    static void setup() throws IOException {
        Random random = new Random();
        random.nextBytes(TEST_ARCHIVE_ENTRY_1_CONTENT);
        random.nextBytes(TEST_ARCHIVE_ENTRY_2_CONTENT);
        random.nextBytes(TEST_ARCHIVE_ENTRY_3_CONTENT);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream);

        zipOutputStream.putNextEntry(new ZipEntry("directory/"));
        zipOutputStream.closeEntry();

        zipOutputStream.putNextEntry(new ZipEntry(TEST_ARCHIVE_ENTRY_1_NAME));
        zipOutputStream.write(TEST_ARCHIVE_ENTRY_1_CONTENT);
        zipOutputStream.closeEntry();

        zipOutputStream.putNextEntry(new ZipEntry(TEST_ARCHIVE_ENTRY_2_NAME));
        zipOutputStream.write(TEST_ARCHIVE_ENTRY_2_CONTENT);
        zipOutputStream.closeEntry();

        zipOutputStream.putNextEntry(new ZipEntry(TEST_ARCHIVE_ENTRY_3_NAME));
        zipOutputStream.write(TEST_ARCHIVE_ENTRY_3_CONTENT);
        zipOutputStream.closeEntry();

        TEST_ARCHIVE = byteArrayOutputStream.toByteArray();
        zipOutputStream.close();
        byteArrayOutputStream.close();
    }

    @Test
    void testExtractFiles() {
        ImportJobContext context = new ImportJobContext();
        context.getFiles().put(TEST_ARCHIVE_NAME, TEST_ARCHIVE);

        extractZipStep.exec(context, TEST_ARCHIVE_NAME, TEST_EXTRACT_PREFIX);

        // ZIP File + 3 extracted files
        Assertions.assertEquals(4, context.getFiles().size());

        Assertions.assertTrue(context.getFiles().containsKey(TEST_EXTRACT_PREFIX + "/" + TEST_ARCHIVE_ENTRY_1_NAME));
        Assertions.assertTrue(context.getFiles().containsKey(TEST_EXTRACT_PREFIX + "/" + TEST_ARCHIVE_ENTRY_2_NAME));
        Assertions.assertTrue(context.getFiles().containsKey(TEST_EXTRACT_PREFIX + "/" + TEST_ARCHIVE_ENTRY_3_NAME));

        Assertions.assertArrayEquals(TEST_ARCHIVE_ENTRY_1_CONTENT, context.getFiles().get(TEST_EXTRACT_PREFIX + "/" + TEST_ARCHIVE_ENTRY_1_NAME));
        Assertions.assertArrayEquals(TEST_ARCHIVE_ENTRY_2_CONTENT, context.getFiles().get(TEST_EXTRACT_PREFIX + "/" + TEST_ARCHIVE_ENTRY_2_NAME));
        Assertions.assertArrayEquals(TEST_ARCHIVE_ENTRY_3_CONTENT, context.getFiles().get(TEST_EXTRACT_PREFIX + "/" + TEST_ARCHIVE_ENTRY_3_NAME));
    }
}
