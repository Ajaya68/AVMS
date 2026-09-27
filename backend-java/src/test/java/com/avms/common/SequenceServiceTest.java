package com.avms.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class SequenceServiceTest {

  @Autowired SequenceService sequences;

  @Test
  void generatesSequentialCodesPerModuleAndVenture() {
    String first = sequences.generateCode("customers", 7L, "C");
    String second = sequences.generateCode("customers", 7L, "C");
    String otherVenture = sequences.generateCode("customers", 8L, "C");
    String otherModule = sequences.generateCode("suppliers", 7L, "S");

    assertThat(first).isEqualTo("C-0001");
    assertThat(second).isEqualTo("C-0002");
    assertThat(otherVenture).isEqualTo("C-0001");
    assertThat(otherModule).isEqualTo("S-0001");
  }
}
