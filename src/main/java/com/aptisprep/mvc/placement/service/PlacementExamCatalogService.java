package com.aptisprep.mvc.placement.service;

import com.aptisprep.mvc.placement.dto.PlacementDTO.PlacementExam;
import java.util.List;

public interface PlacementExamCatalogService {
    List<PlacementExam> findPublished();

    List<PlacementExam> findPublished(String skill);
}
