package com.avms.common;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Upload-surface regression guard (PH8 future-proofing): the backend has NO
 * file-upload endpoints by design. If a future change introduces one
 * (MultipartFile / @RequestPart anywhere in src/main/java), this test fails
 * with the required control checklist instead of letting an unhardened upload
 * reach production. To resolve a failure, implement every item in the upload
 * checklist inside application.yml (auth + venture scope, sniffed MIME +
 * extension allowlists, UUID filenames, isolated storage outside the web root,
 * bounded parser resources) and update this allowlist consciously.
 */
class UploadSurfaceGuardTest {

  private static final List<String> UPLOAD_MARKERS = List.of(
      "multipartfile", "@requestpart", "filewriter", "fileoutputstream");

  @Test
  void noUploadSinksExistWithoutExplicitHardening() throws IOException {
    Path sourceRoot = Paths.get("src", "main", "java");
    assertTrue(Files.isDirectory(sourceRoot), "src/main/java must exist for the guard to scan");
    try (Stream<Path> sources = Files.walk(sourceRoot)) {
      List<Path> hits = sources
          .filter(p -> p.toString().endsWith(".java"))
          .filter(p -> {
            try {
              String text = Files.readString(p).toLowerCase(Locale.ROOT);
              return UPLOAD_MARKERS.stream().anyMatch(text::contains)
                  && !p.getFileName().toString().equals("UploadSurfaceGuardTest.java");
            } catch (IOException e) {
              throw new IllegalStateException(e);
            }
          })
          .toList();
      if (!hits.isEmpty()) {
        fail("Upload sink(s) introduced without hardening: " + hits
            + " — implement the upload checklist in application.yml first "
            + "(see ProfileApplication docs) then update this guard.");
      }
    }
  }
}
