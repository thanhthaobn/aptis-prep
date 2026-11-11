package com.aptisprep.service.exam;

import com.aptisprep.dto.view.PlacementDTO.PlacementExam;
import java.util.List;

public interface PlacementExamCatalogService {
    List<PlacementExam> findPublished();

    List<PlacementExam> findPublished(String skill);
}
