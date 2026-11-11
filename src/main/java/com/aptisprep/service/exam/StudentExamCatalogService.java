package com.aptisprep.service.exam;

import com.aptisprep.dto.StudentCatalogViewDTO.Entry;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StudentExamCatalogService {

    public static class Category {
        private String code;
        private String label;
        private long count;

        public Category() {}

        public Category(String code, String label, long count) {
            this.code = code;
            this.label = label;
            this.count = count;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String code() {
            return code;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }

        public long count() {
            return count;
        }
    }

    public static class Catalog {
        private Page<Entry> page;
        private List<Category> categories;

        public Catalog() {}

        public Catalog(Page<Entry> page, List<Category> categories) {
            this.page = page;
            this.categories = categories;
        }

        public Page<Entry> getPage() {
            return page;
        }

        public void setPage(Page<Entry> page) {
            this.page = page;
        }

        public Page<Entry> page() {
            return page;
        }

        public List<Category> getCategories() {
            return categories;
        }

        public void setCategories(List<Category> categories) {
            this.categories = categories;
        }

        public List<Category> categories() {
            return categories;
        }
    }

    public List<Entry> find(String module, String query, String skill, String level, String state);

    public Catalog browse(
            String query,
            String skill,
            String level,
            String state,
            String examType,
            String sort,
            Pageable pageable);

    public Entry entry(String module, int id);

    public Entry getExamEntryById(String module, int id);
}
