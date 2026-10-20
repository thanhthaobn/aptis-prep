package com.aptisprep.repository;
import com.aptisprep.entity.LearningPath;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface LearningPathRepository extends JpaRepository<LearningPath,Integer>{
    List<LearningPath> findByUserIdAndStatusOrderBySkillAscCreatedAtDesc(Integer userId,String status);
    Optional<LearningPath> findFirstByUserIdAndSkillAndStatusOrderByCreatedAtDesc(Integer userId,String skill,String status);
}
