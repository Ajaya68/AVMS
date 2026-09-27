package com.avms.ventures;

import static org.assertj.core.api.Assertions.assertThat;

import com.avms.security.PermEvaluator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class VentureServiceTest {

  @Autowired VentureService service;

  @Test
  void createAssignsSequentialCodesAndLists() {
    var a = service.create(new VentureDtos.VentureRequest("Mushroom Unit", null, "MUSHROOM", null, null, null, "Pune", null, null, "ACTIVE"));
    var b = service.create(new VentureDtos.VentureRequest("Fish Farm", null, "FISH_FARMING", null, null, null, "Nashik", null, null, "ACTIVE"));

    assertThat(a.ventureCode()).isEqualTo("V-0001");
    assertThat(b.ventureCode()).isEqualTo("V-0002");

    var page = service.list("mushroom", null, PageRequest.of(0, 20));
    assertThat(page.getTotalElements()).isEqualTo(1);
    assertThat(page.getContent().get(0).ventureCode()).isEqualTo("V-0001");

    var fetched = service.get(a.id());
    assertThat(fetched.ventureName()).isEqualTo("Mushroom Unit");
  }
}
